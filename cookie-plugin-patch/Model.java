/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Material
 */
package dev.cookie;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Material;

final class Model {
    private static final String[] SUFFIX = new String[]{"", "k", "M", "B", "T", "Qa", "Qi", "Sx", "Sp", "Oc", "No", "Dc", "UDc", "DDc", "TDc", "QaDc", "QiDc", "SxDc", "SpDc", "OcDc", "NoDc", "Vg"};

    private Model() {
    }

    static String encode(Map<String, Integer> map) {
        StringBuilder stringBuilder = new StringBuilder();
        for (Map.Entry<String, Integer> entry : map.entrySet()) {
            if (entry.getValue() <= 0) continue;
            if (stringBuilder.length() > 0) {
                stringBuilder.append(',');
            }
            stringBuilder.append(entry.getKey()).append(':').append(entry.getValue());
        }
        return stringBuilder.toString();
    }

    static void decode(String string, Map<String, Integer> map) {
        if (string == null || string.isBlank()) {
            return;
        }
        for (String string2 : string.split(",")) {
            int n = string2.indexOf(58);
            if (n <= 0) continue;
            try {
                map.put(string2.substring(0, n), Integer.parseInt(string2.substring(n + 1)));
            }
            catch (NumberFormatException numberFormatException) {
                // empty catch block
            }
        }
    }

    static String shortNum(double d) {
        if (Double.isNaN(d) || Double.isInfinite(d)) {
            return "0";
        }
        double d2 = Math.abs(d);
        if (d2 < 1000.0) {
            if (d2 < 100.0 && d2 != Math.floor(d2)) {
                return new DecimalFormat("0.#", DecimalFormatSymbols.getInstance(Locale.ROOT)).format(d);
            }
            return new DecimalFormat("#,##0", DecimalFormatSymbols.getInstance(Locale.ROOT)).format(Math.floor(d));
        }
        int n = Math.min(SUFFIX.length - 1, (int)Math.floor(Math.log10(d2) / 3.0 + 1.0E-9));
        if ((d2 = Math.abs(d /= Math.pow(1000.0, n))) >= 999.995 && n < SUFFIX.length - 1) {
            d /= 1000.0;
            d2 /= 1000.0;
            ++n;
        }
        String string = d2 >= 100.0 ? "0.#" : "0.##";
        return new DecimalFormat(string, DecimalFormatSymbols.getInstance(Locale.ROOT)).format(d) + SUFFIX[n];
    }

    static String fullNum(double d) {
        if (Math.abs(d) >= 1.0E15) {
            return Model.shortNum(d);
        }
        if (Math.abs(d) < 100.0 && d != Math.floor(d)) {
            return new DecimalFormat("0.#", DecimalFormatSymbols.getInstance(Locale.ROOT)).format(d);
        }
        return new DecimalFormat("#,##0", DecimalFormatSymbols.getInstance(Locale.ROOT)).format(Math.floor(d));
    }

    static double parse(String string) {
        if (string == null || string.isEmpty()) {
            return -1.0;
        }
        String string2 = string.toLowerCase(Locale.ROOT).replace(",", "").replace("_", "");
        double d = 1.0;
        String[] stringArray = new String[]{"k", "m", "b", "t", "qa", "qi"};
        double[] dArray = new double[]{1000.0, 1000000.0, 1.0E9, 1.0E12, 1.0E15, 1.0E18};
        for (int i = stringArray.length - 1; i >= 0; --i) {
            if (!string2.endsWith(stringArray[i]) || string2.length() <= stringArray[i].length()) continue;
            d = dArray[i];
            string2 = string2.substring(0, string2.length() - stringArray[i].length());
            break;
        }
        try {
            double d2 = Double.parseDouble(string2) * d;
            if (Double.isNaN(d2) || Double.isInfinite(d2) || d2 <= 0.0) {
                return -1.0;
            }
            return Math.floor(d2);
        }
        catch (NumberFormatException numberFormatException) {
            return -1.0;
        }
    }

    static final class Rules {
        final Map<String, Building> buildings = new LinkedHashMap<String, Building>();
        final Map<String, Upgrade> upgrades = new LinkedHashMap<String, Upgrade>();
        final List<Golden> golden = new ArrayList<Golden>();
        double costGrowth = 1.15;
        int[] milestones = new int[]{10, 25, 50, 100, 150, 200, 250, 300};
        double basePerClick = 1.0;
        double goldenBaseChance = 0.25;
        double pricePerCookie = 0.02;
        double prestigeBase = 1.0E9;
        double prestigeBonus = 2.0;
        double offlineMaxHours = 12.0;
        double critMultiplier = 10.0;
        int boostMaxLevel = 10;
        double boostCostMultiplier = 10.0;
        double boostCostGrowth = 8.0;
        int[] boostNeeds = new int[]{1, 5, 25, 50, 100, 150, 200, 250, 300, 350};

        Rules() {
        }

