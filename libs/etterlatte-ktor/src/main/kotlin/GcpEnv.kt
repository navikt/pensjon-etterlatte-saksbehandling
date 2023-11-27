import no.nav.etterlatte.libs.common.clusternavn

fun isProd(): Boolean = clusternavn() == GcpEnv.PROD.env

fun isDev(): Boolean = clusternavn() == GcpEnv.DEV.env

enum class GcpEnv(val env: String) {
    PROD("prod-gcp"),
    DEV("dev-gcp"),
}
