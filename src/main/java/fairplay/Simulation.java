package fairplay;

import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Random;

final class Simulation {
    record Config(int players, long durationMs, long cheatStartMs, long seed, Params params) {}

    record Outcome(boolean cheater, long flaggedAtMs, String flaggedBy, int violations) {}

    record Result(Scenario scenario, NetProfile net, Config config, List<Outcome> outcomes) {}

    private record Delivery(long arrivalMs, long order, Packet packet) {}

    private Simulation() {}

    static Result run(Scenario sc, NetProfile net, Config cfg) {
        Random master = new Random(cfg.seed() * 31 + sc.name().hashCode() * 17L + net.name().hashCode());
        Random linkRng = new Random(master.nextLong());
        Random damageRng = new Random(master.nextLong());
        Server server = new Server(cfg.params());

        boolean legitOnly = sc.cheat() == Cheat.NONE;
        int cheaters = legitOnly ? 0 : cfg.players();
        int total = legitOnly ? cfg.players() : cfg.players() * 2;
        List<Client> clients = new ArrayList<>();
        for (int i = 0; i < total; i++) {
            Cheat c = i < cheaters ? sc.cheat() : Cheat.NONE;
            clients.add(new Client(i, new Random(master.nextLong()), c, cfg.cheatStartMs()));
        }

        PriorityQueue<Delivery> queue = new PriorityQueue<>(
                (a, b) -> a.arrivalMs() != b.arrivalMs()
                        ? Long.compare(a.arrivalMs(), b.arrivalMs()) : Long.compare(a.order(), b.order()));
        long order = 0;
        for (long t = 0; t < cfg.durationMs(); t += Game.TICK_MS) {
            drain(queue, server, t);
            for (int i = 0; i < total; i++) {
                if (damageRng.nextDouble() < 0.01) server.damage(i, 5 + damageRng.nextInt(16), t);
            }
            for (int i = 0; i < total; i++) {
                int seen = server.healthAt(i, t - net.delay(linkRng));
                for (Packet p : clients.get(i).tick(t, seen)) {
                    if (net.drops(linkRng)) continue;
                    queue.add(new Delivery(t + net.delay(linkRng), order++, p));
                }
            }
        }
        drain(queue, server, Long.MAX_VALUE);

        List<Outcome> outcomes = new ArrayList<>();
        for (int i = 0; i < total; i++) {
            PlayerState s = server.player(i);
            int v = s.violations.values().stream().mapToInt(Integer::intValue).sum();
            outcomes.add(new Outcome(i < cheaters, s.flaggedAtMs, s.flaggedBy, v));
        }
        return new Result(sc, net, cfg, outcomes);
    }

    private static void drain(PriorityQueue<Delivery> q, Server server, long upTo) {
        while (!q.isEmpty() && q.peek().arrivalMs() <= upTo) {
            Delivery d = q.poll();
            server.receive(d.packet(), d.arrivalMs());
        }
    }
}
