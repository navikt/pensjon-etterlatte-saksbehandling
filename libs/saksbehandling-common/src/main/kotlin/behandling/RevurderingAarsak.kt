package no.nav.etterlatte.libs.common.behandling

import no.nav.etterlatte.libs.common.behandling.KanBrukesIMiljoe.DevOgProd
import no.nav.etterlatte.libs.common.behandling.KanBrukesIMiljoe.IngenMiljoe
import no.nav.etterlatte.libs.common.behandling.KanBrukesIMiljoe.KunIDev
import no.nav.etterlatte.libs.common.behandling.Utfall.IkkeOpphoerSkalIkkeSendeBrev
import no.nav.etterlatte.libs.common.behandling.Utfall.IkkeOpphoerSkalSendeBrev
import no.nav.etterlatte.libs.common.behandling.Utfall.OpphoerMedBrev
import no.nav.etterlatte.libs.common.behandling.Utfall.OpphoerUtenBrev
import no.nav.etterlatte.libs.common.clusternavn

private val SAKTYPE_OMS = listOf(SakType.OMSTILLINGSSTOENAD)
private val SAKTYPE_BP = listOf(SakType.BARNEPENSJON)
private val SAKTYPE_BP_OMS = SAKTYPE_BP + SAKTYPE_OMS

private sealed class KanBrukesIMiljoe {
    abstract val prod: Boolean
    abstract val dev: Boolean

    object KunIDev : KanBrukesIMiljoe() {
        override val prod = false
        override val dev = true
    }

    object DevOgProd : KanBrukesIMiljoe() {
        override val prod = true
        override val dev = true
    }

    object IngenMiljoe : KanBrukesIMiljoe() {
        override val prod = false
        override val dev = false
    }
}

sealed class Utfall {
    abstract val girOpphoer: Boolean
    abstract val skalSendeBrev: Boolean

    object OpphoerUtenBrev : Utfall() {
        override val girOpphoer = true
        override val skalSendeBrev = false
    }

    object OpphoerMedBrev : Utfall() {
        override val girOpphoer = true
        override val skalSendeBrev = true
    }

    object IkkeOpphoerSkalSendeBrev : Utfall() {
        override val girOpphoer = false
        override val skalSendeBrev = true
    }

    object IkkeOpphoerSkalIkkeSendeBrev : Utfall() {
        override val girOpphoer = false
        override val skalSendeBrev = false
    }
}

// Disse må ha en oversettelse i frontend RevurderingAarsak.ts
enum class RevurderingAarsak(
    private val gyldigFor: List<SakType>,
    private val miljoe: KanBrukesIMiljoe,
    val utfall: Utfall,
    val redigerbartBrev: Boolean = false
) {
    ANSVARLIGE_FORELDRE(SAKTYPE_BP, IngenMiljoe, IkkeOpphoerSkalSendeBrev),
    SOESKENJUSTERING(SAKTYPE_BP, KunIDev, IkkeOpphoerSkalSendeBrev),
    UTLAND(SAKTYPE_BP, IngenMiljoe, IkkeOpphoerSkalSendeBrev),
    BARN(SAKTYPE_BP, IngenMiljoe, IkkeOpphoerSkalSendeBrev),
    VERGEMAAL_ELLER_FREMTIDSFULLMAKT(SAKTYPE_BP, IngenMiljoe, IkkeOpphoerSkalSendeBrev),
    REGULERING(SAKTYPE_BP_OMS, DevOgProd, IkkeOpphoerSkalIkkeSendeBrev),
    DOEDSFALL(SAKTYPE_BP_OMS, KunIDev, OpphoerUtenBrev),
    INNTEKTSENDRING(SAKTYPE_OMS, KunIDev, IkkeOpphoerSkalSendeBrev),
    OMGJOERING_AV_FARSKAP(SAKTYPE_BP, KunIDev, OpphoerMedBrev, redigerbartBrev = true),
    ADOPSJON(SAKTYPE_BP, KunIDev, OpphoerMedBrev, redigerbartBrev = true),
    SIVILSTAND(SAKTYPE_OMS, KunIDev, OpphoerMedBrev),
    FENGSELSOPPHOLD(SAKTYPE_BP, KunIDev, IkkeOpphoerSkalSendeBrev, redigerbartBrev = true),
    UT_AV_FENGSEL(SAKTYPE_BP, KunIDev, IkkeOpphoerSkalSendeBrev, redigerbartBrev = true),
    NY_SOEKNAD(SAKTYPE_BP_OMS, DevOgProd, IkkeOpphoerSkalSendeBrev),
    ANNEN(SAKTYPE_BP_OMS, KunIDev, IkkeOpphoerSkalSendeBrev),
    YRKESSKADE(SAKTYPE_BP, KunIDev, IkkeOpphoerSkalSendeBrev, redigerbartBrev = true);

    fun kanBrukesIMiljo(): Boolean = when (clusternavn()) {
        null -> true
        GcpEnv.PROD.name -> miljoe.prod
        GcpEnv.DEV.name -> miljoe.dev
        else -> miljoe.dev
    }

    fun gyldigForSakType(sakType: SakType): Boolean = gyldigFor.any { it == sakType }

    fun erStoettaRevurdering(sakType: SakType) = kanBrukesIMiljo() && gyldigForSakType(sakType) && this != NY_SOEKNAD
}

enum class GcpEnv(val env: String) {
    PROD("prod-gcp"),
    DEV("dev-gcp")
}