package net.chickensmp.acplus;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/** Everything we remember about one player. Access is synchronized on the instance. */
final class PlayerData {

    record Flag(long time, String check, String category, double points, int vl, String info) {}

    private static final int MAX_RECENT = 50;

    final UUID uuid;
    String name;
    final Deque<Flag> recent = new ArrayDeque<>();
    final Map<String, Integer> totals = new HashMap<>();
    int totalFlags;
    double totalPoints;
    int susTimes;
    long firstFlag;
    long lastFlag;
    long lastAlert;
    boolean sus;

    PlayerData(UUID uuid, String name) {
        this.uuid = uuid;
        this.name = name;
    }

    synchronized void add(Flag f) {
        recent.addLast(f);
        while (recent.size() > MAX_RECENT) recent.removeFirst();
        totals.merge(f.check(), 1, Integer::sum);
        totalFlags++;
        totalPoints += f.points();
        if (firstFlag == 0) firstFlag = f.time();
        lastFlag = f.time();
    }

    synchronized void prune(long cutoff) {
        while (!recent.isEmpty() && recent.peekFirst().time() < cutoff) recent.removeFirst();
    }

    /** All-time sus score: points never expire. */
    synchronized double score() {
        return totalPoints;
    }

    /** Checks in the window, most frequent first, e.g. {"KillAura (A)": 6}. */
    synchronized Map<String, Integer> recentChecks(long cutoff) {
        prune(cutoff);
        Map<String, Integer> m = new HashMap<>();
        for (Flag f : recent) m.merge(f.check(), 1, Integer::sum);
        return sortDesc(m);
    }

    synchronized Map<String, Integer> totalChecks() {
        return sortDesc(totals);
    }

    synchronized List<Flag> lastFlags(int n) {
        return recent.stream().skip(Math.max(0, recent.size() - n)).collect(Collectors.toList());
    }

    synchronized int recentCount(long cutoff) {
        prune(cutoff);
        return recent.size();
    }

    static Map<String, Integer> sortDesc(Map<String, Integer> m) {
        return m.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, LinkedHashMap::new));
    }

    static String top(Map<String, Integer> checks, int n) {
        if (checks.isEmpty()) return "-";
        return checks.entrySet().stream().limit(n)
                .map(e -> e.getKey() + " x" + e.getValue())
                .collect(Collectors.joining(", "));
    }
}
