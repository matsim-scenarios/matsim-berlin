#!/usr/bin/env bash
#$ -l h_rt=790000
#$ -j y
#$ -m a
#$ -cwd
#$ -pe mp 8
#$ -l mem_free=6G
#$ -N calib-scenario

date
hostname

command="python -u calibrate.py"

echo ""
echo "command is $command"
echo ""

source env/bin/activate

module add java/17
java -version

$command
