// "Just joined ChickenSMP" – the owner's own voice-over, with real menu footage.
import { backdrop, beats, echo, fact, hookFrame, payoff, punch } from './format.js';

// A framed clip from the server with a soft glow, a slow drift and a pop-in.
function shot(s, file, { at, out, y = s.full.y + s.full.h * 0.43, w = s.full.w * 0.84, rotate = 0, speed = 0.7 } = {}) {
  return s.clip(`assets/clips/${file}.mp4`, {
    x: s.cx, y, w, frame: 'card', radius: 34, at, out, speed, loop: true,
    rotate, float: 10, enter: 'zoom', exit: 'fade', camera: false, overlap: true,
  });
}

// A big headline in the upper third.
function headline(s, text, { at, out, y = s.safe.y + 70, size = 84 } = {}) {
  return s.text(text, { x: s.cx, y, size, font: 'display', bold: true, at, out, enter: 'pop', maxWidth: s.safe.w, overlap: true });
}

export default {
  intro(s) {
    s.camera(punch(s, '#joined', { zoom: 1.08 }));
    backdrop(s, { glow: '#f59f00', pattern: 'rays' });
    s.clip('assets/clips/chicken.mp4', { x: s.cx, y: s.full.y + s.full.h * 0.4, w: s.full.w * 0.9, frame: 'card', radius: 40, at: 0, enter: 'zoom', dur: 0.35, float: 8, overlap: true });
  },

  brand(s) {
    s.camera([{ at: 0, zoom: 1.0 }, { at: 0.2, zoom: 1.12, dur: 1.4 }]);
    backdrop(s, { glow: '#3b7bff', pattern: 'rays' });
    s.image('assets/logo.png', { w: s.safe.w * 0.95, x: s.cx, y: s.safe.y + s.safe.h * 0.45, shadow: false, at: '#this', enter: 'zoom' });
    s.burst({ x: s.cx, y: s.safe.y + s.safe.h * 0.45, at: 'ChickenSMP', count: 70, colors: ['#3b7bff', '#7cc4ff', '#ffd43b', '#ffffff'] });
  },

  survival(s) {
    s.camera(punch(s, 'economy', { zoom: 1.1 }));
    backdrop(s, { glow: '#2f9e44' });
    s.icon('pickaxe', { x: s.cx - s.safe.w * 0.22, y: s.safe.y + s.safe.h * 0.42, size: 230, color: 'green', at: '#surv', enter: 'pop' });
    s.icon('coins', { x: s.cx + s.safe.w * 0.22, y: s.safe.y + s.safe.h * 0.42, size: 230, color: 'yellow', at: 'economy', enter: 'pop' });
    fact(s, 'Survival + *economy*', { at: 'economy', x: s.cx, y: s.safe.y + s.safe.h * 0.62 });
  },

  economy(s) {
    s.camera([]);
    backdrop(s, { glow: '#f59f00' });
    headline(s, 'Auction house', { at: 0, out: '#orders' });
    shot(s, 'auction', { at: 0, out: '#orders', rotate: -2 });
    headline(s, 'Orders = *cash*', { at: '#orders', out: '#fish' });
    shot(s, 'orders', { at: '#orders', out: '#fish', rotate: 2 });
    headline(s, 'Fish worth *millions*', { at: '#fish' });
    shot(s, 'fishing', { at: '#fish', rotate: -1.5, speed: 0.5 });
  },

  pvp(s) {
    s.camera(punch(s, '#vanish', { zoom: 1.1 }));
    backdrop(s, { glow: '#e03131', pattern: 'rays' });
    beats(s, [
      { at: '#spawn', icon: 'swords', label: 'PvP area', color: 'red' },
      { at: '#build', icon: 'hammer', label: 'Build & fight', color: 'orange' },
      { at: '#vanish', icon: 'timer', label: 'Blocks vanish in 20 min', color: 'yellow' },
    ]);
  },

  rewards(s) {
    s.camera([]);
    backdrop(s, { glow: '#f59f00' });
    headline(s, 'Free *key* every hour', { at: 0, out: '#daily' });
    shot(s, 'crate', { at: 0, out: '#daily', rotate: -2, speed: 0.5 });
    headline(s, 'Daily rewards + *quests*', { at: '#daily' });
    shot(s, 'daily', { at: '#daily', out: 'pay', rotate: 1.5 });
    shot(s, 'quests', { at: 'pay', rotate: -1, speed: 0.6 });
  },

  join(s) {
    s.camera(s.short.drift());
    backdrop(s, { glow: '#3b7bff', pattern: 'rays' });
    s.image('assets/logo.png', { w: s.safe.w * 0.8, x: s.cx, y: s.safe.y + s.safe.h * 0.17, shadow: false, at: 0, enter: 'drop' });
    s.text('IP', { x: s.cx, y: s.safe.y + s.safe.h * 0.34, size: 44, color: 'muted', at: '#ip', enter: 'rise' });
    s.text('chickensmp.net', { x: s.cx, y: s.safe.y + s.safe.h * 0.44, size: 76, font: 'display', bold: true, color: '#7cc4ff',
      bg: 'rgba(11,15,30,0.85)', padding: 28, radius: 26, border: '#3b7bff', at: '#ip', enter: 'zoom' });
    s.icon('gamepad-2', { x: s.cx, y: s.safe.y + s.safe.h * 0.62, size: 180, color: 'green', label: 'See you in game', labelColor: '#ffffff', at: '#see', enter: 'pop' });
    s.burst({ x: s.cx, y: s.safe.y + s.safe.h * 0.42, at: '#ip', count: 90, colors: ['#3b7bff', '#7cc4ff', '#ffd43b'] });
  },
};
