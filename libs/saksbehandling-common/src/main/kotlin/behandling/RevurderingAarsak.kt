package no.nav.etterlatte.libs.common.behandling

import no.nav.etterlatte.libs.common.clusternavn

private val SAKTYPE_OMS = listOf(SakType.OMSTILLINGSSTOENAD)
private val SAKTYPE_BP = listOf(SakType.BARNEPENSJON)
private val SAKTYPE_BP_OMS = SAKTYPE_BP + SAKTYPE_OMS

enum class RevurderingAarsak(
    private val gyldigFor: List<SakType>,
    private val kanBrukesDev: Boolean,
    private val kanBrukesProd: Boolean,
    val girOpphoer: Boolean
) {
    ANSVARLIGE_FORELDRE(SAKTYPE_BP, false, false, false),
    SOESKENJUSTERING(SAKTYPE_BP, true, false, false),
    UTLAND(SAKTYPE_BP, false, false, false),
    BARN(SAKTYPE_BP, false, false, false),
    VERGEMAAL_ELLER_FREMTIDSFULLMAKT(SAKTYPE_BP, false, false, false),
    REGULERING(SAKTYPE_BP_OMS, true, true, false),
    DOEDSFALL(SAKTYPE_BP_OMS, true, false, true),
    INNTEKTSENDRING(SAKTYPE_OMS, true, false, false),
    OMGJOERING_AV_FARSKAP(SAKTYPE_BP, true, false, true);

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