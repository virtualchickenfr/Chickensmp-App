#!/usr/bin/env python3
"""Offline preview renderer for ChickenSMPKillEffects traces (-Dke.trace).

Renders block/item/text displays, mannequins (Steve skin) and particles with the real
Minecraft textures, from the killer's point of view, so effects can be judged frame by frame.
Usage: render.py trace.jsonl out.png [tick tick ...]
"""
import json, math, os, sys, random
import numpy as np
from PIL import Image, ImageDraw

A = '/home/claude/mc/assets/minecraft'
W, H = 480, 320
FOV = 70.0

# ------------------------------------------------------------------ textures
_tex = {}
def tex(path):
    if path in _tex: return _tex[path]
    f = f'{A}/textures/{path}.png'
    if not os.path.exists(f):
        _tex[path] = None
        return None
    im = Image.open(f).convert('RGBA')
    if im.height > im.width:  # animated strip: first frame
        im = im.crop((0, 0, im.width, im.width))
    _tex[path] = np.asarray(im).astype(np.float32) / 255.0
    return _tex[path]

def solid(rgb):
    a = np.zeros((1, 1, 4), np.float32); a[0, 0, :3] = rgb; a[0, 0, 3] = 1; return a

# ------------------------------------------------------------------ block models
_models = {}
def load_model(name):
    name = name.replace('minecraft:', '')
    if name in _models: return _models[name]
    f = f'{A}/models/{name}.json'
    if not os.path.exists(f):
        _models[name] = None; return None
    d = json.load(open(f))
    textures, elements = {}, None
    if 'parent' in d:
        p = load_model(d['parent'])
        if p:
            textures.update(p['textures']); elements = p['elements']
    textures.update(d.get('textures', {}))
    if 'elements' in d: elements = d['elements']
    _models[name] = {'textures': textures, 'elements': elements}
    return _models[name]

def resolve(textures, ref, depth=0):
    while ref and ref.startswith('#') and depth < 10:
        ref = textures.get(ref[1:]); depth += 1
    return ref

_block_cache = {}
def block_model(mat):
    """List of (quads) for a block in 0..1 space: [(verts4x3, uv4x2, texture, shade)]"""
    if mat in _block_cache: return _block_cache[mat]
    model = None
    bs = f'{A}/blockstates/{mat}.json'
    if os.path.exists(bs):
        d = json.load(open(bs))
        v = None
        if 'variants' in d:
            v = list(d['variants'].values())[0]
        elif 'multipart' in d:
            v = d['multipart'][0]['apply']
        if isinstance(v, list): v = v[0]
        if v: model = load_model(v['model'])
    quads = []
    if model and model['elements']:
        for el in model['elements']:
            quads += element_quads(el, model['textures'])
    else:
        t = None
        for cand in (f'block/{mat}', f'block/{mat}_side', f'block/{mat}_top', f'item/{mat}'):
            if tex(cand) is not None: t = tex(cand); break
        if t is None: t = solid((0.8, 0.2, 0.8))
        quads = cube_quads((0, 0, 0), (1, 1, 1), t)
    _block_cache[mat] = quads
    return quads

FACES = {  # face: (normal, corners as fractions of from/to)
    'north': ((0, 0, -1), [(1, 1, 0), (0, 1, 0), (0, 0, 0), (1, 0, 0)]),
    'south': ((0, 0, 1), [(0, 1, 1), (1, 1, 1), (1, 0, 1), (0, 0, 1)]),
    'west': ((-1, 0, 0), [(0, 1, 0), (0, 1, 1), (0, 0, 1), (0, 0, 0)]),
    'east': ((1, 0, 0), [(1, 1, 1), (1, 1, 0), (1, 0, 0), (1, 0, 1)]),
    'up': ((0, 1, 0), [(0, 1, 0), (1, 1, 0), (1, 1, 1), (0, 1, 1)]),
    'down': ((0, -1, 0), [(0, 0, 1), (1, 0, 1), (1, 0, 0), (0, 0, 0)]),
}
SHADE = {'up': 1.0, 'down': 0.5, 'north': 0.8, 'south': 0.8, 'west': 0.6, 'east': 0.6}

