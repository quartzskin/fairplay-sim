package fairplay;

import java.util.List;

record Scenario(String name, String description, Cheat cheat) {
    static final Scenario LEGIT = new Scenario("legit", "no cheating", Cheat.NONE);

    static final List<Scenario> CHEATS = List.of(
            new Scenario("speed", "moves at 2x speed", Cheat.NONE.speed(2.0)),
            new Scenario("speed_subtle", "moves at 1.25x speed", Cheat.NONE.speed(1.25)),
            new Scenario("teleport", "40 unit jump every 3 s", Cheat.NONE.teleport(3000, 40)),
            new Scenario("health_hack", "always reports full health", Cheat.NONE.god()),
            new Scenario("ammo_hack", "never runs out of ammo", Cheat.NONE.endlessAmmo()),
            new Scenario("rapid_fire", "fires at 0.4x the cooldown", Cheat.NONE.fire(0.4)),
            new Scenario("replay", "re-sends old packets", Cheat.NONE.replay(0.05)),
            new Scenario("out_of_order", "holds 50 packets, sends them reversed", Cheat.NONE.hold(50)));
}
