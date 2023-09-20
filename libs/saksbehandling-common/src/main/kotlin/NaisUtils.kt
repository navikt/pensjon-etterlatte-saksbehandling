package no.nav.etterlatte.libs.common

fun clusternavn(): String? = System.getenv()["NAIS_CLUSTER_NAME"]
