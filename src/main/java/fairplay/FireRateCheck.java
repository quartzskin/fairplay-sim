package fairplay;

import java.util.Optional;

final class FireRateCheck extends Check {
    private final double cooldownMs, tolerance, burst, windowMs, slackMs;

    FireRateCheck(Params p) {
        super("fire", p, 1);
        cooldownMs = p.get("fire.cooldownMs", Game.FIRE_COOLDOWN_MS);
        tolerance = p.get("fire.tolerance", 0.15);
        burst = p.get("fire.burst", 1);
        windowMs = p.get("fire.windowMs", 1000);
        slackMs = p.get("fire.slackMs", 100);
    }

    @Override
    Optional<String> inspect(Context c) {
        if (c.order() != Order.NEW) return Optional.empty();
        Sample a = c.state().anchor(c.packet().clientMs(), (long) windowMs);
        if (a == null) return Optional.empty();
        double dt = elapsedMs(a, c.packet(), c.arrivalMs(), slackMs);
        int shots = c.packet().shots() - a.shots();
        double allowed = dt / cooldownMs * (1 + tolerance) + burst;
        if (shots <= allowed) return Optional.empty();
        return Optional.of(String.format("%d shots in %.0f ms (allowed %.1f)", shots, dt, allowed));
    }
}
