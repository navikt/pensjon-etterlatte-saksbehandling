package no.nav.etterlatte.libs.common.behandling

import no.nav.etterlatte.libs.common.GcpEnv
import no.nav.etterlatte.libs.common.behandling.KanBrukesIMiljoe.DevOgProd
import no.nav.etterlatte.libs.common.behandling.KanBrukesIMiljoe.IngenMiljoe
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

/*
    Disse må ha en oversettelse i frontend Revurderingaarsak.ts
    Endring av enumnavnet her må også hensynta at det ligger lagret i vedtaksbasen og parses med objectmapper der til denne klassen.
 */
enum class Revurderingaarsak(
    private val gyldigFor: List<SakType>,
    private val miljoe: KanBrukesIMiljoe,
    val skalSendeBrev: Boolean,
) {
    // Endring
    NY_SOEKNAD(SAKTYPE_BP_OMS, DevOgProd, skalSendeBrev = true),
    SOESKENJUSTERING(SAKTYPE_BP, DevOgProd, skalSendeBrev = true),
    REGULERING(SAKTYPE_BP_OMS, DevOgProd, skalSendeBrev = false),
    OMREGNING(SAKTYPE_BP_OMS, KunIDev, skalSendeBrev = false), // TODO
    INNTEKTSENDRING(SAKTYPE_OMS, DevOgProd, skalSendeBrev = true),
    AARLIG_INNTEKTSJUSTERING(SAKTYPE_OMS, DevOgProd, skalSendeBrev = true),
    INSTITUSJONSOPPHOLD(SAKTYPE_BP_OMS, DevOgProd, skalSendeBrev = true),
    YRKESSKADE(SAKTYPE_BP_OMS, DevOgProd, skalSendeBrev = true),
    RETT_UTEN_TIDSBEGRENSNING(SAKTYPE_OMS, DevOgProd, skalSendeBrev = true),
    FORELDRELOES(SAKTYPE_BP, DevOgProd, skalSendeBrev = true),
    FRA_0UTBETALING_TIL_UTBETALING(SAKTYPE_OMS, DevOgProd, skalSendeBrev = true),

    // Opphør
    ALDERSOVERGANG(SAKTYPE_BP_OMS, DevOgProd, skalSendeBrev = false),
    DOEDSFALL(SAKTYPE_BP_OMS, DevOgProd, skalSendeBrev = false),
    ADOPSJON(SAKTYPE_BP, DevOgProd, skalSendeBrev = true),
    SIVILSTAND(SAKTYPE_OMS, DevOgProd, skalSendeBrev = true),
    OMGJOERING_AV_FARSKAP(SAKTYPE_BP, DevOgProd, skalSendeBrev = true),

    // Opphør og endring
    EKSPORT(SAKTYPE_BP_OMS, DevOgProd, skalSendeBrev = true),
    IMPORT(SAKTYPE_BP_OMS, DevOgProd, skalSendeBrev = true),
    ANNEN(SAKTYPE_BP_OMS, DevOgProd, skalSendeBrev = true),
    AKTIVITETSPLIKT(SAKTYPE_OMS, DevOgProd, skalSendeBrev = true),

    UTSENDELSE_AV_KRAVPAKKE(SAKTYPE_BP_OMS, DevOgProd, skalSendeBrev = false),

    OMGJOERING_ETTER_KLAGE(SAKTYPE_BP_OMS, DevOgProd, skalSendeBrev = true),
    SLUTTBEHANDLING(SAKTYPE_BP_OMS, DevOgProd, skalSendeBrev = true),

    FENGSELSOPPHOLD(SAKTYPE_BP, DevOgProd, skalSendeBrev = true),
    UT_AV_FENGSEL(SAKTYPE_BP, DevOgProd, skalSendeBrev = true),
    UTLAND(SAKTYPE_BP, DevOgProd, skalSendeBrev = true),
    BARN(SAKTYPE_BP, DevOgProd, skalSendeBrev = true),
    ANSVARLIGE_FORELDRE(SAKTYPE_BP, DevOgProd, skalSendeBrev = true),
    VERGEMAAL_ELLER_FREMTIDSFULLMAKT(SAKTYPE_BP, DevOgProd, skalSendeBrev = true),

    AVKORTING_MOT_UFOERETRYGD(SAKTYPE_BP, DevOgProd, skalSendeBrev = true),

    OPPHOER_3_AAR_ETTER_DOEDSFALL(SAKTYPE_OMS, DevOgProd, skalSendeBrev = true),
    SOEKNAD_OM_GJENOPPTAK(SAKTYPE_OMS, DevOgProd, skalSendeBrev = true),

    OMGJOERING_ETTER_ANKE(SAKTYPE_BP_OMS, DevOgProd, skalSendeBrev = true),
    OMGJOERING_PAA_EGET_INITIATIV(SAKTYPE_BP_OMS, DevOgProd, skalSendeBrev = true),
    OMGJOERING_ETTER_KRAV_FRA_BRUKER(SAKTYPE_BP_OMS, DevOgProd, skalSendeBrev = true),

    REVURDERE_ETTER_OPPHOER(SAKTYPE_BP_OMS, DevOgProd, skalSendeBrev = false),

    // Mangler funksjonalitet
    UTSENDELSE_AV_SED(SAKTYPE_BP_OMS, KunIDev, skalSendeBrev = true),
    SANKSJON_PGA_MANGLENDE_OPPLYSNINGER(SAKTYPE_OMS, KunIDev, skalSendeBrev = true),
    OPPHOER_AV_2_UTVIDEDE_AAR(SAKTYPE_OMS, KunIDev, skalSendeBrev = true),

    // Skal ikke kunne opprettes via vanlig flyt
    ETTEROPPGJOER(SAKTYPE_OMS, IngenMiljoe, skalSendeBrev = true),

    // Utgår men har blitt brukt
    ANNEN_UTEN_BREV(SAKTYPE_BP_OMS, IngenMiljoe, skalSendeBrev = false),
    OPPHOER_UTEN_BREV(SAKTYPE_BP_OMS, IngenMiljoe, skalSendeBrev = false),
    ;

    fun kanBrukesIMiljo(): Boolean =
        when (clusterNavn()) {
            null -> true
            GcpEnv.PROD.env -> miljoe.prod
            GcpEnv.DEV.env -> miljoe.dev
            else -> miljoe.dev
        }

    fun gyldigForSakType(sakType: SakType): Boolean = gyldigFor.any { it == sakType }

    fun erStoettaRevurdering(sakType: SakType): Boolean {
        val erIkkeStoetta = listOf(AARLIG_INNTEKTSJUSTERING)
        return kanBrukesIMiljo() && gyldigForSakType(sakType) && !erIkkeStoetta.contains(this)
    }

    fun kanLagreFritekstFeltForManuellRevurdering(): Boolean = this in listOf<Revurderingaarsak>(ANNEN, ANNEN_UTEN_BREV)
}
