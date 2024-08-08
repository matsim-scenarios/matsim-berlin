#!/bin/bash --login
#SBATCH --time=200:00:00
#SBATCH --output=logfile_%x-%j.log
#SBATCH --partition=smp
#SBATCH --nodes=1
#SBATCH --ntasks=1
#SBATCH --cpus-per-task=16
#SBATCH --mem=32G
#SBATCH --job-name=calib-berlin

date
hostname

command="python -u calibrate_dist.py"

echo ""
echo "command is $command"
echo ""

source env/bin/activate

module add java/21
java -version

$command
