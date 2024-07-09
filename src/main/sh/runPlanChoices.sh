#!/usr/bin/env bash

CONFIG="./input/v6.3/berlin-v6.3.config.xml"
JVM_ARGS="-Xmx22G -Xms22G -XX:+AlwaysPreTouch -XX:+UseParallelGC"

run_eval() {
    echo "Running evaluation with $1"
    java $JVM_ARGS -cp matsim-berlin-*.jar org.matsim.prepare.choices.ComputePlanChoices --config $CONFIG\
      --scenario org.matsim.run.OpenBerlinScenario\
      --args --10pct\
      --modes walk,pt,car,bike,ride\
      $1
}


run_eval "--plan-candidates bestK --top-k 3"
run_eval "--plan-candidates bestK --top-k 5"
run_eval "--plan-candidates bestK --top-k 9"
run_eval "--plan-candidates bestK --top-k 3 --time-util-only"
run_eval "--plan-candidates bestK --top-k 5 --time-util-only"
run_eval "--plan-candidates bestK --top-k 9 --time-util-only"
run_eval "--plan-candidates random --top-k 3"
run_eval "--plan-candidates random --top-k 5"
run_eval "--plan-candidates random --top-k 9"
run_eval "--plan-candidates diverse --top-k 9"
