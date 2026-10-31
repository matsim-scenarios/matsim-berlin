#!/bin/bash --login
#SBATCH --job-name=berlin-elasticity-dashboard
#SBATCH --time=01:00:00
#SBATCH --partition=smp
#SBATCH --nodes=1
#SBATCH --ntasks=1
#SBATCH --cpus-per-task=1
#SBATCH --mem=4G
#SBATCH --output=elasticity-dashboard-%j.log

set -euo pipefail
cd -- "${SLURM_SUBMIT_DIR:?Submit this script with sbatch}"

# Set the newly built JAR and completed run directories here.
jar="${RUN_JAR:-matsim-berlin.jar}"
output="${RUN_DIR:-output/elasticities}"
base="${BASE_RUN:-$output/base}"
experiments=(
    # "/runs/car-cheaper"
    # "/runs/pt-cheaper"
)
# Leave experiments empty to discover run folders under output, excluding base.

module load "${JAVA_MODULE:-java/openjdk25}"
[[ -r "$jar" ]] || { echo "Cannot read JAR: $jar (set jar or RUN_JAR)" >&2; exit 2; }

args=(--base "$base" --output "$output")
if (( ${#experiments[@]} )); then args+=("${experiments[@]}"); fi

exec srun java -Xmx2G -Dmatsim.preferLocalDtds=true -cp "$jar" \
    org.matsim.analysis.PriceElasticitiesAnalysis "${args[@]}"