        double cost(Building building, int n, int n2) {
            double d = building.baseCost() * Math.pow(this.costGrowth, n);
            if (n2 <= 1) {
                return Math.ceil(d);
            }
            return Math.ceil(d * (Math.pow(this.costGrowth, n2) - 1.0) / (this.costGrowth - 1.0));
        }

        int maxAffordable(Building building, int n, double d) {
            int n2;
            double d2 = building.baseCost() * Math.pow(this.costGrowth, n);
            if (d < d2) {
                return 0;
            }
            for (n2 = (int)Math.floor(Math.log(d * (this.costGrowth - 1.0) / d2 + 1.0) / Math.log(this.costGrowth)); n2 > 0 && this.cost(building, n, n2) > d; --n2) {
            }
            while (this.cost(building, n, n2 + 1) <= d && n2 < 100000) {
                ++n2;
            }
            return n2;
        }

        double milestoneMultiplier(int n) {
            int n2 = 0;
            for (int n3 : this.milestones) {
                if (n < n3) continue;
                ++n2;
            }
            return Math.pow(2.0, n2);
        }

        int nextMilestone(int n) {
            for (int n2 : this.milestones) {
                if (n >= n2) continue;
                return n2;
            }
            return -1;
        }

        double buildingCps(Building building, int n) {
            return n <= 0 ? 0.0 : building.cps() * (double)n * this.milestoneMultiplier(n);
        }

        double buildingCps(Profile profile, Building building) {
            return this.buildingCps(building, profile.building(building.id())) * this.boostMultiplier(profile, building);
        }

        double boostMultiplier(Profile profile, Building building) {
            return Math.pow(2.0, profile.upgrade(building.boostKey()));
        }

        double globalMultiplier(Profile profile) {
            return this.prestigeMultiplier(profile) * (1.0 + (double)profile.upgrade("kitchen") * this.level("kitchen") / 100.0);
        }

        double discount(Profile profile) {
            return Math.min(0.9, (double)profile.upgrade("discount") * this.level("discount") / 100.0);
        }

        double price(Profile profile, Building building, int n) {
            return Math.ceil(this.cost(building, profile.building(building.id()), n) * (1.0 - this.discount(profile)));
        }

        int maxAffordable(Profile profile, Building building) {
            int n;
            int n2 = profile.building(building.id());
            for (n = this.maxAffordable(building, n2, profile.cookies / (1.0 - this.discount(profile))); n > 0 && this.price(profile, building, n) > profile.cookies; --n) {
            }
            return n;
        }

        double boostCost(Building building, int n) {
            return Math.ceil(building.baseCost() * this.boostCostMultiplier * Math.pow(this.boostCostGrowth, n));
        }

        int boostNeeds(int n) {
            if (n >= this.boostMaxLevel) {
                return -1;
            }
            return n < this.boostNeeds.length ? this.boostNeeds[n] : this.boostNeeds[this.boostNeeds.length - 1] + 50 * (n - this.boostNeeds.length + 1);
        }

        double prestigeMultiplier(Profile profile) {
            return 1.0 + (double)profile.prestige * this.prestigeBonus / 100.0;
        }

        double baseCps(Profile profile) {
            double d = 0.0;
            for (Building building : this.buildings.values()) {
                d += this.buildingCps(profile, building);
            }
            return d * this.globalMultiplier(profile);
        }

        double frenzyMultiplier(Profile profile, long l) {
            if (profile.frenzyUntil <= l) {
                return 1.0;
            }
            Golden golden = this.golden("frenzy");
            return golden == null ? 7.0 : golden.multiplier();
        }

        double clickFrenzyMultiplier(Profile profile, long l) {
            if (profile.clickFrenzyUntil <= l) {
                return 1.0;
            }
            Golden golden = this.golden("click-frenzy");
            return golden == null ? 777.0 : golden.multiplier();
        }

        double cps(Profile profile, long l) {
            return this.baseCps(profile) * this.frenzyMultiplier(profile, l);
        }

        double perClick(Profile profile, long l) {
            double d = (this.basePerClick + (double)profile.upgrade("click") * this.level("click")) * this.globalMultiplier(profile);
            double d2 = this.cps(profile, l) * (double)profile.upgrade("synergy") * this.level("synergy") / 100.0;
            return (d + d2) * this.clickFrenzyMultiplier(profile, l);
        }

        double goldenChance(Profile profile) {
            return this.goldenBaseChance + (double)profile.upgrade("golden") * this.level("golden");
        }

        double critChance(Profile profile) {
            return Math.min(100.0, (double)profile.upgrade("crit") * this.level("crit"));
        }

        double autoClicks(Profile profile) {
            return (double)profile.upgrade("autoclick") * this.level("autoclick");
        }

        double goldenPower(Profile profile) {
            return 1.0 + (double)profile.upgrade("golden-power") * this.level("golden-power") / 100.0;
        }

        double offlineHours(Profile profile) {
            return this.offlineMaxHours + (double)profile.upgrade("nap") * this.level("nap");
        }

        double sellPrice(Profile profile) {
            return this.pricePerCookie * (1.0 + (double)profile.upgrade("lucky") * this.level("lucky") / 100.0);
        }

