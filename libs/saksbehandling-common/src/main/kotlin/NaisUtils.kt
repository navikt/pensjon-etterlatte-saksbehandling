package no.nav.etterlatte.libs.common

fun appName(): String? = System.getenv()["NAIS_APP_NAME"]

fun clusterNavn(): String? = System.getenv()["NAIS_CLUSTER_NAME"]

enum class GcpEnv(
    val env: String,
) {
    PROD("prod-gcp"),
    DEV("dev-gcp"),
}

fun isDev(): Boolean = clusterNavn() == GcpEnv.DEV.env

fun isProd(): Boolean = clusterNavn() == GcpEnv.PROD.env

fun appIsInGCP(): Boolean =
    when (val naisClusterName = clusterNavn()) {
        null -> false
        else -> GcpEnv.entries.map { it.env }.contains(naisClusterName)
    }
