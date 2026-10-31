#!/bin/bash --login
#SBATCH --job-name=berlin-elasticities
#SBATCH --array=0-4
#SBATCH --time=200:00:00
#SBATCH --partition=smp
#SBATCH --nodes=1
#SBATCH --ntasks=1
#SBATCH --cpus-per-task=16
#SBATCH --mem=48G
#SBATCH --output=elasticities-%A_%a.log

set -euo pipefail
cd -- "${SLURM_SUBMIT_DIR:?Submit this script with sbatch}"

# Set these to the freshly built shaded JAR and your scenario config on the cluster.
jar="${RUN_JAR:-matsim-berlin.jar}"
config="${RUN_CONFIG:-input/v7.2-pre-1/berlin-v7.2-pre-1-1pct.config.xml}"
output="${RUN_DIR:-output/elasticities}"
factors=(0.9 1.1)
# true: scale car distance AND daily costs by each factor; false: distance costs only.
car_daily_costs="${CAR_DAILY_COSTS:-false}"
# Array IDs must run from 0 to twice the number of factors: 2 factors -> 0-4; 3 -> 0-6.

if [[ ! -r "$jar" ]]; then
    echo "Cannot read shaded JAR: $jar. Set jar or RUN_JAR before submitting." >&2
    exit 2
fi
# Load the cluster's Java 25 module; JAVA_MODULE can select another module.
module load "${JAVA_MODULE:-java/openjdk25}"
# Use the module's executable, even when an inherited JAVA_HOME still points at Java 8.
java_bin="$(command -v java)"
java_version="$("$java_bin" -XshowSettings:properties -version 2>&1 | awk '$1 == "java.specification.version" {print $3}')"
java_major="${java_version#1.}"
java_major="${java_major%%.*}"
if [[ ! "$java_major" =~ ^[0-9]+$ ]] || (( java_major < 25 )); then
    echo "Java 25+ is required; found ${java_version:-unknown} at $java_bin. Load Java 25 or set JAVA_MODULE." >&2
    exit 2
fi
echo "Using Java $java_version: $java_bin"

if [[ "${1:-}" == --report ]]; then
    exec srun "$java_bin" -Xmx2G -Dmatsim.preferLocalDtds=true -cp "$jar" org.matsim.analysis.PriceElasticitiesAnalysis \
        --output "$output"
fi

if [[ ! -r "$config" ]]; then
    echo "Cannot read config: $config (working directory: $PWD). Set RUN_CONFIG to its path on the cluster." >&2
    exit 2
fi

task="${SLURM_ARRAY_TASK_ID:?Submit as a Slurm array}"
if (( task < 0 || task > 2 * ${#factors[@]} )); then
    echo "Array task $task has no matching factor. Check --array and factors." >&2
    exit 2
fi

car_factor=1.0
car_daily_factor=1.0
pt_factor=1.0
case "$car_daily_costs" in
    true|false) ;;
    *) echo "CAR_DAILY_COSTS must be true or false." >&2; exit 2;;
esac
if (( task > 0 )); then
    factor="${factors[$(( (task - 1) / 2 ))]}"
    if (( task % 2 == 1 )); then car_factor="$factor"; else pt_factor="$factor"; fi
fi
if [[ "$car_daily_costs" == true ]]; then car_daily_factor="$car_factor"; fi

# Read the original costs; standard MATSim overrides are applied after Berlin's config setup.
overrides="$(python3 - "$config" "$car_factor" "$car_daily_factor" "$pt_factor" <<'PY'
import math
import sys
import xml.etree.ElementTree as ET

distance, daily, pt = map(float, sys.argv[2:])
if any(not math.isfinite(f) or f < 0 for f in (distance, daily, pt)):
    raise SystemExit("Cost factors must be finite and non-negative.")
scoring = ET.parse(sys.argv[1]).findall("./module[@name='scoring']/parameterset[@type='scoringParameters']")
if not scoring:
    raise SystemExit("Config needs scoring/scoringParameters.")
for group in scoring:
    subpop = group.find("./param[@name='subpopulation']")
    subpop = subpop.get("value") if subpop is not None else "null"
    for mode in group.findall("./parameterset[@type='modeParams']"):
        name = mode.find("./param[@name='mode']").get("value")
        costs = {"car": (("monetaryDistanceRate", distance), ("dailyMonetaryConstant", daily)),
                 "pt": (("dailyMonetaryConstant", pt),)}.get(name, ())
        for parameter, factor in costs:
            value = mode.find("./param[@name='%s']" % parameter)
            if factor == 1 or value is None:
                continue
            value = float(value.get("value")) * factor
            if not math.isfinite(value):
                raise SystemExit("Non-finite monetary parameter: " + parameter)
            print("--config:scoring.scoringParameters[subpopulation=%s].modeParams[mode=%s].%s" % (subpop, name, parameter))
            print(format(value, ".17g"))
PY
)"
name=base
if (( task > 0 )); then
    name="car-${car_factor}"
    if [[ "$car_daily_factor" != 1.0 ]]; then name+="_carDaily-${car_daily_factor}"; fi
    name+="_pt-${pt_factor}"
fi
args=(run --config "$config" --output "$output/$name" --runId "${name//./p}")
if [[ -n "$overrides" ]]; then
    while IFS= read -r option; do args+=("$option"); done <<< "$overrides"
fi

# Submit the report directly: Slurm's spool copy is not a shared script path.
if (( task == 0 )); then
    report_args=(--export=ALL --array=0 --dependency="afterok:${SLURM_ARRAY_JOB_ID:?Missing array job ID}"
        --kill-on-invalid-dep=yes --job-name=berlin-price-elasticities
        --partition="${SLURM_JOB_PARTITION:-smp}" --nodes=1 --ntasks=1
        --cpus-per-task=1 --mem=4G --time=01:00:00 --output=elasticities-%A_%a.log)
    if [[ -n "${SLURM_JOB_ACCOUNT:-}" ]]; then report_args+=(--account="$SLURM_JOB_ACCOUNT"); fi
    printf -v report_command '%q ' srun "$java_bin" -Xmx2G -Dmatsim.preferLocalDtds=true \
        -cp "$jar" org.matsim.analysis.PriceElasticitiesAnalysis --output "$output"
    if ! sbatch "${report_args[@]}" <<EOF
#!/bin/bash --login
set -euo pipefail
cd -- $(printf '%q' "$SLURM_SUBMIT_DIR")
exec $report_command
EOF
    then
        echo "Could not queue the dashboard report; continuing the base simulation. Submit this script with --report after all runs finish." >&2
    fi
fi

echo "Starting array task $task: car distance=$car_factor, car daily=$car_daily_factor, PT daily=$pt_factor"
exec srun "$java_bin" "-Xmx${RUN_MEMORY:-46G}" -Dmatsim.preferLocalDtds=true \
    -cp "$jar" org.matsim.run.OpenBerlinScenarioWrapper "${args[@]}"
