package fairplay;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

final class Server {
    private final List<Check> checks;
    private final Map<Integer, PlayerState> players = new HashMap<>();
    private final int window;
    private final int strikeLimit;
    private final long strikeWindowMs;

    Server(Params params) {
        this(params, defaultChecks(params));
    }

    Server(Params params, List<Check> checks) {
        this.checks = checks;
        window = (int) params.get("seq.window", 32);
        strikeLimit = (int) params.get("server.strikes", 3);
        strikeWindowMs = (long) params.get("server.strikeWindowMs", 2000);
    }

    static List<Check> defaultChecks(Params p) {
        return List.of(new SequenceCheck(p), new SpeedCheck(p), new TeleportCheck(p),
                new FireRateCheck(p), new AmmoCheck(p), new HealthCheck(p));
    }

    PlayerState player(int id) {
        return players.computeIfAbsent(id, PlayerState::new);
    }

    void damage(int id, int amount, long now) {
        PlayerState s = player(id);
        int h = s.health() - amount;
        s.setHealth(h > 0 ? h : Game.MAX_HEALTH, now);
    }

    int healthAt(int id, long t) {
        return player(id).healthAt(t);
    }

    void receive(Packet p, long arrivalMs) {
        PlayerState s = player(p.player());
        Order order = s.classify(p.seq(), window);
        Context ctx = new Context(s, p, arrivalMs, order);
        for (Check check : checks) {
            Optional<String> hit = check.inspect(ctx);
            if (hit.isEmpty()) continue;
            s.violations.merge(check.name, 1, Integer::sum);
            boolean over = s.addStrike(arrivalMs, check.weight, strikeWindowMs, strikeLimit);
            if (over && s.flaggedAtMs < 0) {
                s.flaggedAtMs = arrivalMs;
                s.flaggedBy = check.name;
            }
        }
        if (order == Order.NEW) s.accept(p, arrivalMs);
    }
}
