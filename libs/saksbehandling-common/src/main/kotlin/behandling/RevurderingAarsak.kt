package no.nav.etterlatte.libs.common.behandling

private val SAKTYPE_OMS = listOf(SakType.OMSTILLINGSSTOENAD)
private val SAKTYPE_BP = listOf(SakType.BARNEPENSJON)
private val SAKTYPE_BEGGE = SAKTYPE_BP + SAKTYPE_OMS

enum class RevurderingAarsak(
    val gyldigForSakTyper: List<SakType>,
    val kanBrukesDev: Boolean,
    val kanBrukesProd: Boolean
) {
    ANSVARLIGE_FORELDRE(SAKTYPE_BP, false, false),
    SOESKENJUSTERING(SAKTYPE_BP, true, false),
    UTLAND(SAKTYPE_BP, false, false),
    BARN(SAKTYPE_BP, false, false),
    VERGEMAAL_ELLER_FREMTIDSFULLMAKT(SAKTYPE_BP, false, false),
    REGULERING(SAKTYPE_BEGGE, true, true),
    DOEDSFALL(SAKTYPE_BEGGE, true, false);

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

    fun gyldigForSakType(sakType: SakType): Boolean = gyldigForSakTyper.any { it == sakType }
}

enum class GcpEnv(val env: String) {
    PROD("prod-gcp"),
    DEV("dev-gcp")
}