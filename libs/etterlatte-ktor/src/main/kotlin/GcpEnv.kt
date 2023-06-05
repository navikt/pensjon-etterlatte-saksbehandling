

fun isProd(): Boolean {
    val currentEnv = getNaisclustername()
    return if (currentEnv == null) {
        false
    } else {
        currentEnv == GcpEnv.PROD.name
    }
}

fun isDev(): Boolean {
    val currentEnv = getNaisclustername()
    return if (currentEnv == null) {
        false
    } else {
        currentEnv == GcpEnv.DEV.name
    }
}

fun getNaisclustername(): String? {
    val env = System.getenv()
    return env["NAIS_CLUSTER_NAME"]
}

enum class GcpEnv(val env: String) {
    PROD("prod-gcp"),
    DEV("dev-gcp")
}