import no.nav.etterlatte.libs.common.clusternavn

fun isProd(): Boolean = clusternavn() == GcpEnv.PROD.name

fun isDev(): Boolean = clusternavn() == GcpEnv.DEV.name

enum class GcpEnv(val env: String) {
    PROD("prod-gcp"),
    DEV("dev-gcp")
}