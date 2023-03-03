#!/usr/bin/env bash

# Iterate options to generate all runs
for it in "--iterations 100"  "--iterations 1000"; do
for w in "0.05" "0.1" "0.2"; do

    # Simple example, please adjust
    ./start.sh --mc none --plans --weight "$w" "$it"

done
done