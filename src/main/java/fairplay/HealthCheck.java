package fairplay;

import java.util.Optional;

final class HealthCheck extends Check {
    private final long graceMs;
    private final int tolerance;

    HealthCheck(Params p) {
        super("health", p, 1);
        graceMs = (long) p.get("health.graceMs", 400);
        tolerance = (int) p.get("health.tolerance", 0);
    }

    @Override
    Optional<String> inspect(Context c) {
        if (c.order() != Order.NEW) return Optional.empty();
        int allowed = c.state().maxHealthSince(c.arrivalMs() - graceMs);
        if (c.packet().health() <= allowed + tolerance) return Optional.empty();
        return Optional.of("claims " + c.packet().health() + " health, server has at most " + allowed);
    }
}
