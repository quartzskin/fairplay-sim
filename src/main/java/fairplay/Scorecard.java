package fairplay;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

record Scorecard(String scenario, String net, int players, int cheaters, int detected,
                 int falseFlags, double medianTtdMs, double p95TtdMs, double legitViolationsPerMin,
                 String rules) {

    double detectionRate() {
        return cheaters == 0 ? Double.NaN : (double) detected / cheaters;
    }

    double falsePositiveRate() {
        return (double) falseFlags / players;
    }

    static Scorecard of(Simulation.Result r) {
        long start = r.config().cheatStartMs();
        int cheaters = 0, detected = 0, falseFlags = 0;
        long legitViolations = 0;
        int legitPlayers = 0;
        List<Long> ttd = new ArrayList<>();
        Map<String, Integer> rules = new TreeMap<>();
        for (Simulation.Outcome o : r.outcomes()) {
            boolean flagged = o.flaggedAtMs() >= 0;
            if (o.cheater()) {
                cheaters++;
                if (flagged && o.flaggedAtMs() >= start) {
                    detected++;
                    ttd.add(o.flaggedAtMs() - start);
                    rules.merge(o.flaggedBy(), 1, Integer::sum);
                } else if (flagged) {
                    falseFlags++;
                }
            } else {
                legitPlayers++;
                legitViolations += o.violations();
                if (flagged) falseFlags++;
            }
        }
        double minutes = r.config().durationMs() / 60000.0;
        double vpm = legitPlayers == 0 ? 0 : legitViolations / (legitPlayers * minutes);
        java.util.Collections.sort(ttd);
        return new Scorecard(r.scenario().name(), r.net().name(), r.outcomes().size(), cheaters, detected,
                falseFlags, percentile(ttd, 0.5), percentile(ttd, 0.95), vpm, describe(rules, detected));
    }

    private static double percentile(List<Long> sorted, double q) {
        if (sorted.isEmpty()) return Double.NaN;
        int idx = (int) Math.ceil(q * sorted.size()) - 1;
        return sorted.get(Math.max(0, idx));
    }

    private static String describe(Map<String, Integer> rules, int detected) {
        if (detected == 0) return "-";
        List<Map.Entry<String, Integer>> es = new ArrayList<>(rules.entrySet());
        es.sort((a, b) -> b.getValue() - a.getValue());
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.min(2, es.size()); i++) {
            if (i > 0) sb.append(", ");
            sb.append(es.get(i).getKey()).append(' ').append(Math.round(100.0 * es.get(i).getValue() / detected)).append('%');
        }
        return sb.toString();
    }

    static String pct(double v) {
        return Double.isNaN(v) ? "-" : String.format("%.1f%%", v * 100);
    }

    static String secs(double ms) {
        return Double.isNaN(ms) ? "-" : String.format("%.2fs", ms / 1000);
    }

    static void printCheats(List<Scorecard> rows) {
        System.out.printf("%-14s %8s %9s %9s %9s %9s %s%n", "scenario", "cheaters", "detected", "rate", "ttd p50", "ttd p95", "first trigger");
        for (Scorecard s : rows) {
            System.out.printf("%-14s %8d %9d %9s %9s %9s %s%n", s.scenario, s.cheaters, s.detected,
                    pct(s.detectionRate()), secs(s.medianTtdMs), secs(s.p95TtdMs), s.rules);
        }
    }

    static void printFalsePositives(List<Scorecard> rows) {
        System.out.printf("%-14s %8s %9s %9s %12s%n", "run", "players", "flagged", "fp rate", "viol/min");
        for (Scorecard s : rows) {
            System.out.printf("%-14s %8d %9d %9s %12.2f%n", s.scenario + "/" + s.net, s.players, s.falseFlags,
                    pct(s.falsePositiveRate()), s.legitViolationsPerMin);
        }
    }
}
