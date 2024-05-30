package no.nav.etterlatte.libs.common.behandling

import no.nav.etterlatte.libs.common.behandling.KanBrukesIMiljoe.DevOgProd
import no.nav.etterlatte.libs.common.behandling.KanBrukesIMiljoe.KunIDev
import no.nav.etterlatte.libs.common.clusterNavn

private val SAKTYPE_OMS = listOf(SakType.OMSTILLINGSSTOENAD)
private val SAKTYPE_BP = listOf(SakType.BARNEPENSJON)
private val SAKTYPE_BP_OMS = SAKTYPE_BP + SAKTYPE_OMS

private sealed class KanBrukesIMiljoe {
    abstract val prod: Boolean
    abstract val dev: Boolean

    data object KunIDev : KanBrukesIMiljoe() {
        override val prod = false
        override val dev = true
    }

    data object DevOgProd : KanBrukesIMiljoe() {
        override val prod = true
        override val dev = true
    }

    data object IngenMiljoe : KanBrukesIMiljoe() {
        override val prod = false
        override val dev = false
    }
}

// Disse må ha en oversettelse i frontend Revurderingaarsak.ts
enum class Revurderingaarsak(
    private val gyldigFor: List<SakType>,
    private val miljoe: KanBrukesIMiljoe,
) {
    // Endring
    NY_SOEKNAD(SAKTYPE_BP_OMS, DevOgProd),
    SOESKENJUSTERING(SAKTYPE_BP, DevOgProd),
    REGULERING(SAKTYPE_BP_OMS, DevOgProd),
    INNTEKTSENDRING(SAKTYPE_OMS, DevOgProd),
    INSTITUSJONSOPPHOLD(SAKTYPE_BP_OMS, DevOgProd),
    YRKESSKADE(SAKTYPE_BP_OMS, DevOgProd),
    RETT_UTEN_TIDSBEGRENSNING(SAKTYPE_OMS, DevOgProd),
    FORELDRELOES(SAKTYPE_BP, DevOgProd),

    // Opphør
    ALDERSOVERGANG(SAKTYPE_BP_OMS, DevOgProd),
    DOEDSFALL(SAKTYPE_BP_OMS, DevOgProd),
    ADOPSJON(SAKTYPE_BP, DevOgProd),
    SIVILSTAND(SAKTYPE_OMS, DevOgProd),
    OMGJOERING_AV_FARSKAP(SAKTYPE_BP, DevOgProd),

    // Opphør og endring
    EKSPORT(SAKTYPE_BP_OMS, DevOgProd),
    IMPORT(SAKTYPE_BP_OMS, DevOgProd),
    ANNEN(SAKTYPE_BP_OMS, DevOgProd),

    UTSENDELSE_AV_KRAVPAKKE(SAKTYPE_BP_OMS, DevOgProd),

    OMGJOERING_ETTER_KLAGE(SAKTYPE_BP_OMS, DevOgProd),
    SLUTTBEHANDLING_UTLAND(SAKTYPE_BP_OMS, DevOgProd),

    FENGSELSOPPHOLD(SAKTYPE_BP, DevOgProd),
    UT_AV_FENGSEL(SAKTYPE_BP, DevOgProd),
    UTLAND(SAKTYPE_BP, DevOgProd),
    BARN(SAKTYPE_BP, DevOgProd),
    ANSVARLIGE_FORELDRE(SAKTYPE_BP, DevOgProd),
    VERGEMAAL_ELLER_FREMTIDSFULLMAKT(SAKTYPE_BP, DevOgProd),

    AVKORTING_MOT_UFOERETRYGD(SAKTYPE_BP, DevOgProd),

    OPPHOER_3_AAR_ETTER_DOEDSFALL(SAKTYPE_OMS, DevOgProd),
    SOEKNAD_OM_GJENOPPTAK(SAKTYPE_OMS, DevOgProd),

    OMGJOERING_ETTER_ANKE(SAKTYPE_BP_OMS, DevOgProd),
    OMGJOERING_PAA_EGET_INITIATIV(SAKTYPE_BP_OMS, DevOgProd),
    OMGJOERING_ETTER_KRAV_FRA_BRUKER(SAKTYPE_BP_OMS, DevOgProd),

    // Mangler funksjonalitet
    UTSENDELSE_AV_SED(SAKTYPE_BP_OMS, KunIDev),
    ETTEROPPGJOER(SAKTYPE_OMS, KunIDev),
    SANKSJON_PGA_MANGLENDE_OPPLYSNINGER(SAKTYPE_OMS, KunIDev),
    OPPHOER_AV_2_UTVIDEDE_AAR(SAKTYPE_OMS, KunIDev),
    ;

    fun kanBrukesIMiljo(): Boolean =
        when (clusterNavn()) {
            null -> true
            GcpEnv.PROD.env -> miljoe.prod
            GcpEnv.DEV.env -> miljoe.dev
            else -> miljoe.dev
        }

    fun gyldigForSakType(sakType: SakType): Boolean = gyldigFor.any { it == sakType }

    fun erStoettaRevurdering(sakType: SakType) = kanBrukesIMiljo() && gyldigForSakType(sakType) && this != NY_SOEKNAD
}

enum class GcpEnv(val env: String) {
    PROD("prod-gcp"),
    DEV("dev-gcp"),
}
