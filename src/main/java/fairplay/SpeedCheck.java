package fairplay;

import java.util.Optional;

final class SpeedCheck extends Check {
    private final double maxSpeed, tolerance, windowMs, slackMs, epsilon;

    SpeedCheck(Params p) {
        super("speed", p, 1);
        maxSpeed = p.get("speed.max", Game.MAX_SPEED);
        tolerance = p.get("speed.tolerance", 1.1);
        windowMs = p.get("speed.windowMs", 750);
        slackMs = p.get("speed.slackMs", 100);
        epsilon = p.get("speed.epsilon", 0.3);
    }

    @Override
    Optional<String> inspect(Context c) {
        if (c.order() != Order.NEW) return Optional.empty();
        Sample a = c.state().anchor(c.packet().clientMs(), (long) windowMs);
        if (a == null) return Optional.empty();
        double dt = elapsedMs(a, c.packet(), c.arrivalMs(), slackMs);
        double dist = Math.hypot(c.packet().x() - a.x(), c.packet().y() - a.y());
        double limit = maxSpeed * tolerance * dt / 1000 + epsilon;
        if (dist <= limit) return Optional.empty();
        return Optional.of(String.format("%.1f units in %.0f ms (limit %.1f)", dist, dt, limit));
    }
}
