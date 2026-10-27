package fairplay;

import java.util.Optional;

final class TeleportCheck extends Check {
    private final double maxSpeed, tolerance, slackMs, minJump;

    TeleportCheck(Params p) {
        super("teleport", p, 3);
        maxSpeed = p.get("speed.max", Game.MAX_SPEED);
        tolerance = p.get("speed.tolerance", 1.1);
        slackMs = p.get("speed.slackMs", 100);
        minJump = p.get("teleport.minJump", 12);
    }

    @Override
    Optional<String> inspect(Context c) {
        Sample prev = c.state().last;
        if (c.order() != Order.NEW || prev == null) return Optional.empty();
        double dt = Math.max(0, elapsedMs(prev, c.packet(), c.arrivalMs(), slackMs));
        double dist = Math.hypot(c.packet().x() - prev.x(), c.packet().y() - prev.y());
        double limit = maxSpeed * tolerance * dt / 1000 + minJump;
        if (dist <= limit) return Optional.empty();
        return Optional.of(String.format("jumped %.1f units in %.0f ms", dist, dt));
    }
}
