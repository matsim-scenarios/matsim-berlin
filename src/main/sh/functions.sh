#!/bin/bash

# check if a script started as slurm-job and change to SLURM_SUBMIT_DIR
# otherwise fail if RUN_LOCAL is not explicitly set to true!
checkIfSlurmAndChangeDirOrAbort () {
	
	: ${RUN_LOCAL:="false"} # if RUN_LOCAL is not explicitly set, set to false
	
	if [[ -n "$SLURM_SUBMIT_DIR" ]]; then
		echo "running as slurm-job. Changing to $SLURM_SUBMIT_DIR"
		cd "$SLURM_SUBMIT_DIR"
	elif [[ ${RUN_LOCAL} == "true" ]]; then
		echo "Manual started job. Stay in: $PWD"
	else
		echo "you did not start from a slurm-script. If you really want to runs this locally, set RUN_LOCAL=true"
		exit 1
	fi
}