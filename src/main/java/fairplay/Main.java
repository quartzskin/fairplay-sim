package fairplay;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class Main {
    private Main() {}

    public static void main(String[] args) throws IOException {
        long seed = 1;
        int players = 50;
        long durationMs = 30_000;
        String net = "typical";
        String html = "fairplay-report.html";
        Map<String, Double> overrides = new HashMap<>();

        try {
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--seed" -> seed = Long.parseLong(args[++i]);
                case "--players" -> players = Integer.parseInt(args[++i]);
                case "--duration" -> durationMs = Long.parseLong(args[++i]) * 1000;
                case "--net" -> net = args[++i];
                case "--html" -> html = args[++i];
                case "--param" -> {
                    String[] kv = args[++i].split("=", 2);
                    if (kv.length != 2) usage();
                    overrides.put(kv[0], Double.parseDouble(kv[1]));
                }
                default -> usage();
            }
        }
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
            System.err.println("error: invalid or missing option value");
            System.exit(2);
        }
        if (durationMs < 15_000) {
            System.err.println("duration must be at least 15 seconds");
            System.exit(2);
        }

        Params params = new Params(overrides);
        Simulation.Config cfg = new Simulation.Config(players, durationMs, 10_000, seed, params);
        NetProfile cheatNet = NetProfile.named(net);

        List<Scorecard> cheats = new ArrayList<>();
        for (Scenario sc : Scenario.CHEATS) cheats.add(Scorecard.of(Simulation.run(sc, cheatNet, cfg)));
        List<Scorecard> legit = new ArrayList<>();
        for (NetProfile n : NetProfile.ALL) legit.add(Scorecard.of(Simulation.run(Scenario.LEGIT, n, cfg)));

        for (String k : params.unused().keySet()) System.err.println("warning: unknown parameter " + k);

        System.out.printf("seed %d, %d cheaters + %d legit per scenario, %d s, cheats start at 10 s, network %s%n%n",
                seed, players, players, durationMs / 1000, net);
        Scorecard.printCheats(cheats);
        System.out.println();
        System.out.println("legit traffic only:");
        Scorecard.printFalsePositives(legit);

        if (!html.isEmpty()) {
            Files.writeString(Path.of(html), HtmlReport.render(cfg, cheatNet, cheats, legit, params.used()));
            System.out.println("\nwrote " + html);
        }
    }

    private static void usage() {
        System.err.println("usage: java -jar fairplay-sim.jar [--seed N] [--players N] [--duration SECONDS]"
                + " [--net lan|typical|poor|bad] [--html FILE] [--param key=value ...]");
        System.exit(2);
    }
}
