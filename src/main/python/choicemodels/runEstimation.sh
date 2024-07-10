#!/bin/bash --login
#SBATCH --time=200:00:00
#SBATCH --output=logfile_%x-%j.log
#SBATCH --partition=smp
#SBATCH --nodes=1
#SBATCH --ntasks=1
#SBATCH --cpus-per-task=16
#SBATCH --mem=32G
#SBATCH --job-name=berlin-estimation
#SBATCH --mail-type=END,FAIL

date
hostname

source env/bin/activate

python -u estimate_biogeme_plan_choice.py --input "plan-choices-bestK_9-tt-only.csv"
