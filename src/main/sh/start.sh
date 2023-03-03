#!/usr/bin/env bash

# Helper script to start one or multiple jobs and pass arguments to it

name=$( echo "$*" | sed -e 's/ //g' -e 's/--//g')

export RUN_ARGS="$*"
export RUN_NAME="$name"

echo "Starting run kh-$name"
echo "$*"

qsub -V -N matsim-"$name" job.sh