def element_quads(el, textures):
    fr = np.array(el['from'], float) / 16; to = np.array(el['to'], float) / 16
    rot = el.get('rotation')
    out = []
    for face, fd in el.get('faces', {}).items():
        ref = resolve(textures, fd.get('texture', ''))
        t = tex(ref.replace('minecraft:', '')) if ref else None
        if t is None: t = solid((0.8, 0.2, 0.8))
        if 'tintindex' in fd:
            t = t.copy(); t[..., :3] *= np.array([0.55, 0.8, 0.35])
        n, corners = FACES[face]
        vs = np.array([[fr[i] + c[i] * (to[i] - fr[i]) for i in range(3)] for c in corners])
        if 'uv' in fd:
            u0, v0, u1, v1 = [x / 16 for x in fd['uv']]
        else:
            ax = {'north': (0, 1), 'south': (0, 1), 'west': (2, 1), 'east': (2, 1), 'up': (0, 2), 'down': (0, 2)}[face]
            u0, u1 = fr[ax[0]], to[ax[0]]
            if face in ('up', 'down'): v0, v1 = fr[2], to[2]
            else: v0, v1 = 1 - to[1], 1 - fr[1]
            if face in ('north', 'east'): u0, u1 = 1 - u0, 1 - u1
        uv = np.array([(u0, v0), (u1, v0), (u1, v1), (u0, v1)])
        r = fd.get('rotation', 0)
        if r: uv = np.roll(uv, -(r // 90), axis=0)
        if rot:
            o = np.array(rot['origin'], float) / 16; a = math.radians(rot['angle'])
            ax = rot['axis']
            c, s = math.cos(a), math.sin(a)
            M = {'x': [[1, 0, 0], [0, c, -s], [0, s, c]], 'y': [[c, 0, s], [0, 1, 0], [-s, 0, c]], 'z': [[c, -s, 0], [s, c, 0], [0, 0, 1]]}[ax]
            vs = (vs - o) @ np.array(M).T + o
        out.append((vs, uv, t, SHADE[face]))
    return out

def cube_quads(fr, to, t, uvmap=None):
    out = []
    fr = np.array(fr, float); to = np.array(to, float)
    for face, (n, corners) in FACES.items():
        vs = np.array([[fr[i] + c[i] * (to[i] - fr[i]) for i in range(3)] for c in corners])
        uv = np.array([(0, 0), (1, 0), (1, 1), (0, 1)], float) if uvmap is None else uvmap[face]
        tt = t[face] if isinstance(t, dict) else t
        out.append((vs, uv, tt, SHADE[face]))
    return out

# ------------------------------------------------------------------ player model (Steve)
def box_uv(u, v, w, h, d, size=64):
    def r(x, y, ww, hh): return np.array([(x, y), (x + ww, y), (x + ww, y + hh), (x, y + hh)], float) / size
    return {'up': r(u + d, v, w, d), 'down': r(u + d + w, v, w, d), 'east': r(u, v + d, d, h),
            'south': r(u + d, v + d, w, h), 'west': r(u + d + w, v + d, d, h), 'north': r(u + 2 * d + w, v + d, w, h)}

PARTS = [  # from, to (pixels), uv origin, size
    ((-4, 24, -4), (4, 32, 4), (0, 0)),
    ((-4, 12, -2), (4, 24, 2), (16, 16)),
    ((-8, 12, -2), (-4, 24, 2), (40, 16)),
    ((4, 12, -2), (8, 24, 2), (32, 48)),
    ((-4, 0, -2), (0, 12, 2), (0, 16)),
    ((0, 0, -2), (4, 12, 2), (16, 48)),
]

def player_quads():
    skin = tex('entity/player/wide/steve')
    out = []
    for fr, to, (u, v) in PARTS:
        w, h, d = to[0] - fr[0], to[1] - fr[1], to[2] - fr[2]
        uvm = box_uv(u, v, w, h, d)
        for vs, uv, t, sh in cube_quads(np.array(fr) / 16, np.array(to) / 16, skin, uvm):
            out.append((vs, uv, t, sh))
    return out

# ------------------------------------------------------------------ math
def quat_mat(q):
    x, y, z, w = q
    return np.array([[1 - 2 * (y * y + z * z), 2 * (x * y - z * w), 2 * (x * z + y * w)],
                     [2 * (x * y + z * w), 1 - 2 * (x * x + z * z), 2 * (y * z - x * w)],
                     [2 * (x * z - y * w), 2 * (y * z + x * w), 1 - 2 * (x * x + y * y)]])

def roty(a):
    c, s = math.cos(a), math.sin(a); return np.array([[c, 0, s], [0, 1, 0], [-s, 0, c]])

def rotx(a):
    c, s = math.cos(a), math.sin(a); return np.array([[1, 0, 0], [0, c, -s], [0, s, c]])

class Cam:
    def __init__(self, pos, target):
        self.p = np.array(pos, float)
        f = np.array(target, float) - self.p; f /= np.linalg.norm(f)
        r = np.cross(f, (0, 1, 0)); r /= np.linalg.norm(r)
        u = np.cross(r, f)
        self.R = np.array([r, u, -f])
        self.f = (H / 2) / math.tan(math.radians(FOV) / 2)

    def proj(self, v):
        c = (v - self.p) @ self.R.T
        z = -c[..., 2]
        z = np.maximum(z, 1e-4)
        x = W / 2 + c[..., 0] / z * self.f
        y = H / 2 - c[..., 1] / z * self.f
        return np.stack([x, y, z], -1)

    def billboard(self):
        return self.R.T  # columns: right, up, back

# ------------------------------------------------------------------ raster
class Raster:
    def __init__(self, cam):
        self.cam = cam
        self.img = np.zeros((H, W, 3), np.float32)
        yy = np.linspace(0, 1, H)[:, None]
        self.img[:] = (np.array([0.47, 0.65, 1.0]) * (1 - yy * 0.4) + np.array([0.75, 0.85, 1.0]) * yy * 0.4)[:, None, :]
        self.z = np.full((H, W), np.inf, np.float32)
        self.trans = []

    def quad(self, vs, uv, t, shade, light=1.0, tint=None, alpha_mul=1.0):
        P = self.cam.proj(vs)
        if np.all(P[:, 2] <= 1e-3): return
        for tri in ((0, 1, 2), (0, 2, 3)):
            self.tri(P[list(tri)], uv[list(tri)], t, shade * light, tint, alpha_mul)

    def tri(self, P, uv, t, shade, tint, am):
        x0, y0 = max(int(np.floor(P[:, 0].min())), 0), max(int(np.floor(P[:, 1].min())), 0)
        x1, y1 = min(int(np.ceil(P[:, 0].max())), W - 1), min(int(np.ceil(P[:, 1].max())), H - 1)
        if x1 < x0 or y1 < y0: return
        if (x1 - x0) * (y1 - y0) > W * H * 4: return
        xs, ys = np.meshgrid(np.arange(x0, x1 + 1) + 0.5, np.arange(y0, y1 + 1) + 0.5)
        (ax, ay, az), (bx, by, bz), (cx, cy, cz) = P
        den = (by - cy) * (ax - cx) + (cx - bx) * (ay - cy)
        if abs(den) < 1e-9: return
        w0 = ((by - cy) * (xs - cx) + (cx - bx) * (ys - cy)) / den
        w1 = ((cy - ay) * (xs - cx) + (ax - cx) * (ys - cy)) / den
        w2 = 1 - w0 - w1
        m = (w0 >= -1e-6) & (w1 >= -1e-6) & (w2 >= -1e-6)
        if not m.any(): return
        iz = w0 / az + w1 / bz + w2 / cz
        z = 1 / iz
        u = (w0 * uv[0, 0] / az + w1 * uv[1, 0] / bz + w2 * uv[2, 0] / cz) * z
        v = (w0 * uv[0, 1] / az + w1 * uv[1, 1] / bz + w2 * uv[2, 1] / cz) * z
        th, tw = t.shape[:2]
        ti = np.clip((v * th).astype(int), 0, th - 1); tj = np.clip((u * tw).astype(int), 0, tw - 1)
        c = t[ti, tj]
        a = c[..., 3] * am
        col = c[..., :3] * shade
        if tint is not None: col = col * np.array(tint)
        sub = (slice(y0, y1 + 1), slice(x0, x1 + 1))
        zb = self.z[sub]
        opaque = m & (a > 0.98) & (z < zb)
        self.img[sub][opaque] = col[opaque]
        zb[opaque] = z[opaque]
        tr = m & (a > 0.02) & (a <= 0.98)
        if tr.any():
            self.trans.append((float(z[tr].mean()), sub, tr, z, col, a))

    def flush(self):
        for zz, sub, tr, z, col, a in sorted(self.trans, key=lambda t: -t[0]):
            zb = self.z[sub]
            vis = tr & (z < zb)
            img = self.img[sub]
            img[vis] = img[vis] * (1 - a[vis, None]) + col[vis] * a[vis, None]
        self.trans = []

    def sprite(self, pos, size, t, color=None, alpha=1.0, add=False):
        P = self.cam.proj(np.array([pos]))[0]
        if P[2] < 0.05: return
        s = max(1, int(size / P[2] * self.cam.f))
        x, y = int(P[0] - s / 2), int(P[1] - s / 2)
        if x >= W or y >= H or x + s < 0 or y + s < 0: return
        if t is None: t = solid((1, 1, 1))
        th, tw = t.shape[:2]
        ii = (np.arange(s) * th // s); jj = (np.arange(s) * tw // s)
        c = t[ii][:, jj]
        ys0, xs0 = max(0, -y), max(0, -x)
        ys1, xs1 = min(s, H - y), min(s, W - x)
        c = c[ys0:ys1, xs0:xs1]
        sub = (slice(y + ys0, y + ys1), slice(x + xs0, x + xs1))
        a = c[..., 3] * alpha
        col = c[..., :3] * (np.array(color) if color is not None else 1)
        vis = (a > 0.05) & (P[2] < self.z[sub])
        img = self.img[sub]
        if add:
            img[vis] = np.minimum(1, img[vis] + col[vis] * a[vis, None])
        else:
            img[vis] = img[vis] * (1 - a[vis, None]) + col[vis] * a[vis, None]

# ------------------------------------------------------------------ scene
GROUND = None
def draw_ground(r):
    g = tex('block/grass_block_top')
    g = g.copy(); g[..., :3] *= np.array([0.55, 0.8, 0.35])
    for gx in range(-8, 8):
        for gz in range(-10, 6):
            vs = np.array([(gx, 0, gz), (gx + 1, 0, gz), (gx + 1, 0, gz + 1), (gx, 0, gz + 1)], float)
            r.quad(vs, np.array([(0, 0), (1, 0), (1, 1), (0, 1)], float), g, 1.0)

def ent_rot(e):
    return roty(-math.radians(e.get('yaw', 0))) @ rotx(math.radians(e.get('pitch', 0)))

def display_mat(e, cam):
    tr = np.array(e['tr']); L = quat_mat(e['lr']); S = np.diag(e['s']); Rr = quat_mat(e['rr'])
    bb = e.get('bb', 'FIXED')
    if bb == 'CENTER':
        B = cam.billboard()
        base = B
    elif bb == 'VERTICAL':
        d = cam.p - np.array(e['p']); a = math.atan2(d[0], d[2])
        base = roty(a)
    else:
        base = ent_rot(e)
    M = base @ L @ S @ Rr
    T = np.array(e['p']) + base @ tr
    return M, T

ITEM_BLOCKS = set()
def item_quads(mat):
    t = tex(f'item/{mat}')
    if mat == 'player_head':
        skin = tex('entity/player/wide/steve')
        return [(vs - 0.25, uv, tt, sh) for vs, uv, tt, sh in cube_quads((0, 0, 0), (0.5, 0.5, 0.5), skin, box_uv(0, 0, 8, 8, 8))]
    if t is not None and not os.path.exists(f'{A}/blockstates/{mat}.json'):
        vs = np.array([(-0.5, 0.5, 0), (0.5, 0.5, 0), (0.5, -0.5, 0), (-0.5, -0.5, 0)], float)
        uv = np.array([(0, 0), (1, 0), (1, 1), (0, 1)], float)
        # thickness: back face
        return [(vs, uv, t, 1.0), (vs[[1, 0, 3, 2]] + [0, 0, -1 / 16], uv[[1, 0, 3, 2]], t, 0.8)]
    q = block_model(mat)
    return [((vs - 0.5) * 0.5, uv, tt, sh) for vs, uv, tt, sh in q]

def particle_look(k, rec):
    """(texture, size, life, gravity, color, additive)"""
    c = rec.get('c'); col = None
    if c is not None: col = ((c >> 16 & 255) / 255, (c >> 8 & 255) / 255, (c & 255) / 255)
    g = lambda n: tex(f'particle/{n}')
    k = k.upper()
    if k in ('DUST', 'DUST_COLOR_TRANSITION'): return g('generic_0'), 0.11 * rec.get('sz', 1), 18, -0.002, col, False
    if k == 'FLAME': return g('flame'), 0.14, 14, -0.002, None, True
    if k == 'SOUL_FIRE_FLAME': return g('soul_fire_flame'), 0.14, 14, -0.002, None, True
    if k == 'SOUL': return g('soul_0'), 0.3, 20, -0.004, None, True
    if k == 'END_ROD': return g('glitter_3'), 0.12, 40, 0, None, True
    if k in ('ELECTRIC_SPARK', 'WAX_ON', 'WAX_OFF', 'SCRAPE'): return g('glow'), 0.1, 6, 0, (0.7, 0.9, 1.0), True
    if k in ('FIREWORK',): return g('spark_3'), 0.12, 30, 0.005, None, True
    if k in ('CRIT', 'ENCHANTED_HIT'): return g('critical_hit'), 0.12, 12, 0.01, (0.9, 0.9, 1) if k == 'CRIT' else (0.6, 0.5, 1), False
    if k == 'EXPLOSION': return g('explosion_3'), 1.6, 8, 0, None, False
    if k == 'EXPLOSION_EMITTER': return g('explosion_5'), 3.0, 8, 0, None, False
    if k == 'FLASH': return g('flash'), 3.5, 4, 0, col, True
    if k == 'SONIC_BOOM': return g('sonic_boom_4'), 1.6, 8, 0, None, True
    if k in ('SMOKE', 'LARGE_SMOKE', 'CAMPFIRE_COSY_SMOKE', 'CAMPFIRE_SIGNAL_SMOKE', 'WHITE_SMOKE'):
        sz = {'SMOKE': 0.15, 'LARGE_SMOKE': 0.45, 'WHITE_SMOKE': 0.3}.get(k, 0.9)
        return g('big_smoke_4'), sz, 30, -0.01, (0.25, 0.25, 0.25) if k != 'WHITE_SMOKE' else (0.9, 0.9, 0.9), False
    if k in ('POOF', 'CLOUD'): return g('generic_2'), 0.35, 12, 0, (0.95, 0.95, 0.95), False
    if k in ('BLOCK', 'BLOCK_CRUMBLE', 'FALLING_DUST', 'DUST_PILLAR', 'BLOCK_MARKER', 'ITEM'):
        m = rec.get('m', 'stone')
        t = None
        for cand in (f'block/{m}', f'item/{m}', f'block/{m}_top', f'block/{m}_side'):
            if tex(cand) is not None: t = tex(cand); break
        return (t if t is not None else g('generic_0')), 0.12 if k != 'DUST_PILLAR' else 0.2, 16, 0.04, None, False
    if k == 'TOTEM_OF_UNDYING': return g('glitter_5'), 0.12, 40, 0.01, (0.6, 0.95, 0.3), True
    if k in ('PORTAL', 'REVERSE_PORTAL'): return g('generic_1'), 0.1, 30, 0, (0.75, 0.3, 1.0), True
    if k == 'DRAGON_BREATH': return g('generic_3'), 0.2, 30, 0, (0.8, 0.3, 0.9), True
    if k in ('HEART',): return g('heart'), 0.25, 20, -0.01, None, False
    if k == 'NOTE': return g('note'), 0.25, 8, 0, (0.3, 0.9, 0.3), False
    if k == 'LAVA': return g('lava'), 0.2, 20, 0.04, None, True
    if k in ('SNOWFLAKE', 'ITEM_SNOWBALL'): return g('generic_0'), 0.08, 30, 0.003, (0.95, 0.97, 1), False
    if k in ('GLOW',): return g('glow'), 0.12, 30, 0, (0.4, 1, 0.8), True
    if k in ('TRAIL',): return g('generic_0'), 0.12, rec.get('dur', 20), 0, col, True
    if k in ('GUST', 'GUST_EMITTER_LARGE', 'GUST_EMITTER_SMALL', 'SMALL_GUST'): return g('gust_4'), 1.0, 10, 0, None, False
    if k in ('SHRIEK',): return g('shriek'), 0.8, 30, -0.02, None, True
    if k in ('SCULK_SOUL',): return g('sculk_soul_3'), 0.3, 20, -0.005, None, True
    if k in ('WITCH', 'EFFECT', 'INSTANT_EFFECT', 'ENTITY_EFFECT'): return g('spell_3'), 0.15, 20, -0.005, col, True
    if k in ('ENCHANT',): return g('sga_e'), 0.1, 30, 0, None, False
    if k in ('SPLASH', 'FALLING_WATER', 'BUBBLE'): return g('splash_0'), 0.12, 15, 0.04, (0.4, 0.6, 1), False
    if k in ('ANGRY_VILLAGER',): return g('angry'), 0.3, 15, 0, None, False
    if k in ('HAPPY_VILLAGER', 'COMPOSTER'): return g('glow'), 0.1, 15, 0, (0.4, 1, 0.4), True
    if k in ('SWEEP_ATTACK',): return g('sweep_3'), 1.2, 6, 0, None, False
    if k in ('TRIAL_SPAWNER_DETECTION', 'TRIAL_SPAWNER_DETECTION_OMINOUS'): return g('trial_spawner_detection_2'), 0.25, 20, -0.01, None, True
    if k in ('SCULK_CHARGE_POP',): return g('sculk_charge_pop_1'), 0.2, 10, 0, None, True
    if k in ('CHERRY_LEAVES',): return g('cherry_3'), 0.15, 60, 0.005, None, False
    if k in ('FIREFLY',): return g('firefly'), 0.12, 40, 0, None, True
    return g('generic_0'), 0.1, 15, 0, (1, 1, 1), False

class Particles:
    """Simulates particles from the trace (spawn tick, position, spread, speed)."""
    def __init__(self, frames):
        self.items = []
        for fr in frames:
            t0 = fr['tick']
            for rec in fr['pt']:
                k = rec['k']
                look = particle_look(k, rec)
                rnd = random.Random(hash((t0, k, tuple(rec['p']))) & 0xffffffff)
                n = rec['n']; d = rec['d']; sp = rec['sp']
                life = look[2]
                if n == 0:
                    ps = [(np.array(rec['p']), np.array(d) * (sp if sp else 1) * (1 if k not in ('DUST',) else 0))]
                else:
                    ps = []
                    for _ in range(min(n, 60)):
                        off = np.array([rnd.gauss(0, d[0]), rnd.gauss(0, d[1]), rnd.gauss(0, d[2])])
                        dirv = np.array([rnd.gauss(0, 1), rnd.gauss(0, 1), rnd.gauss(0, 1)])
                        dirv = dirv / (np.linalg.norm(dirv) + 1e-9) * sp
                        if k in ('FLAME', 'SOUL_FIRE_FLAME', 'END_ROD', 'FIREWORK', 'TOTEM_OF_UNDYING', 'PORTAL', 'CLOUD', 'POOF', 'SMOKE', 'LARGE_SMOKE', 'LAVA', 'BLOCK', 'ITEM', 'CRIT', 'ENCHANTED_HIT', 'ELECTRIC_SPARK', 'DRAGON_BREATH'):
                            pass
                        else:
                            dirv *= 0.3
                        ps.append((np.array(rec['p']) + off, dirv))
                for p0, v0 in ps:
                    self.items.append((t0, p0, v0, look, rnd.uniform(0.7, 1.2) * life, rnd.random()))

    def draw(self, r, tick):
        for t0, p0, v0, (t, size, life, grav, col, add), lf, rr in self.items:
            age = tick - t0
            if age < 0 or age > lf: continue
            drag = (1 - 0.96 ** age) / 0.04 if age > 0 else 0
            pos = p0 + v0 * drag + np.array([0, -grav * age * age * 0.5, 0])
            fade = 1 - (age / lf) ** 3
            r.sprite(pos, size * (0.9 + 0.2 * rr), t, col, alpha=fade, add=add)

def _lerp_state(a, b, t):
    out = dict(b)
    for k in ('tr', 's', 'p'):
        out[k] = list(np.array(a[k]) * (1 - t) + np.array(b[k]) * t)
    for k in ('lr', 'rr'):
        qa, qb = np.array(a[k]), np.array(b[k])
        if np.dot(qa, qb) < 0: qb = -qb
        q = qa * (1 - t) + qb * t
        out[k] = list(q / (np.linalg.norm(q) + 1e-9))
    return out

def interpolate(frames):
    """Replace display targets with what the client shows (interpolation / teleport smoothing)."""
    st = {}
    for fr in sorted(frames, key=lambda f: f['tick']):
        tk = fr['tick']
        for e in fr['e']:
            if 'tr' not in e: continue
            i = e['id']
            key = (tuple(e['tr']), tuple(e['lr']), tuple(e['s']), tuple(e['rr']))
            pkey = tuple(e['p'])
            s = st.get(i)
            if s is None:
                st[i] = s = {'key': key, 'from': dict(e), 'to': dict(e), 'start': tk, 'dur': 0, 'pkey': pkey, 'pfrom': e['p'], 'pstart': tk}
            cur = lambda: _lerp_state(s['from'], s['to'], 1.0 if s['dur'] <= 0 else min(1, max(0, (tk - s['start']) / s['dur'])))
            if key != s['key']:
                c = cur()
                s['from'] = c; s['to'] = dict(e); s['start'] = tk + max(0, e.get('idel', 0)); s['dur'] = e.get('idur', 0); s['key'] = key
            if pkey != s['pkey']:
                td = e.get('tdur', 0)
                prevp = s['to'].get('p', e['p'])
                s['pfrom'] = prevp; s['pstart'] = tk; s['pdur'] = td; s['pkey'] = pkey
            s['to']['p'] = e['p']
            v = cur()
            if s.get('pdur', 0) > 0 and tk - s['pstart'] < s['pdur']:
                t = (tk - s['pstart']) / s['pdur']
                v['p'] = list(np.array(s['pfrom']) * (1 - t) + np.array(e['p']) * t)
            else:
                v['p'] = e['p']
            e.update({k: v[k] for k in ('tr', 'lr', 's', 'rr', 'p')})

def render_frame(frames_by_tick, parts, tick, cam):
    r = Raster(cam)
    draw_ground(r)
    fr = frames_by_tick.get(tick)
    if fr:
        for e in fr['e']:
            t = e.get('t')
            if t in ('b', 'i', 't'):
                M, T = display_mat(e, cam)
                if t == 'b':
                    q = block_model(e['m'])
                elif t == 'i':
                    q = item_quads(e['m'])
                else:
                    bg = e.get('bg', 0) & 0xFFFFFFFF
                    a = (bg >> 24 & 255) / 255
                    text = e.get('text', '')
                    lines = max(1, text.count('\n') + 1)
                    if text.strip() == '':
                        w, h = 0.125, 0.25
                    else:
                        wpx = max(len(l) for l in (text.split('\n') or [''])) * 6 + 1
                        w = (wpx + 2) / 40; h = (lines * 10 + 1) / 40
                    vs = np.array([(-w / 2, h, 0), (w / 2, h, 0), (w / 2, 0, 0), (-w / 2, 0, 0)], float)
                    col = solid(((bg >> 16 & 255) / 255, (bg >> 8 & 255) / 255, (bg & 255) / 255)).copy()
                    col[..., 3] = a
                    q = [(vs, np.array([(0, 0), (1, 0), (1, 1), (0, 1)], float), col, 1.0), (vs[[1, 0, 3, 2]], np.array([(0, 0), (1, 0), (1, 1), (0, 1)], float), col, 1.0)] if a > 0.01 else []
                for vs, uv, tt, sh in q:
                    r.quad(vs @ M.T + T, uv, tt, sh)
            elif t == 'm':
                if e.get('inv'): continue
                s = e.get('scale', 1) * 0.9375
                R = roty(-math.radians(e.get('body', e.get('yaw', 0))))
                pose = e.get('pose', 'STANDING')
                off = np.zeros(3)
                if pose == 'SLEEPING':
                    R = R @ rotx(-math.pi / 2); off = np.array([0, 0.2, 0])
                elif pose == 'SWIMMING':
                    R = R @ rotx(math.pi / 2); off = np.array([0, 0.3, 0])
                elif pose == 'SNEAKING':
                    R = R @ rotx(0.3); off = np.array([0, -0.2, 0])
                for vs, uv, tt, sh in player_quads():
                    r.quad((vs * s) @ R.T + np.array(e['p']) + off, uv, tt, sh)
    r.flush()
    parts.draw(r, tick)
    return r.img

def main():
    trace, out = sys.argv[1], sys.argv[2]
    ticks = [int(x) for x in sys.argv[3:]] or [2, 8, 15, 22, 30, 40, 50, 65]
    frames = [json.loads(l) for l in open(trace) if l.strip()]
    interpolate(frames)
    by = {f['tick']: f for f in frames}
    parts = Particles(frames)
    f = math.radians(frames[0].get('face', 0.0))
    D = float(os.environ.get('CAMD', '5.5')); cam = Cam((D * math.sin(f) + 0.3 * math.cos(f), 2.0, D * math.cos(f) - 0.3 * math.sin(f)), (0, float(os.environ.get('CAMY', '1.6')), 0))
    cols = 4
    rows = math.ceil(len(ticks) / cols)
    sheet = Image.new('RGB', (W * cols, (H + 18) * rows), (20, 20, 24))
    d = ImageDraw.Draw(sheet)
    for i, tk in enumerate(ticks):
        img = render_frame(by, parts, tk, cam)
        im = Image.fromarray((np.clip(img, 0, 1) * 255).astype(np.uint8))
        x, y = (i % cols) * W, (i // cols) * (H + 18)
        sheet.paste(im, (x, y + 18))
        n = len(by[tk]['e']) if tk in by else 0
        d.text((x + 4, y + 3), f'{os.path.basename(trace)}  t={tk} ({tk / 20:.2f}s)  ents={n}', fill=(230, 230, 230))
    sheet.save(out)

if __name__ == '__main__':
    main()
