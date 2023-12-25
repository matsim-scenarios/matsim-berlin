#!/bin/bash --login
#$ -l h_rt=790000
#$ -j y
#$ -m a
#$ -cwd
#$ -pe mp 16
#$ -l mem_free=4G
#$ -N calib-berlin

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
