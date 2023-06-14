fun isProd(): Boolean = getNaisclustername() == GcpEnv.PROD.name

fun isDev(): Boolean = getNaisclustername() == GcpEnv.DEV.name

fun getNaisclustername(): String? = System.getenv()["NAIS_CLUSTER_NAME"]

enum class GcpEnv(val env: String) {
    PROD("prod-gcp"),
    DEV("dev-gcp")
}