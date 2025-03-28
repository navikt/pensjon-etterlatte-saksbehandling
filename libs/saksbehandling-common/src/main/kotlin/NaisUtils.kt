package no.nav.etterlatte.libs.common

import no.nav.etterlatte.libs.common.NaisKey.NAIS_APP_NAME
import no.nav.etterlatte.libs.common.NaisKey.NAIS_CLUSTER_NAME

fun appName(): String? = Miljoevariabler.systemEnv()[NAIS_APP_NAME]

fun clusterNavn(): String? = Miljoevariabler.systemEnv()[NAIS_CLUSTER_NAME]

enum class GcpEnv(
    val env: String,
) {
    PROD("prod-gcp"),
    DEV("dev-gcp"),
}

enum class TestKey : EnvEnum {
    TEST_RUNNER,
    ;

    override fun key() = name
}

fun isTestRunner(): Boolean = Miljoevariabler.systemEnv()[TestKey.TEST_RUNNER] == "true"

fun isDev(): Boolean = clusterNavn() == GcpEnv.DEV.env

fun isProd(): Boolean = clusterNavn() == GcpEnv.PROD.env

fun appIsInGCP(): Boolean =
    when (val naisClusterName = clusterNavn()) {
        null -> false
        else -> GcpEnv.entries.map { it.env }.contains(naisClusterName)
    }

enum class NaisKey : EnvEnum {
    NAIS_APP_NAME,
    NAIS_CLUSTER_NAME,
    NAIS_APP_IMAGE,
    ;

    override fun key() = name
}
