package fairplay;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

final class Client {
    private final int id;
    private final Random rng;
    private final Cheat cheat;
    private final long cheatStartMs;

    private double x, y, heading, speed;
    private long seq;
    private int shots, reloads, ammo = Game.MAGAZINE;
    private long reloadUntil, nextTeleport;
    private int engaged, cooldown;
    private final List<Packet> sent = new ArrayList<>();
    private final List<Packet> held = new ArrayList<>();

    Client(int id, Random rng, Cheat cheat, long cheatStartMs) {
        this.id = id;
        this.rng = rng;
        this.cheat = cheat;
        this.cheatStartMs = cheatStartMs;
        x = 100 + rng.nextDouble() * 800;
        y = 100 + rng.nextDouble() * 800;
        pickHeading();
        nextTeleport = cheatStartMs + cheat.teleportEveryMs();
    }

    private void pickHeading() {
        heading = rng.nextDouble() * 2 * Math.PI;
        speed = 3 + rng.nextDouble() * 3.5;
    }

    List<Packet> tick(long now, int observedHealth) {
        boolean cheating = now >= cheatStartMs;
        if (rng.nextDouble() < 0.02) pickHeading();
        move(now, cheating);
        fire(now, cheating);

        int health = cheating && cheat.godHealth() ? Game.MAX_HEALTH : observedHealth;
        Packet p = new Packet(id, seq++, now, x, y, health, ammo, shots, reloads);
        return cheating ? tamper(p) : List.of(p);
    }

    private void move(long now, boolean cheating) {
        double v = speed * (cheating ? cheat.speedFactor() : 1) * Game.TICK_MS / 1000;
        x += Math.cos(heading) * v;
        y += Math.sin(heading) * v;
        if (x < 0 || x > Game.ARENA) heading = Math.PI - heading;
        if (y < 0 || y > Game.ARENA) heading = -heading;
        x = Math.max(0, Math.min(Game.ARENA, x));
        y = Math.max(0, Math.min(Game.ARENA, y));

        if (cheating && cheat.teleportEveryMs() > 0 && now >= nextTeleport) {
            double dir = Math.atan2(Game.ARENA / 2 - y, Game.ARENA / 2 - x) + (rng.nextDouble() - 0.5);
            x = Math.max(0, Math.min(Game.ARENA, x + Math.cos(dir) * cheat.teleportDist()));
            y = Math.max(0, Math.min(Game.ARENA, y + Math.sin(dir) * cheat.teleportDist()));
            nextTeleport = now + cheat.teleportEveryMs();
        }
    }

    private void fire(long now, boolean cheating) {
        if (engaged > 0) engaged--;
        else if (rng.nextDouble() < 0.015) engaged = 40 + rng.nextInt(60);
        if (cooldown > 0) cooldown--;
        if (engaged == 0 || now < reloadUntil) return;

        boolean endless = cheating && cheat.infiniteAmmo();
        if (ammo == 0 && !endless) {
            reloads++;
            ammo = Game.MAGAZINE;
            reloadUntil = now + Game.RELOAD_MS;
        } else if (cooldown == 0) {
            shots++;
            if (!endless) ammo--;
            double factor = cheating ? cheat.fireFactor() : 1;
            cooldown = Math.max(1, (int) Math.round((6 + rng.nextInt(5)) * factor));
        }
    }

    private List<Packet> tamper(Packet p) {
        if (cheat.holdPackets() > 0) {
            held.add(p);
            if (held.size() < cheat.holdPackets()) return List.of();
            List<Packet> out = new ArrayList<>(held);
            java.util.Collections.reverse(out);
            held.clear();
            return out;
        }
        List<Packet> out = new ArrayList<>(2);
        out.add(p);
        if (cheat.replayProb() > 0 && sent.size() > 40 && rng.nextDouble() < cheat.replayProb()) {
            out.add(sent.get(rng.nextInt(sent.size() - 1)));
        }
        sent.add(p);
        if (sent.size() > 300) sent.remove(0);
        return out;
    }
}
