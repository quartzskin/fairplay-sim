package fairplay;

import java.util.Optional;

abstract class Check {
    final String name;
    final int weight;

    Check(String name, Params params, int defaultWeight) {
        this.name = name;
        this.weight = (int) params.get(name + ".weight", defaultWeight);
    }

    abstract Optional<String> inspect(Context c);

    // Client clocks can be trusted only as far as arrivals allow, so the shorter span wins.
    static double elapsedMs(Sample a, Packet p, long arrivalMs, double slackMs) {
        return Math.min(p.clientMs() - a.clientMs(), arrivalMs - a.arrivalMs() + slackMs);
    }
}
