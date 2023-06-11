#!/bin/bash --login
#$ -l h_rt=600000
#$ -j y
#$ -m a
#$ -o ./logfile/logfile_$JOB_NAME.log
#$ -cwd
#$ -pe mp 4
#$ -l mem_free=4G
#$ -N sumo-berlin

date
hostname

source env/bin/activate

ENV="/net/ils/matsim-berlin/capacity/env"

export LD_LIBRARY_PATH="$ENV/lib64:$ENV/lib:$LD_LIBRARY_PATH"
export SUMO_HOME="$ENV/bin"

# use with -t 1-10
idx=$((SGE_TASK_ID - 1))
total=$SGE_TASK_LAST

scenario="base"

f="output-junctions/scenario-$scenario"

command="python -u junction_volumes.py intersections.txt --scenario $scenario --network sumo.net.xml --output $f --runner runner/${JOB_ID}${SGE_TASK_ID} --runner-index $idx --runner-total $total"

#command="python -u edge_volumes.py sample.csv --scenario $scenario--network sumo.net.xml --output $f --runner runner${JOB_ID}${SGE_TASK_ID} --runner-index $idx --runner-total $total"

echo ""
echo "command is $command"
echo ""

$command
python results.py --output $f --result $f.csv


