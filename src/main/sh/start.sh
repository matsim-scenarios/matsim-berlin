#!/usr/bin/env bash

# Helper script to start one or multiple jobs and pass arguments to it

if [ -z "${run_name}" ]; then
    # Unset or emtpy
    name=$( echo "$*" | sed -e 's/ //g' -e 's/:/_/g' -e 's/--//g' -e 's/\./_/g' -e 's/\///g')
else
    name="${run_name}"
fi


export RUN_ARGS="$*"
export RUN_NAME="$name"

echo "Starting run $name"
echo "$*"

sbatch --parsable --export=ALL --job-name matsim-"$name" job.sh
