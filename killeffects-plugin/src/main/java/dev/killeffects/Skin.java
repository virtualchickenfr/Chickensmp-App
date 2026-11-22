package dev.killeffects;

import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import javax.imageio.ImageIO;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.profile.PlayerTextures;

/**
 * Reads a player's skin and turns every body part into 4 horizontal colour bands (top to bottom),
 * each mapped to the closest block. Used to build a blocky copy of the player (see Rig).
 */
final class Skin {
    enum Part { TORSO, RARM, LARM, RLEG, LLEG }

    /** blocks[part][band], band 0 = top. */
    final Material[][] blocks;
    final boolean slim;

    private Skin(Material[][] blocks, boolean slim) {
        this.blocks = blocks;
        this.slim = slim;
    }

    Material get(Part p, int band) {
        return blocks[p.ordinal()][Math.max(0, Math.min(3, band))];
    }

    // ------------------------------------------------------------------ cache

    private static final Map<UUID, Skin> CACHE = new ConcurrentHashMap<>();
    private static final Map<UUID, Boolean> LOADING = new ConcurrentHashMap<>();
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).followRedirects(HttpClient.Redirect.NORMAL).build();

    static final Skin DEFAULT = new Skin(new Material[][]{
            {Material.CYAN_CONCRETE, Material.CYAN_CONCRETE, Material.CYAN_CONCRETE, Material.CYAN_TERRACOTTA},
            {Material.CYAN_CONCRETE, Material.WHITE_TERRACOTTA, Material.WHITE_TERRACOTTA, Material.WHITE_TERRACOTTA},
            {Material.CYAN_CONCRETE, Material.WHITE_TERRACOTTA, Material.WHITE_TERRACOTTA, Material.WHITE_TERRACOTTA},
            {Material.BLUE_CONCRETE, Material.BLUE_CONCRETE, Material.BLUE_CONCRETE, Material.GRAY_CONCRETE},
            {Material.BLUE_CONCRETE, Material.BLUE_CONCRETE, Material.BLUE_CONCRETE, Material.GRAY_CONCRETE}}, false);

    static Skin of(Player p) {
        if (p == null) return DEFAULT;
        Skin s = CACHE.get(p.getUniqueId());
        if (s == null) {
            load((Plugin) KillEffects.getPlugin(KillEffects.class), p);
            return DEFAULT;
        }
        return s;
    }

    /** Fetch and analyse the skin in the background (on join, and on demand). */
    static void load(Plugin plugin, Player p) {
        UUID id = p.getUniqueId();
        if (CACHE.containsKey(id) || LOADING.putIfAbsent(id, true) != null) return;
        URL url;
        boolean slim;
        try {
            PlayerTextures tex = p.getPlayerProfile().getTextures();
            url = tex.getSkin();
            slim = tex.getSkinModel() == PlayerTextures.SkinModel.SLIM;
        } catch (Exception e) {
            LOADING.remove(id);
            return;
        }
        if (url == null) {
            LOADING.remove(id);
            return;
        }
        final URL u = url;
        final boolean sl = slim;
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                HttpRequest req = HttpRequest.newBuilder(URI.create(u.toString().replace("http://", "https://"))).timeout(Duration.ofSeconds(8)).build();
                HttpResponse<InputStream> res = HTTP.send(req, HttpResponse.BodyHandlers.ofInputStream());
                BufferedImage img;
                try (InputStream in = res.body()) {
                    img = ImageIO.read(in);
                }
                if (img != null) CACHE.put(id, analyse(img, sl));
            } catch (Exception e) {
                plugin.getLogger().fine("Skin of " + id + " not loaded: " + e.getMessage());
            } finally {
                LOADING.remove(id);
            }
        });
    }

    static void forget(UUID id) {
        CACHE.remove(id);
    }

    // ------------------------------------------------------------------ analysis

    /** origin u,v of each part's box (base layer, overlay layer) in a 64x64 skin; width, height, depth in pixels. */
    static Skin analyse(BufferedImage img, boolean slim) {
        boolean legacy = img.getHeight() < 64;
        int aw = slim ? 3 : 4;
        int[][] base = {{16, 16}, {40, 16}, {32, 48}, {0, 16}, {16, 48}};
        int[][] over = {{16, 32}, {40, 32}, {48, 48}, {0, 32}, {0, 48}};
        int[] w = {8, aw, aw, 4, 4};
        Material[][] out = new Material[5][4];
        for (int p = 0; p < 5; p++) {
            int[] b = base[p];
            int[] o = over[p];
            if (legacy && (p == 2 || p == 4)) {
                b = base[p == 2 ? 1 : 3];
                o = null;
            }
            if (legacy) o = null;
            for (int band = 0; band < 4; band++) {
                long r = 0, g = 0, bl = 0, n = 0;
                for (int row = band * 3; row < band * 3 + 3; row++) {
                    int faces = 2 * w[p] + 8; // right side 4, front w, left side 4, back w
                    for (int col = 0; col < faces; col++) {
                        int px = sample(img, b[0] + col, b[1] + 4 + row);
                        if (o != null) {
                            int ov = sample(img, o[0] + col, o[1] + 4 + row);
                            if ((ov >>> 24) > 128) px = ov;
                        }
                        if ((px >>> 24) < 128) continue;
                        // the front face counts double: it is what people look at
                        int weight = (col >= 4 && col < 4 + w[p]) ? 2 : 1;
                        r += (long) ((px >> 16) & 0xFF) * weight;
                        g += (long) ((px >> 8) & 0xFF) * weight;
                        bl += (long) (px & 0xFF) * weight;
                        n += weight;
                    }
                }
                out[p][band] = n == 0 ? DEFAULT.blocks[p][band] : nearest((int) (r / n), (int) (g / n), (int) (bl / n));
            }
        }
        return new Skin(out, slim);
    }

    private static int sample(BufferedImage img, int x, int y) {
        if (x < 0 || y < 0 || x >= img.getWidth() || y >= img.getHeight()) return 0;
        return img.getRGB(x, y);
    }

    private static final Object[][] PALETTE = {
            {Material.WHITE_CONCRETE, 0xCFD5D6}, {Material.ORANGE_CONCRETE, 0xE06101}, {Material.MAGENTA_CONCRETE, 0xA9309F},
            {Material.LIGHT_BLUE_CONCRETE, 0x2489C7}, {Material.YELLOW_CONCRETE, 0xF1AF15}, {Material.LIME_CONCRETE, 0x5EA918},
            {Material.PINK_CONCRETE, 0xD5658E}, {Material.GRAY_CONCRETE, 0x373A3E}, {Material.LIGHT_GRAY_CONCRETE, 0x7D7D73},
            {Material.CYAN_CONCRETE, 0x157788}, {Material.PURPLE_CONCRETE, 0x64209C}, {Material.BLUE_CONCRETE, 0x2C2E8F},
            {Material.BROWN_CONCRETE, 0x603C20}, {Material.GREEN_CONCRETE, 0x495B24}, {Material.RED_CONCRETE, 0x8E2121},
            {Material.BLACK_CONCRETE, 0x080A0F},
            {Material.WHITE_TERRACOTTA, 0xD1B2A1}, {Material.ORANGE_TERRACOTTA, 0xA15325}, {Material.MAGENTA_TERRACOTTA, 0x95586C},
            {Material.LIGHT_BLUE_TERRACOTTA, 0x716C89}, {Material.YELLOW_TERRACOTTA, 0xBA8523}, {Material.LIME_TERRACOTTA, 0x677534},
            {Material.PINK_TERRACOTTA, 0xA14E4E}, {Material.GRAY_TERRACOTTA, 0x392923}, {Material.LIGHT_GRAY_TERRACOTTA, 0x876A61},
            {Material.CYAN_TERRACOTTA, 0x575B5B}, {Material.PURPLE_TERRACOTTA, 0x7A4958}, {Material.BLUE_TERRACOTTA, 0x4C3E5C},
            {Material.BROWN_TERRACOTTA, 0x4D3323}, {Material.GREEN_TERRACOTTA, 0x4C532A}, {Material.RED_TERRACOTTA, 0x8F3D2E},
            {Material.BLACK_TERRACOTTA, 0x251610}, {Material.TERRACOTTA, 0x985E43},
            {Material.WHITE_WOOL, 0xE9ECEC}, {Material.LIGHT_GRAY_WOOL, 0x8E8E87}, {Material.RED_WOOL, 0xA12722},
            {Material.PINK_WOOL, 0xED8DAC}, {Material.LIGHT_BLUE_WOOL, 0x3AAFD9}, {Material.YELLOW_WOOL, 0xF8C527},
            {Material.LIME_WOOL, 0x70B919}, {Material.ORANGE_WOOL, 0xF07613}, {Material.MAGENTA_WOOL, 0xBD44B3},
            {Material.SAND, 0xDBD3A0}, {Material.BIRCH_PLANKS, 0xC0AF79}, {Material.OAK_PLANKS, 0xA2834F},
            {Material.SPRUCE_PLANKS, 0x735531}, {Material.DARK_OAK_PLANKS, 0x42290F}, {Material.QUARTZ_BLOCK, 0xEBE5DE},
            {Material.SNOW_BLOCK, 0xF9FEFE}, {Material.IRON_BLOCK, 0xDCDCDC}, {Material.MUSHROOM_STEM, 0xCBC4B9},
            {Material.PACKED_MUD, 0x8E6B50}, {Material.SMOOTH_SANDSTONE, 0xE0D6AA}, {Material.STRIPPED_BIRCH_WOOD, 0xC4B07B},
            {Material.NETHERRACK, 0x6F3535}, {Material.CRIMSON_PLANKS, 0x653147}, {Material.WARPED_PLANKS, 0x2B6963},
            {Material.GOLD_BLOCK, 0xF6D03D}, {Material.EMERALD_BLOCK, 0x2ACB57}, {Material.DIAMOND_BLOCK, 0x62EDE4},
            {Material.LAPIS_BLOCK, 0x1F438C}, {Material.REDSTONE_BLOCK, 0xAF1805}, {Material.COAL_BLOCK, 0x101010}};

    static Material nearest(int r, int g, int b) {
        Material best = Material.WHITE_CONCRETE;
        double bd = Double.MAX_VALUE;
        for (Object[] e : PALETTE) {
            int c = (Integer) e[1];
            int dr = r - ((c >> 16) & 0xFF), dg = g - ((c >> 8) & 0xFF), db = b - (c & 0xFF);
            double rm = (r + ((c >> 16) & 0xFF)) / 2.0;
            double d = (2 + rm / 256) * dr * dr + 4 * dg * dg + (2 + (255 - rm) / 256) * db * db;
            if (d < bd) {
                bd = d;
                best = (Material) e[0];
            }
        }
        return best;
    }
}
