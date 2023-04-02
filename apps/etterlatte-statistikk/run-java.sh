#!/usr/bin/env bash

if test -d "/app/WEB-INF";
then
    set -x
    exec java \
    ${DEFAULT_JVM_OPTS} \
    ${JAVA_OPTS} \
    -server \
    -classpath \
    ${CLASSPATH} \
    ${MAIN_CLASS} \
    ${RUNTIME_OPTS} \
    $@
elif test -d "/app/BOOT-INF";
then
    unset CLASSPATH
    set -x
    exec java \
    ${DEFAULT_JVM_OPTS} \
    ${JAVA_OPTS} \
    "org.springframework.boot.loader.JarLauncher" \
    ${RUNTIME_OPTS} \
    $@
else
    set -x
    exec java \
    ${DEFAULT_JVM_OPTS} \
    ${JAVA_OPTS} \
    -jar ${APP_JAR} \
    ${RUNTIME_OPTS} \
    $@
fi
