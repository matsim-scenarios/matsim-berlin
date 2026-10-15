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
# Array IDs must run from 0 to twice the number of factors: 2 factors -> 0-4; 3 -> 0-6.

if [[ ! -r "$jar" ]]; then
    echo "Cannot read shaded JAR: $jar. Set jar or RUN_JAR before submitting." >&2
    exit 2
fi
# Load Java 25+ before submission, or specify your cluster's module name.
if [[ -n "${JAVA_MODULE:-}" ]]; then module load "$JAVA_MODULE"; fi
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
    exec srun "$java_bin" -Xmx2G -Dmatsim.preferLocalDtds=true -cp "$jar" org.matsim.run.RunOpenBerlinElasticities run \
        --report --output "$output"
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
pt_factor=1.0
if (( task > 0 )); then
    factor="${factors[$(( (task - 1) / 2 ))]}"
    if (( task % 2 == 1 )); then car_factor="$factor"; else pt_factor="$factor"; fi
fi

# Submit the report directly: Slurm's spool copy is not a shared script path.
if (( task == 0 )); then
    report_args=(--export=ALL --array=0 --dependency="afterok:${SLURM_ARRAY_JOB_ID:?Missing array job ID}"
        --kill-on-invalid-dep=yes --job-name=berlin-price-elasticities
        --partition="${SLURM_JOB_PARTITION:-smp}" --nodes=1 --ntasks=1
        --cpus-per-task=1 --mem=4G --time=01:00:00 --output=elasticities-%A_%a.log)
    if [[ -n "${SLURM_JOB_ACCOUNT:-}" ]]; then report_args+=(--account="$SLURM_JOB_ACCOUNT"); fi
    printf -v report_command '%q ' srun "$java_bin" -Xmx2G -Dmatsim.preferLocalDtds=true \
        -cp "$jar" org.matsim.run.RunOpenBerlinElasticities run --report --output "$output"
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

echo "Starting array task $task: car factor=$car_factor, PT factor=$pt_factor"
exec srun "$java_bin" "-Xmx${RUN_MEMORY:-46G}" -Dmatsim.preferLocalDtds=true \
    -cp "$jar" org.matsim.run.RunOpenBerlinElasticities run --config "$config" \
    --car-monetary-distance-rate-factor "$car_factor" \
    --pt-daily-monetary-constant-factor "$pt_factor" \
    --output "$output"
