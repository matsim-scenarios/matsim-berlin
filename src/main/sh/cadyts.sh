#!/bin/bash

set -e

jar="matsim-berlin-*.jar"
MEMORY="${MAKE_XMX:-60G}"
CONFIG=$1
VERSION=$2

if [ -n "$SLURM_SUBMIT_DIR" ]; then
	echo "running as slurm-job. Changing to $SLURM_SUBMIT_DIR"
	cd "$SLURM_SUBMIT_DIR"
else
	echo "Manual started job. Stay in: $PWD"
fi

date
hostname
pwd

## the location of the population-file needs to be relative to the cadyts-config.xml, which is kind of weird to me
arguments="--all-car --weight 0.15 --output output/v7.0/cadyts --mode cadyts --scale-factor 1.0 --25pct --iterations 50 --population ./berlin-cadyts-input-${VERSION}-25pct.plans.xml.gz"

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
#echo "command is $command"
#
#echo ""
#module add java/21
java -version

$command
