package fairplay;

import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

final class PlayerState {
    final int id;

    private long highestSeq = -1;
    private long seenMask;

    final ArrayDeque<Sample> history = new ArrayDeque<>();
    Sample last;
    int reloadsSeen;
    int shotsAtReload;

    private final ArrayDeque<long[]> healthLog = new ArrayDeque<>();
    private final ArrayDeque<long[]> strikes = new ArrayDeque<>();
    private int strikeTotal;

    final Map<String, Integer> violations = new TreeMap<>();
    long flaggedAtMs = -1;
    String flaggedBy;

    PlayerState(int id) {
        this.id = id;
        healthLog.add(new long[] {Long.MIN_VALUE, Game.MAX_HEALTH});
    }

    Order classify(long seq, int window) {
        if (highestSeq < 0 || seq > highestSeq) {
            long d = highestSeq < 0 ? 64 : seq - highestSeq;
            seenMask = d >= 64 ? 1 : (seenMask << d) | 1;
            highestSeq = seq;
            return Order.NEW;
        }
        long back = highestSeq - seq;
        if (back >= Math.min(window, 64)) return Order.STALE;
        long bit = 1L << back;
        if ((seenMask & bit) != 0) return Order.DUPLICATE;
        seenMask |= bit;
        return Order.LATE;
    }

    void accept(Packet p, long arrivalMs) {
        Sample s = new Sample(p.clientMs(), arrivalMs, p.x(), p.y(), p.shots());
        history.addLast(s);
        last = s;
        while (history.size() > 1 && history.peekFirst().clientMs() < p.clientMs() - 4000) history.pollFirst();
        if (p.reloads() > reloadsSeen) {
            reloadsSeen = p.reloads();
            shotsAtReload = p.shots();
        }
    }

    Sample anchor(long clientMs, long windowMs) {
        Iterator<Sample> it = history.descendingIterator();
        while (it.hasNext()) {
            Sample s = it.next();
            if (clientMs - s.clientMs() >= windowMs) return s;
        }
        return null;
    }

    void setHealth(int health, long now) {
        healthLog.addLast(new long[] {now, health});
        while (healthLog.size() > 2) {
            Iterator<long[]> it = healthLog.iterator();
            it.next();
            if (it.next()[0] >= now - 6000) break;
            healthLog.pollFirst();
        }
    }

    int health() {
        return (int) healthLog.peekLast()[1];
    }

    int healthAt(long t) {
        Iterator<long[]> it = healthLog.descendingIterator();
        while (it.hasNext()) {
            long[] e = it.next();
            if (e[0] <= t) return (int) e[1];
        }
        return Game.MAX_HEALTH;
    }

    int maxHealthSince(long t0) {
        int max = 0;
        Iterator<long[]> it = healthLog.descendingIterator();
        while (it.hasNext()) {
            long[] e = it.next();
            max = Math.max(max, (int) e[1]);
            if (e[0] < t0) break;
        }
        return max;
    }

    boolean addStrike(long now, int weight, long windowMs, int limit) {
        strikes.addLast(new long[] {now, weight});
        strikeTotal += weight;
        while (!strikes.isEmpty() && strikes.peekFirst()[0] < now - windowMs) {
            strikeTotal -= (int) strikes.pollFirst()[1];
        }
        return strikeTotal >= limit;
    }
}
