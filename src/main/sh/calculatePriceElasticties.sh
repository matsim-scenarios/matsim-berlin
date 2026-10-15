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
cd -- "${SLURM_SUBMIT_DIR:?Submit with sbatch src/main/sh/calculatePriceElasticties.sh}"

# Set these to the freshly built shaded JAR and your scenario config on the cluster.
jar="${RUN_JAR:-matsim-berlin.jar}"
config="${RUN_CONFIG:-input/v7.2-pre-1/berlin-v7.2-pre-1-1pct.config.xml}"
factors=(0.9 1.1)
# Array IDs must run from 0 to twice the number of factors: 2 factors -> 0-4; 3 -> 0-6.

if [[ ! -r "$jar" ]]; then
    echo "Cannot read shaded JAR: $jar. Set jar or RUN_JAR before submitting." >&2
    exit 2
fi
# Load Java 25+ before submission, or specify your cluster's module name.
if [[ -n "${JAVA_MODULE:-}" ]]; then module load "$JAVA_MODULE"; fi
java_bin="${JAVA_HOME:+$JAVA_HOME/bin/}java"

if [[ "${1:-}" == --report ]]; then
    exec srun "$java_bin" -Xmx2G -Dmatsim.preferLocalDtds=true -cp "$jar" org.matsim.run.RunOpenBerlinElasticities run \
        --report --output "${RUN_DIR:-output/elasticities}"
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

# Queue exactly one report after every simulation in this array has succeeded.
if (( task == 0 )); then
    sbatch --export=ALL --array=0 --dependency="afterok:${SLURM_ARRAY_JOB_ID:?Missing array job ID}" \
        --kill-on-invalid-dep=yes --job-name=berlin-price-elasticities \
        --cpus-per-task=1 --mem=4G --time=01:00:00 "${BASH_SOURCE[0]}" --report
fi

exec srun "$java_bin" "-Xmx${RUN_MEMORY:-46G}" \
    -cp "$jar" org.matsim.run.RunOpenBerlinElasticities run --config "$config" \
    --car-monetary-distance-rate-factor "$car_factor" \
    --pt-daily-monetary-constant-factor "$pt_factor" \
    --output "${RUN_DIR:-output/elasticities}"
