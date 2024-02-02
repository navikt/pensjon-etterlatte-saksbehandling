package no.nav.etterlatte.libs.common

fun clusternavn(): String? = System.getenv()["NAIS_CLUSTER_NAME"]

enum class GcpEnv(val env: String) {
    PROD("prod-gcp"),
    DEV("dev-gcp"),
}

fun isDev(): Boolean = clusternavn() == GcpEnv.DEV.env

fun isProd(): Boolean = clusternavn() == GcpEnv.PROD.env

fun appIsInGCP(): Boolean {
    return when (val naisClusterName = clusternavn()) {
        null -> false
        else -> GcpEnv.entries.map { it.env }.contains(naisClusterName)
    }
}
