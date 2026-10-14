# Berlin v7.1.1 with DSim: runtime comparison with the parallel QSim in rust

This branch is based on the tag `v7.1.1` and uses MATSim `2027.0-PR5206` (the same version as `dr/test-dsim`), which contains DSim.
It runs Berlin v7.1 for 10 iterations with DSim, set up like `berlin_iterations_v7` of the parallel QSim in rust
(repository `parallel-qsim-berlin`), so that the runtimes of mobsim, replanning and scoring can be compared without any
profiling code.

- Configs: `input/v7.1/berlin-v7.1-{1,3,10}pct.dsim.config.xml`
- Run class: `org.matsim.run.dsim.OpenBerlinDSimBenchmark`. It only adds what the rust binary does in code: persons with a pt
  leg in any plan are removed, and plans are scored by travel time only (`OnlyTravelTimeScoringFunctionFactory`).
- Phase times: `<runId>.stopwatch.csv`, or with millisecond resolution `src/main/python/dsim_phase_times.py`

## Build and run

Requires Java 25.

```shell
./mvnw clean package -DskipTests
```

DSim needs `--add-exports java.base/jdk.internal.misc=ALL-UNNAMED` when started with `-cp`.

```shell
java -Xmx20G --add-exports java.base/jdk.internal.misc=ALL-UNNAMED \
  -cp matsim-berlin-7.1-SNAPSHOT-*.jar org.matsim.run.dsim.OpenBerlinDSimBenchmark \
  --config input/v7.1/berlin-v7.1-1pct.dsim.config.xml \
  --threads 16 \
  --output output/dsim/1pct-p016 \
  --config:network.inputNetworkFile=<shared-svn>/projects/rust-qsim/berlin-v7.1/network/berlin-v7.1.network.metis.016.xml.zst \
  --config:dsim.partitioning=none
```

- `--threads N` sets `dsim.threads` (= number of partitions) and `global.numberOfThreads` (replanning), like the `num_parts`
  argument of `berlin_iterations_v7`.
- The same partitioning as in rust is used by passing the network partitioned for rust (`partition` attributes, written by
  `scripts/partition-networks.sh` in parallel-qsim-berlin) and `dsim.partitioning=none`. The number of partitions of the
  network must equal `--threads`, otherwise the run stops. Without these two options, DSim partitions the network itself with
  METIS, weighted by the routes of the agents.
- The configs read the inputs from svn via https. On the cluster, pass local copies with
  `--config:plans.inputPlansFile=...`, `--config:vehicles.vehiclesFile=...` and `--config:facilities.inputFacilitiesFile=...`.
- The configs run 10 iterations (`lastIteration=9`). `--iterations N` sets the last iteration, i.e. runs N+1 iterations.

## Settings

| | rust (`berlin_iterations_v7`, `berlin-v7.1.*pct.config.yml`) | Java (this branch) |
|---|---|---|
| Input | `calibrated-<pct>/output` plans (all plans with scores), vehicles, facilities | same files |
| Population | persons with `routingMode=pt` in any plan removed | same, in `OpenBerlinDSimBenchmark` |
| Iterations | 10 (`controller.last_iteration=9`) | `lastIteration=9` |
| Modes on the network | car, freight, truck, bike (`main_modes`) | `dsim.networkModes` |
| Teleported | ride, walk | ride, walk |
| Links | FIFO queue, storage released in the next time step, no kinematic waves | `linkDynamics=FIFO`, `trafficDynamics=queue` |
| Flow/storage capacity | `sample_size` = pct | `flowCapacityFactor`, `storageCapacityFactor` = pct |
| Stuck time, end time | 30 s, 36:00 | 30 s, 36:00 |
| Partitioning | METIS (InLinkCount, Capacity, 1.1), pre-partitioned networks | same networks with `dsim.partitioning=none` |
| Threads | `num_parts` partitions, replanning and scoring threads | `dsim.threads`, `global.numberOfThreads` |
| Replanning `person` | ReRoute 0.2, SelectExpBeta 0.8 (beta 1) | ReRoute 0.2, SelectExpBeta 0.8 (`brainExpBeta=1`) |
| Replanning other subpopulations | no strategy, selected plan is kept | KeepLastSelected 1.0 |
| Plan memory | 5, remove worst score | 5, `WorstPlanSelector` |
| Innovation | never switched off (fraction 1.0), no annealing | `fractionOfIterationsToDisableInnovation=1.0`, no annealing |
| Score update | new score replaces old score | no `fractionOfIterationsToStartScoreMSA`, learning rate 1 |
| Scoring | `OnlyTravelTimeDependentScoring` | `OnlyTravelTimeScoringFunctionFactory` |
| Router | A* with ALT, travel time from the mobsim, no randomness | SpeedyALT, travel time from the mobsim, `routingRandomness=0` |
| Output during iterations | no events | no events, plans, trips, link stats, leg histograms |

## Remaining differences

- DSim scores the backpacks in one thread in the scoring phase, rust in `num_parts` threads. This is part of what is compared.
- The router uses the Berlin travel times and disutilities (e.g. bike with `BicycleTravelTime`, mode parameters of the calibrated
  config), rust its defaults. This changes routes, not the kind of work.
- The iteration ends listeners of MATSim (mode stats, score stats, travel distance stats, mode choice coverage) still run. They
  are not part of the compared phases.
- rust writes the experienced plans inside the scoring phase in iteration 0 and in the last iteration
  (`scoring.write_experienced_plans` is true by default). Set `--set scoring.write_experienced_plans=false` for the rust runs or
  leave these iterations out.
- Java needs to warm up (JIT) in the first iteration.

## Phases

Java runs replanning at the beginning of iterations 1 to 9, rust at the end of iterations 0 to 8. Both do 10 mobsims,
10 scorings and 9 replannings.

| rust (instrument spans) | Java stopwatch | `dsim_phase_times.py` |
|---|---|---|
| `run_mobsim_phase` (incl. `prepare_for_mobsim`) | `prepareForMobsim` + `mobsim` | `mobsim_ms` |
| `run_scoring_phase` | `scoring` | `scoring_ms` |
| `run_replanning_phase` after iteration i | `replanning` in iteration i+1 | `replanning_ms` in iteration i+1 |

The stopwatch only has a resolution of one second, which is too coarse for scoring and for small runs. The logfile has a
millisecond timestamp at every phase boundary:

```shell
python3 src/main/python/dsim_phase_times.py output/dsim/1pct-p016/berlin-v7.1-1pct.logfile.log phases.csv
```

DSim additionally writes `runtimes-0/runtime-0.csv` (simulation loop of the last iteration only, in ms).
