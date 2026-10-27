package fairplay;

import java.util.Optional;

final class AmmoCheck extends Check {
    private final int magazine;
    private final int tolerance;

    AmmoCheck(Params p) {
        super("ammo", p, 1);
        magazine = (int) p.get("ammo.magazine", Game.MAGAZINE);
        tolerance = (int) p.get("ammo.tolerance", 0);
    }

    @Override
    Optional<String> inspect(Context c) {
        if (c.order() != Order.NEW) return Optional.empty();
        Packet p = c.packet();
        PlayerState s = c.state();
        int base = p.reloads() > s.reloadsSeen ? p.shots() : s.shotsAtReload;
        int expected = magazine - (p.shots() - base);
        if (Math.abs(p.ammo() - expected) <= tolerance) return Optional.empty();
        return Optional.of("claims " + p.ammo() + " rounds, expected " + expected);
    }
}
