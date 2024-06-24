#!/bin/bash --login
#SBATCH --time=100:00:00
#SBATCH --output=logfile_%x-%j.log
#SBATCH --partition=smp
#SBATCH --nodes=1
#SBATCH --ntasks=1
#SBATCH --cpus-per-task=16
#SBATCH --mem=32G
#SBATCH --job-name=eval6.3-berlin
#SBATCH --constraint=intel
#SBATCH --mail-type=END,FAIL

date
hostname

echo "Task Id: $SLURM_ARRAY_TASK_ID"
echo "Task Count: $SLURM_ARRAY_TASK_COUNT"

JAR=matsim-berlin-6.3-SNAPSHOT-v6.0-158-gcdbb04f.jar
CONFIG="../input/v6.3/berlin-v6.3.config.xml"
ARGS="--3pct --iterations 200 --yaml params/run10.yaml --config:plans.inputPlansFile=/net/ils/matsim-berlin/calibration-v6.3/mode-choice-3pct-default-v1/runs/008/008.output_plans.xml.gz"
JVM_ARGS="-Xmx30G -Xms30G -XX:+AlwaysPreTouch -XX:+UseParallelGC"
RUNS=12

# Start job as array with --array=0-5
idx=$SLURM_ARRAY_TASK_ID
total=$SLURM_ARRAY_TASK_COUNT

source env/bin/activate

module add java/21
java -version

# Runs simulation multiple times (simultaneously) with different random seeds

python -u -m matsim.calibration run-simulations\
 --jar $JAR --config $CONFIG --args "$ARGS" --jvm-args "$JVM_ARGS" --runs $RUNS\
 --worker-id $idx --workers $total
