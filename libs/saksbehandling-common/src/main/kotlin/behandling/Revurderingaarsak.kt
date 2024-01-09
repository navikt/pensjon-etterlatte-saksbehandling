package no.nav.etterlatte.libs.common.behandling

import no.nav.etterlatte.libs.common.behandling.KanBrukesIMiljoe.DevOgProd
import no.nav.etterlatte.libs.common.behandling.KanBrukesIMiljoe.IngenMiljoe
import no.nav.etterlatte.libs.common.behandling.KanBrukesIMiljoe.KunIDev
import no.nav.etterlatte.libs.common.behandling.Utfall.IkkeOpphoerSkalIkkeSendeBrev
import no.nav.etterlatte.libs.common.behandling.Utfall.IkkeOpphoerSkalSendeBrev
import no.nav.etterlatte.libs.common.behandling.Utfall.OpphoerMedBrev
import no.nav.etterlatte.libs.common.behandling.Utfall.OpphoerUtenBrev
import no.nav.etterlatte.libs.common.clusternavn
import org.slf4j.Logger
import org.slf4j.LoggerFactory

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

sealed class Utfall {
    abstract val girOpphoer: Boolean
    abstract val skalSendeBrev: Boolean

    data object OpphoerUtenBrev : Utfall() {
        override val girOpphoer = true
        override val skalSendeBrev = false
    }

    data object OpphoerMedBrev : Utfall() {
        override val girOpphoer = true
        override val skalSendeBrev = true
    }

    data object IkkeOpphoerSkalSendeBrev : Utfall() {
        override val girOpphoer = false
        override val skalSendeBrev = true
    }

    data object IkkeOpphoerSkalIkkeSendeBrev : Utfall() {
        override val girOpphoer = false
        override val skalSendeBrev = false
    }
}

// Disse må ha en oversettelse i frontend Revurderingaarsak.ts
enum class Revurderingaarsak(
    private val gyldigFor: List<SakType>,
    private val miljoe: KanBrukesIMiljoe,
    val utfall: Utfall,
    val redigerbartBrev: Boolean = false,
) {
    ANSVARLIGE_FORELDRE(SAKTYPE_BP, IngenMiljoe, IkkeOpphoerSkalSendeBrev),
    SOESKENJUSTERING(SAKTYPE_BP, KunIDev, IkkeOpphoerSkalSendeBrev),
    UTLAND(SAKTYPE_BP, IngenMiljoe, IkkeOpphoerSkalSendeBrev),
    BARN(SAKTYPE_BP, IngenMiljoe, IkkeOpphoerSkalSendeBrev),
    VERGEMAAL_ELLER_FREMTIDSFULLMAKT(SAKTYPE_BP, IngenMiljoe, IkkeOpphoerSkalSendeBrev),
    REGULERING(SAKTYPE_BP_OMS, DevOgProd, IkkeOpphoerSkalIkkeSendeBrev),
    DOEDSFALL(SAKTYPE_BP_OMS, DevOgProd, OpphoerUtenBrev),
    INNTEKTSENDRING(SAKTYPE_OMS, KunIDev, IkkeOpphoerSkalSendeBrev),
    OMGJOERING_AV_FARSKAP(SAKTYPE_BP, KunIDev, OpphoerMedBrev, redigerbartBrev = true),
    ADOPSJON(SAKTYPE_BP, KunIDev, OpphoerMedBrev, redigerbartBrev = true),
    SIVILSTAND(SAKTYPE_OMS, KunIDev, OpphoerMedBrev, redigerbartBrev = true),
    FENGSELSOPPHOLD(SAKTYPE_BP, KunIDev, IkkeOpphoerSkalSendeBrev, redigerbartBrev = true),
    UT_AV_FENGSEL(SAKTYPE_BP, KunIDev, IkkeOpphoerSkalSendeBrev, redigerbartBrev = true),
    NY_SOEKNAD(SAKTYPE_BP_OMS, DevOgProd, IkkeOpphoerSkalSendeBrev),
    ANNEN(SAKTYPE_BP_OMS, KunIDev, IkkeOpphoerSkalSendeBrev),
    INSTITUSJONSOPPHOLD(SAKTYPE_BP_OMS, KunIDev, IkkeOpphoerSkalSendeBrev, redigerbartBrev = true),
    YRKESSKADE(SAKTYPE_BP_OMS, KunIDev, IkkeOpphoerSkalSendeBrev, redigerbartBrev = true),
    OMGJOERING_ETTER_KLAGE(SAKTYPE_BP_OMS, KunIDev, IkkeOpphoerSkalSendeBrev, redigerbartBrev = true),
    SLUTTBEHANDLING_UTLAND(SAKTYPE_BP_OMS, KunIDev, IkkeOpphoerSkalSendeBrev, redigerbartBrev = true),
    OPPHOER_UTEN_BREV(SAKTYPE_BP_OMS, DevOgProd, OpphoerUtenBrev),
    ;

    private val log: Logger = LoggerFactory.getLogger(Revurderingaarsak::class.java)

    fun kanBrukesIMiljo(): Boolean {
        val clusternavn = clusternavn()

        val kanBrukes =
            when (clusternavn) {
                null -> true
                GcpEnv.PROD.name -> miljoe.prod
                GcpEnv.DEV.name -> miljoe.dev
                else -> miljoe.dev
            }

        log.info("Miljøsjekk i cluster: $clusternavn for $this med $miljoe ga resultat $kanBrukes")

        return kanBrukes
    }

    fun gyldigForSakType(sakType: SakType): Boolean = gyldigFor.any { it == sakType }

    fun erStoettaRevurdering(sakType: SakType) = kanBrukesIMiljo() && gyldigForSakType(sakType) && this != NY_SOEKNAD
}

enum class GcpEnv(val env: String) {
    PROD("prod-gcp"),
    DEV("dev-gcp"),
}
