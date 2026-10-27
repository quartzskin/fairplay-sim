package fairplay;

import java.util.List;
import java.util.Random;

record NetProfile(String name, double baseMs, double jitterMs, double loss) {
    static final List<NetProfile> ALL = List.of(
            new NetProfile("lan", 15, 2, 0),
            new NetProfile("typical", 50, 10, 0.01),
            new NetProfile("poor", 100, 30, 0.05),
            new NetProfile("bad", 180, 60, 0.10));

    static NetProfile named(String name) {
        return ALL.stream().filter(n -> n.name.equals(name)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("unknown network profile: " + name));
    }

    long delay(Random rng) {
        return Math.max(1, Math.round(baseMs + rng.nextGaussian() * jitterMs));
    }

    boolean drops(Random rng) {
        return rng.nextDouble() < loss;
    }
}
