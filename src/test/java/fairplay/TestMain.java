package fairplay;

import java.util.ArrayList;
import java.util.List;

public final class TestMain {
    private static int failures;
    private static int run;

    interface Body { void run() throws Exception; }

    static void test(String name, Body body) {
        run++;
        try {
            body.run();
            System.out.println("ok    " + name);
        } catch (Throwable t) {
            failures++;
            System.out.println("FAIL  " + name + ": " + t);
        }
    }

    static void check(boolean cond, String msg) {
        if (!cond) throw new AssertionError(msg);
    }

    static Packet pkt(long seq, long ms, double x, double y) {
        return new Packet(1, seq, ms, x, y, 100, 30, 0, 0);
    }

    static Packet pkt(long seq, long ms, double x, double y, int health, int ammo, int shots, int reloads) {
        return new Packet(1, seq, ms, x, y, health, ammo, shots, reloads);
    }

    static Simulation.Config config(long seed) {
        return new Simulation.Config(30, 30_000, 10_000, seed, Params.defaults());
    }

    static Scorecard score(Scenario sc, String net, long seed) {
        return Scorecard.of(Simulation.run(sc, NetProfile.named(net), config(seed)));
    }

    static Scenario scenario(String name) {
        return Scenario.CHEATS.stream().filter(s -> s.name().equals(name)).findFirst().orElseThrow();
    }

    public static void main(String[] args) {
        test("duplicate packet is flagged, late unseen packet is not", () -> {
            Server s = new Server(Params.defaults());
            s.receive(pkt(0, 0, 0, 0), 50);
            s.receive(pkt(2, 40, 0, 0), 90);
            s.receive(pkt(1, 20, 0, 0), 100);
            check(s.player(1).violations.isEmpty(), "late packet flagged: " + s.player(1).violations);
            s.receive(pkt(1, 20, 0, 0), 110);
            check(s.player(1).violations.get("seq") == 1, "duplicate not flagged");
        });

        test("packet older than the reorder window is stale", () -> {
            Server s = new Server(Params.defaults());
            s.receive(pkt(0, 0, 0, 0), 10);
            s.receive(pkt(100, 2000, 0, 0), 2010);
            s.receive(pkt(5, 100, 0, 0), 2020);
            check(s.player(1).violations.containsKey("seq"), "stale packet accepted");
        });

        test("steady walking at legal speed raises nothing", () -> {
            Server s = new Server(Params.defaults());
            for (int i = 0; i < 200; i++) s.receive(pkt(i, i * 20L, i * 0.1, 0), i * 20L + 40);
            check(s.player(1).violations.isEmpty(), "violations: " + s.player(1).violations);
        });

        test("2x speed over a window is flagged", () -> {
            Server s = new Server(Params.defaults());
            for (int i = 0; i < 200; i++) s.receive(pkt(i, i * 20L, i * 0.28, 0), i * 20L + 40);
            check(s.player(1).flaggedAtMs >= 0, "speedhack not flagged");
            check("speed".equals(s.player(1).flaggedBy), "flagged by " + s.player(1).flaggedBy);
        });

        test("single teleport flags at once", () -> {
            Server s = new Server(Params.defaults());
            for (int i = 0; i < 20; i++) s.receive(pkt(i, i * 20L, 10, 10), i * 20L + 40);
            s.receive(pkt(20, 400, 60, 10), 440);
            check(s.player(1).flaggedAtMs == 440, "flaggedAt=" + s.player(1).flaggedAtMs);
        });

        test("ammo claim must follow the shot counter", () -> {
            Server s = new Server(Params.defaults());
            s.receive(pkt(0, 0, 0, 0, 100, 30, 0, 0), 10);
            s.receive(pkt(1, 20, 0, 0, 100, 29, 1, 0), 30);
            check(s.player(1).violations.isEmpty(), "honest ammo flagged");
            s.receive(pkt(2, 40, 0, 0, 100, 29, 3, 0), 50);
            check(s.player(1).violations.containsKey("ammo"), "bad ammo accepted");
        });

        test("ammo survives a lost reload packet", () -> {
            Server s = new Server(Params.defaults());
            s.receive(pkt(0, 0, 0, 0, 100, 0, 30, 0), 10);
            s.receive(pkt(2, 40, 0, 0, 100, 30, 30, 1), 50);
            check(s.player(1).violations.isEmpty(), "violations: " + s.player(1).violations);
        });

        test("health above the recent server value is flagged after the grace period", () -> {
            Server s = new Server(Params.defaults());
            s.receive(pkt(0, 0, 0, 0), 10);
            s.damage(1, 40, 1000);
            s.receive(pkt(1, 1100, 0, 0, 100, 30, 0, 0), 1200);
            check(s.player(1).violations.isEmpty(), "flagged inside grace period");
            s.receive(pkt(2, 1500, 0, 0, 100, 30, 0, 0), 1600);
            check(s.player(1).violations.containsKey("health"), "stale health accepted");
        });

        test("fire rate above cooldown is flagged", () -> {
            Server s = new Server(Params.defaults());
            for (int i = 0; i < 100; i++) s.receive(pkt(i, i * 20L, 0, 0, 100, 30 - i / 2 % 30, i / 2, 0), i * 20L + 30);
            check(s.player(1).violations.containsKey("fire"), "violations: " + s.player(1).violations);
        });

        test("custom check list is honoured", () -> {
            Params p = Params.defaults();
            Server s = new Server(p, List.of(new SequenceCheck(p)));
            s.receive(pkt(0, 0, 0, 0), 10);
            s.receive(pkt(1, 20, 900, 900), 30);
            check(s.player(1).violations.isEmpty(), "teleport check ran anyway");
        });

        test("same seed gives identical scorecards", () -> {
            Scorecard a = score(scenario("speed"), "typical", 7);
            Scorecard b = score(scenario("speed"), "typical", 7);
            check(a.equals(b), a + " vs " + b);
        });

        test("blatant cheats are caught on the typical network", () -> {
            for (String n : List.of("speed", "teleport", "health_hack", "ammo_hack", "rapid_fire", "replay", "out_of_order")) {
                Scorecard s = score(scenario(n), "typical", 3);
                check(s.detectionRate() >= 0.9, n + " detection " + s.detectionRate());
            }
        });

        test("legit traffic on lan and typical networks is never flagged", () -> {
            for (String n : List.of("lan", "typical")) {
                Scorecard s = score(Scenario.LEGIT, n, 5);
                check(s.falseFlags() == 0, n + " flagged " + s.falseFlags());
            }
        });

        test("detection happens after cheating starts", () -> {
            Scorecard s = score(scenario("teleport"), "lan", 2);
            check(s.medianTtdMs() >= 0 && s.medianTtdMs() < 5000, "ttd " + s.medianTtdMs());
        });

        System.out.println(run - failures + "/" + run + " passed");
        if (failures > 0) System.exit(1);
    }
}
