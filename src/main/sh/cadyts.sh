#!/bin/bash

set -e

## find out where this script lies, get the directory, assume that functions.sh is here, source it
SCRIPT_DIR=$(dirname "$(readlink -f "$0")")
. ${SCRIPT_DIR}/functions.sh

jar="matsim-berlin-*.jar"
MEMORY="${MAKE_XMX:-60G}"
CONFIG=$1
VERSION=$2
## sample token, e.g. 25pct / 10pct / 3pct / 1pct. Must be one of the samples declared in
## RunOpenBerlinCalibration's SampleOptions, since it is passed through as the --<N>pct switch.
SAMPLE_PCT=${3:-25pct}

# check if we're on SLURM-cluster or if we really wan't to run this locally
checkIfSlurmAndChangeDirOrAbort

date
hostname
pwd

## the location of the population-file needs to be relative to the cadyts-config.xml, which is kind of weird to me
arguments="--output output/cadyts-${SAMPLE_PCT} --${SAMPLE_PCT} --iterations 50 --population ./berlin-cadyts-input-${VERSION}-${SAMPLE_PCT}.plans.xml.gz"

# Don't change anything below
################

jvm_opts="-Xmx$MEMORY -Xms$MEMORY -XX:+AlwaysPreTouch -XX:+UseParallelGC -XX:-UseGCOverheadLimit"
command="java $jvm_opts $JAVA_OPTS -cp $jar org.matsim.prepare.RunOpenBerlinCalibration --config $CONFIG $RUN_ARGS $arguments run"

## If there is a run dir, set it to the run name
#if [ -n "$RUN_DIR" ]; then
#	  command="$command --output $RUN_DIR/$RUN_NAME --runId $RUN_NAME"
#fi
#
#
# Optional parameters
#if [ "$RUN_MONITOR" == "true" ]; then
#	  command="$command -Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.port=9010 -Dcom.sun.management.jmxremote.rmi.port=9011 -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false -Djava.net.preferIPv4Stack=true -Djava.rmi.server.hostname=0.0.0.0"
#	  echo "Running in monitoring mode"
#fi
#
#if [ "$RUN_DEBUG" == "true" ]; then
#	  command="$command -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005"
#	  echo "Running in debug mode"
#fi
#
#echo ""
#
#echo ""
#module add java/21

echo "command is $command"
java -version

$command
