package no.nav.etterlatte.libs.common.behandling

enum class RevurderingAarsak(val kanBrukesDev: Boolean, val kanBrukesProd: Boolean) {
    REGULERING(true, true),
    ANSVARLIGE_FORELDRE(false, false),
    SOESKENJUSTERING(true, false),
    UTLAND(false, false),
    BARN(false, false),
    DOEDSFALL(true, false),
    VERGEMAAL_ELLER_FREMTIDSFULLMAKT(false, false);

    fun kanBrukesIMiljo(): Boolean {
        val env = System.getenv()
        val naisClusterName = env.get("NAIS_CLUSTER_NAME")
        if (naisClusterName == null) {
            return true
        } else {
            if (naisClusterName == GcpEnv.PROD.name) {
                return this.kanBrukesProd
            }
            return this.kanBrukesDev
        }
    }
}

enum class GcpEnv(val env: String) {
    PROD("prod-gcp"),
    DEV("dev-gcp")
}