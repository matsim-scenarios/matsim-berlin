#!/usr/bin/env bash

# Check which runs did not complete

for d in output/*; do

	if [ ! -f $d/*.output_trips.csv.gz ]; then
		echo "Run $d is not finished"
		#rm -r $d
	fi

done
