package no.nav.etterlatte.libs.common.behandling

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

// Disse må ha en oversettelse i frontend Revurderingaarsak.ts
enum class Revurderingaarsak(
    private val gyldigFor: List<SakType>,
    private val miljoe: KanBrukesIMiljoe,
    val skalSendeBrev: Boolean,
) {
    // Endring
    NY_SOEKNAD(SAKTYPE_BP_OMS, DevOgProd, skalSendeBrev = true),
    SOESKENJUSTERING(SAKTYPE_BP, DevOgProd, skalSendeBrev = true),
    REGULERING(SAKTYPE_BP_OMS, DevOgProd, skalSendeBrev = false),
    INNTEKTSENDRING(SAKTYPE_OMS, DevOgProd, skalSendeBrev = true),
    INSTITUSJONSOPPHOLD(SAKTYPE_BP_OMS, DevOgProd, skalSendeBrev = true),
    YRKESSKADE(SAKTYPE_BP_OMS, DevOgProd, skalSendeBrev = true),
    RETT_UTEN_TIDSBEGRENSNING(SAKTYPE_OMS, DevOgProd, skalSendeBrev = true),
    FORELDRELOES(SAKTYPE_BP, KunIDev, skalSendeBrev = true),

    // Opphør
    ALDERSOVERGANG(SAKTYPE_BP_OMS, DevOgProd, skalSendeBrev = false),
    DOEDSFALL(SAKTYPE_BP_OMS, DevOgProd, skalSendeBrev = false),
    OPPHOER_UTEN_BREV(SAKTYPE_BP_OMS, DevOgProd, skalSendeBrev = false),
    ADOPSJON(SAKTYPE_BP, DevOgProd, skalSendeBrev = true),
    SIVILSTAND(SAKTYPE_OMS, DevOgProd, skalSendeBrev = true),
    OMGJOERING_AV_FARSKAP(SAKTYPE_BP, DevOgProd, skalSendeBrev = true),

    // Opphør og endring
    EKSPORT(SAKTYPE_BP_OMS, DevOgProd, skalSendeBrev = true),
    IMPORT(SAKTYPE_BP_OMS, DevOgProd, skalSendeBrev = true),
    ANNEN(SAKTYPE_BP_OMS, DevOgProd, skalSendeBrev = true),
    ANNEN_UTEN_BREV(SAKTYPE_BP_OMS, DevOgProd, skalSendeBrev = false),

    // TODO vurdere disse
    OMGJOERING_ETTER_KLAGE(SAKTYPE_BP_OMS, KunIDev, skalSendeBrev = true),
    SLUTTBEHANDLING_UTLAND(SAKTYPE_BP_OMS, KunIDev, skalSendeBrev = true),

    // TODO ikke i noe miljø ennå
    FENGSELSOPPHOLD(SAKTYPE_BP, IngenMiljoe, skalSendeBrev = true),
    UT_AV_FENGSEL(SAKTYPE_BP, IngenMiljoe, skalSendeBrev = true),
    UTLAND(SAKTYPE_BP, IngenMiljoe, skalSendeBrev = true),
    BARN(SAKTYPE_BP, IngenMiljoe, skalSendeBrev = true),
    ANSVARLIGE_FORELDRE(SAKTYPE_BP, IngenMiljoe, skalSendeBrev = true),
    VERGEMAAL_ELLER_FREMTIDSFULLMAKT(SAKTYPE_BP, IngenMiljoe, skalSendeBrev = true),
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
