package no.nav.etterlatte.libs.common.behandling

import no.nav.etterlatte.libs.common.clusternavn

private val SAKTYPE_OMS = listOf(SakType.OMSTILLINGSSTOENAD)
private val SAKTYPE_BP = listOf(SakType.BARNEPENSJON)
private val SAKTYPE_BP_OMS = SAKTYPE_BP + SAKTYPE_OMS

enum class RevurderingAarsak(
    private val gyldigFor: List<SakType>,
    private val kanBrukesDev: Boolean,
    private val kanBrukesProd: Boolean
) {
    ANSVARLIGE_FORELDRE(SAKTYPE_BP, false, false),
    SOESKENJUSTERING(SAKTYPE_BP, true, false),
    UTLAND(SAKTYPE_BP, false, false),
    BARN(SAKTYPE_BP, false, false),
    VERGEMAAL_ELLER_FREMTIDSFULLMAKT(SAKTYPE_BP, false, false),
    REGULERING(SAKTYPE_BP_OMS, true, true),
    DOEDSFALL(SAKTYPE_BP_OMS, true, false),
    INNTEKTSENDRING(SAKTYPE_OMS, true, false);

    fun kanBrukesIMiljo(): Boolean = when (clusternavn()) {
        null -> true
        GcpEnv.PROD.name -> this.kanBrukesProd
        GcpEnv.DEV.name -> this.kanBrukesDev
        else -> this.kanBrukesDev
    }

    fun gyldigForSakType(sakType: SakType): Boolean = gyldigFor.any { it == sakType }
}

enum class GcpEnv(val env: String) {
    PROD("prod-gcp"),
    DEV("dev-gcp")
}