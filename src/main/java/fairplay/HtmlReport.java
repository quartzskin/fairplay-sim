package fairplay;

import java.util.List;
import java.util.Map;

final class HtmlReport {
    private HtmlReport() {}

    static String render(Simulation.Config cfg, NetProfile net, List<Scorecard> cheats,
                         List<Scorecard> legit, Map<String, Double> params) {
        StringBuilder h = new StringBuilder();
        h.append("<!doctype html><html lang=\"en\"><head><meta charset=\"utf-8\"><title>fairplay-sim report</title><style>")
                .append("body{font:14px/1.4 system-ui,sans-serif;max-width:960px;margin:2em auto;padding:0 1em}")
                .append("table{border-collapse:collapse;width:100%;margin:.6em 0 1.4em}")
                .append("th,td{padding:4px 8px;border-bottom:1px solid #ccc;text-align:right}")
                .append("th:first-child,td:first-child,td.l,th.l{text-align:left}")
                .append("</style></head><body><h1>fairplay-sim report</h1>");
        h.append("<p>seed ").append(cfg.seed()).append(", ").append(cfg.players()).append(" cheaters and ")
                .append(cfg.players()).append(" legit players per scenario, ").append(cfg.durationMs() / 1000)
                .append(" s per run, cheating starts at ").append(cfg.cheatStartMs() / 1000)
                .append(" s. Cheat scenarios ran on the <code>").append(esc(net.name())).append("</code> network (")
                .append(fmt(net.baseMs())).append(" ms base, ").append(fmt(net.jitterMs())).append(" ms jitter, ")
                .append(fmt(net.loss() * 100)).append("% loss).</p>");

        h.append("<h2>Detection per scenario</h2><table><tr><th>scenario</th><th>cheaters</th><th>detected</th>")
                .append("<th class=\"l\">rate</th><th>ttd p50</th><th>ttd p95</th><th>false flags</th><th class=\"l\">first trigger</th></tr>");
        for (Scorecard s : cheats) {
            h.append("<tr><td>").append(esc(s.scenario())).append("</td><td>").append(s.cheaters())
                    .append("</td><td>").append(s.detected()).append("</td><td class=\"l\">")
                    .append("").append(Scorecard.pct(s.detectionRate()))
                    .append("</td><td>").append(Scorecard.secs(s.medianTtdMs())).append("</td><td>")
                    .append(Scorecard.secs(s.p95TtdMs())).append("</td><td>").append(s.falseFlags())
                    .append("</td><td class=\"l\">").append(esc(s.rules())).append("</td></tr>");
        }
        h.append("</table>");

        h.append("<h2>False positives, legit players only</h2><table><tr><th>network</th><th>players</th>")
                .append("<th>flagged</th><th class=\"l\">fp rate</th><th>violations / player-min</th></tr>");
        for (Scorecard s : legit) {
            h.append("<tr><td>").append(esc(s.net())).append("</td><td>").append(s.players()).append("</td><td>")
                    .append(s.falseFlags()).append("</td><td class=\"l\">").append("")
                    .append(Scorecard.pct(s.falsePositiveRate())).append("</td><td>")
                    .append(String.format("%.2f", s.legitViolationsPerMin())).append("</td></tr>");
        }
        h.append("</table>");

        h.append("<h2>Thresholds used</h2><table><tr><th>parameter</th><th>value</th></tr>");
        params.forEach((k, v) -> h.append("<tr><td>").append(esc(k)).append("</td><td>").append(fmt(v)).append("</td></tr>"));
        h.append("</table></body></html>\n");
        return h.toString();
    }

    private static String fmt(double v) {
        return v == Math.rint(v) ? String.valueOf((long) v) : String.valueOf(v);
    }

    private static String esc(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
