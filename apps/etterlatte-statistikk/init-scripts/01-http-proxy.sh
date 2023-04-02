#!/usr/bin/env sh

# inject proxy settings set by the nais platform
export JAVA_OPTS="${JAVA_OPTS} ${JAVA_PROXY_OPTIONS}"
