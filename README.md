# fairplay-sim

A small simulator for server-side cheat detection. A toy authoritative game
server receives position/health/ammo packets from in-process clients, runs a
list of validation checks on them, and flags a player once enough violations
pile up. Some clients cheat, others are legit and sit behind a lossy, jittery
network. Everything is seeded, offline, and runs in a few seconds. It does not
touch any real game or anti-cheat.

## Build

Needs JDK 21 and nothing else (no Maven/Gradle).

    sh build.sh      # Linux/macOS
    build.cmd        # Windows

This compiles, runs the tests in `src/test` (a small hand-rolled runner, no
JUnit) and builds `out/fairplay-sim.jar`.

## Usage

    java -jar out/fairplay-sim.jar [--seed N] [--players N] [--duration SECONDS]
                                   [--net lan|typical|poor|bad] [--html FILE]
                                   [--param key=value ...]

Each cheat scenario runs `--players` cheaters next to `--players` legit
players; cheating starts at 10 s. The legit-only runs go through all four
network profiles. Output is a console table plus `fairplay-report.html`
(self-contained, pass `--html ""` to skip it).

Thresholds are plain parameters, e.g. `--param speed.tolerance=1.05
--param health.graceMs=800`. The HTML report lists every parameter that was
in effect. Checks live one per file (`SpeedCheck`, `TeleportCheck`,
`FireRateCheck`, `AmmoCheck`, `HealthCheck`, `SequenceCheck`); a new one extends
`Check` and gets added to `Server.defaultChecks`.

Scenarios: speed (2x), speed_subtle (1.25x), teleport, health_hack, ammo_hack,
rapid_fire, replay, out_of_order.

## Results

Default run (`--seed 1`, 50 + 50 players, typical network: 50 ms, 10 ms
jitter, 1% loss):

    scenario       cheaters  detected      rate   ttd p50   ttd p95 first trigger
    speed                50        50    100.0%     0.65s     3.51s speed 100%
    speed_subtle         50         6     12.0%    10.75s    19.96s speed 100%
    teleport             50        50    100.0%     3.05s     3.07s teleport 100%
    health_hack          50        50    100.0%     0.09s     0.77s health 100%
    ammo_hack            50        50    100.0%     0.47s     4.06s ammo 100%
    rapid_fire           50        50    100.0%     1.56s     8.10s fire 100%
    replay               50        50    100.0%     1.94s     5.34s seq 100%
    out_of_order         50        50    100.0%     1.02s     1.03s seq 94%, speed 6%

    legit/lan            50         0      0.0%         0.00
    legit/typical        50         0      0.0%         0.00
    legit/poor           50         0      0.0%         0.00
    legit/bad            50         0      0.0%         1.72   (violations/min)

Time to detect counts from the moment cheating starts, so the teleport figure
is mostly the 3 s gap between jumps. No legit player was flagged on any
profile, but on the `bad` network (180 ms, 60 ms jitter, 10% loss) legit
players still trip about 1.7 violations a minute; they just don't pile up fast
enough to cross the strike limit. The 25% speed cheat is mostly missed by
design: it only trips when the cheater's random pace plus 25% exceeds the
limit.

## Known gaps

- Cheaters use honest client timestamps. A client that lies about its clock
  gets some help from the arrival-time cap but this is not modelled.
- One seed, one run. The numbers above are not confidence intervals; try other
  seeds before trusting a threshold.
- The legit client is a simple random walker with scripted bursts of fire.
  Real player movement (strafing, sprint-stop, vehicles) would produce more
  false positives.
- Health is only checked against upward claims, and the grace period is a
  fixed number, so the `bad` profile is close to its limit.
- A flagged player is just recorded; there is no kick, rollback, or
  rubber-banding, so speed and teleport cheats keep accumulating violations.
- Dropped and delayed packets are the only network faults; no duplication by
  the network itself.