        double offlinePercent(Profile profile) {
            return Math.min(100.0, (double)profile.upgrade("oven") * this.level("oven"));
        }

        double offlineEarnings(Profile profile, long l) {
            double d = Math.min((double)l / 1000.0, this.offlineHours(profile) * 3600.0);
            if (d <= 0.0) {
                return 0.0;
            }
            return this.baseCps(profile) * this.offlinePercent(profile) / 100.0 * d;
        }

        double level(String string) {
            Upgrade upgrade = this.upgrades.get(string);
            return upgrade == null ? 0.0 : upgrade.perLevel();
        }

        double upgradeCost(Upgrade upgrade, int n) {
            return Math.ceil(upgrade.baseCost() * Math.pow(upgrade.growth(), n));
        }

        int prestigeFor(double d) {
            if (d < this.prestigeBase) {
                return 0;
            }
            return (int)Math.floor(Math.cbrt(d / this.prestigeBase) + 1.0E-9);
        }

        int prestigeGain(Profile profile) {
            return Math.max(0, this.prestigeFor(profile.allTime) - profile.prestige);
        }

        double nextPrestigeAt(Profile profile) {
            int n = Math.max(this.prestigeFor(profile.allTime), profile.prestige) + 1;
            return this.prestigeBase * Math.pow(n, 3.0);
        }

        Golden golden(String string) {
            for (Golden golden : this.golden) {
                if (!golden.id().equals(string)) continue;
                return golden;
            }
            return null;
        }

        Golden rollGolden(Random random) {
            double d = 0.0;
            for (Golden golden : this.golden) {
                d += golden.weight();
            }
            double d2 = random.nextDouble() * d;
            for (Golden golden : this.golden) {
                if (!((d2 -= golden.weight()) <= 0.0)) continue;
                return golden;
            }
            return this.golden.isEmpty() ? null : this.golden.get(0);
        }

        double luckyReward(Golden golden, Profile profile, long l) {
            double d = profile.cookies * golden.bankPercent() / 100.0;
            double d2 = this.baseCps(profile) * golden.cpsSeconds();
            double d3 = Math.min(d, d2);
            return (Math.max(d3, this.perClick(profile, l) * golden.minClicks()) + 13.0) * this.goldenPower(profile);
        }
    }

    /** Max level per upgrade from the config; levels bought before a cap was lowered count only up to it. Missing = upgrade disabled. */
    static int cap(String id) {
        org.bukkit.plugin.Plugin p = org.bukkit.Bukkit.getPluginManager().getPlugin("CookieClicker");
        if (p == null) return Integer.MAX_VALUE;
        return Math.max(0, p.getConfig().getInt("upgrades." + id + ".max-level", Integer.MAX_VALUE));
    }

    static final class Profile {
        String name;
        double cookies;
        double baked;
        double allTime;
        long clicks;
        long golden;
        double sold;
        double money;
        int prestige;
        long lastSeen;
        String sellDay = "";
        double soldToday;
        final Map<String, Integer> buildings = new ConcurrentHashMap<String, Integer>();
        final Map<String, Integer> upgrades = new ConcurrentHashMap<String, Integer>();
        transient long frenzyUntil;
        transient long clickFrenzyUntil;
        final transient ArrayDeque<Long> clickTimes = new ArrayDeque();
        transient boolean dirty;
        transient long lastFloat;

        Profile() {
        }

        int building(String string) {
            return this.buildings.getOrDefault(string, 0);
        }

        int upgrade(String string) {
            int v = this.upgrades.getOrDefault(string, 0);
            return Math.min(v, cap(string));
        }

        int buildingCount() {
            int n = 0;
            for (int n2 : this.buildings.values()) {
                n += n2;
            }
            return n;
        }

        void earn(double d) {
            if (d <= 0.0 || Double.isNaN(d) || Double.isInfinite(d)) {
                return;
            }
            this.cookies += d;
            this.baked += d;
            this.allTime += d;
            this.dirty = true;
        }

        Profile copy() {
            Profile profile = new Profile();
            profile.name = this.name;
            profile.cookies = this.cookies;
            profile.baked = this.baked;
            profile.allTime = this.allTime;
            profile.clicks = this.clicks;
            profile.golden = this.golden;
            profile.sold = this.sold;
            profile.money = this.money;
            profile.prestige = this.prestige;
            profile.lastSeen = this.lastSeen;
            profile.sellDay = this.sellDay;
            profile.soldToday = this.soldToday;
            profile.buildings.putAll(this.buildings);
            profile.upgrades.putAll(this.upgrades);
            return profile;
        }
    }

    record Golden(String id, double weight, double multiplier, int seconds, double bankPercent, double cpsSeconds, double minClicks) {
    }

    record Upgrade(String id, String name, Material icon, double baseCost, double growth, int maxLevel, double perLevel, String description) {
    }

    record Building(String id, String name, Material icon, double baseCost, double cps, String description, int index, String upgradeName) {
        String boostKey() {
            return "b_" + this.id;
        }
    }
}

