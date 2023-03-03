#!/usr/bin/env bash
#$ -l h_rt=790000
#$ -j y
#$ -m a
#$ -o ./logfile/logfile_$JOB_NAME.log
#$ -cwd
#$ -pe mp 8
#$ -l mem_free=6G
#$ -N run-berlin

date
hostname

jar="matsim-berlin-6.x-SNAPSHOT.jar"
memory="${RUN_MEMORY:-46G}"
config="${RUN_CONFIG:-config.xml}"

arguments="<put custom args here>"

# Don't change anything below
################

jvm_opts="-Xmx$memory -Xms$memory -XX:+AlwaysPreTouch -XX:+UseParallelGC"
command="java $jvm_opts $JAVA_OPTS -jar $jar --config $config $RUN_ARGS $arguments run"

# If there is a run dir, set it to the run name
if [ -n "$RUN_DIR" ]; then
      command="$command --output $RUN_DIR/$RUN_NAME --runId $RUN_NAME"
fi


# Optional parameters
if [ "$RUN_MONITOR" == "true" ]; then
      command="$command -Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.port=9010 -Dcom.sun.management.jmxremote.rmi.port=9011 -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false -Djava.net.preferIPv4Stack=true -Djava.rmi.server.hostname=0.0.0.0"
      echo "Running in monitoring mode"
fi

if [ "$RUN_DEBUG" == "true" ]; then
      command="$command -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005"
      echo "Running in debug mode"
fi

echo ""
echo "command is $command"

echo ""
module add java/17
java -version

$command
