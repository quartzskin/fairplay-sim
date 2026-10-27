package fairplay;

import java.util.Map;
import java.util.TreeMap;

final class Params {
    private final Map<String, Double> overrides;
    private final Map<String, Double> used = new TreeMap<>();

    Params(Map<String, Double> overrides) {
        this.overrides = overrides;
    }

    static Params defaults() {
        return new Params(Map.of());
    }

    double get(String key, double def) {
        double v = overrides.getOrDefault(key, def);
        used.put(key, v);
        return v;
    }

    Map<String, Double> used() {
        return used;
    }

    Map<String, Double> unused() {
        Map<String, Double> m = new TreeMap<>(overrides);
        m.keySet().removeAll(used.keySet());
        return m;
    }
}
