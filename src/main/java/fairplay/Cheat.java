package fairplay;

record Cheat(double speedFactor, long teleportEveryMs, double teleportDist, boolean godHealth,
             boolean infiniteAmmo, double fireFactor, double replayProb, int holdPackets) {
    static final Cheat NONE = new Cheat(1, 0, 0, false, false, 1, 0, 0);

    Cheat speed(double f) {
        return new Cheat(f, teleportEveryMs, teleportDist, godHealth, infiniteAmmo, fireFactor, replayProb, holdPackets);
    }

    Cheat teleport(long everyMs, double dist) {
        return new Cheat(speedFactor, everyMs, dist, godHealth, infiniteAmmo, fireFactor, replayProb, holdPackets);
    }

    Cheat god() {
        return new Cheat(speedFactor, teleportEveryMs, teleportDist, true, infiniteAmmo, fireFactor, replayProb, holdPackets);
    }

    Cheat endlessAmmo() {
        return new Cheat(speedFactor, teleportEveryMs, teleportDist, godHealth, true, fireFactor, replayProb, holdPackets);
    }

    Cheat fire(double f) {
        return new Cheat(speedFactor, teleportEveryMs, teleportDist, godHealth, infiniteAmmo, f, replayProb, holdPackets);
    }

    Cheat replay(double prob) {
        return new Cheat(speedFactor, teleportEveryMs, teleportDist, godHealth, infiniteAmmo, fireFactor, prob, holdPackets);
    }

    Cheat hold(int packets) {
        return new Cheat(speedFactor, teleportEveryMs, teleportDist, godHealth, infiniteAmmo, fireFactor, replayProb, packets);
    }
}
