#!/usr/bin/env sh

if [ -r "/opt/appdynamics/javaagent.jar" ] &&
    [ "${APPD_ENABLED}" = "true" ] &&
    ([ -n "${APP_NAME}" ] || [ -n "${APPD_NAME}" ] || [ -n "${NAIS_APP_NAME}" ])
then
    APPD_NAME=${APPD_NAME:-$APP_NAME}
    APPD_NAME=${APPD_NAME:-$NAIS_APP_NAME}
    APPD_HOSTNAME="${APPD_HOSTNAME:-$HOSTNAME}"
    APPD_TIER="${APPD_TIER:-$APPD_NAME}"

    JAVA_OPTS="${JAVA_OPTS} -javaagent:/opt/appdynamics/javaagent.jar"
    JAVA_OPTS="${JAVA_OPTS} -Dappdynamics.agent.applicationName=${APPD_NAME}"
    JAVA_OPTS="${JAVA_OPTS} -Dappdynamics.agent.nodeName=${APPD_HOSTNAME}"
    JAVA_OPTS="${JAVA_OPTS} -Dappdynamics.agent.tierName=${APPD_TIER}"
    JAVA_OPTS="${JAVA_OPTS} -Dappdynamics.agent.reuse.nodeName=true"
    JAVA_OPTS="${JAVA_OPTS} -Dappdynamics.agent.reuse.nodeName.prefix=${APPD_NAME}"
    JAVA_OPTS="${JAVA_OPTS} -Dappdynamics.jvm.shutdown.mark.node.as.historical=true"
    export JAVA_OPTS
fi
