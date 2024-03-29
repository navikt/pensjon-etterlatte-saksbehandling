package no.nav.etterlatte.libs.common.behandling

import no.nav.etterlatte.libs.common.behandling.KanBrukesIMiljoe.DevOgProd
import no.nav.etterlatte.libs.common.behandling.KanBrukesIMiljoe.IngenMiljoe
import no.nav.etterlatte.libs.common.behandling.KanBrukesIMiljoe.KunIDev
import no.nav.etterlatte.libs.common.behandling.Utfall.IkkeOpphoerSkalIkkeSendeBrev
import no.nav.etterlatte.libs.common.behandling.Utfall.IkkeOpphoerSkalSendeBrev
import no.nav.etterlatte.libs.common.behandling.Utfall.OpphoerMedBrev
import no.nav.etterlatte.libs.common.behandling.Utfall.OpphoerUtenBrev
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
) {
    // Endring
    NY_SOEKNAD(SAKTYPE_BP_OMS, DevOgProd, IkkeOpphoerSkalSendeBrev),
    SOESKENJUSTERING(SAKTYPE_BP, DevOgProd, IkkeOpphoerSkalSendeBrev),
    REGULERING(SAKTYPE_BP_OMS, DevOgProd, IkkeOpphoerSkalIkkeSendeBrev),
    INNTEKTSENDRING(SAKTYPE_OMS, DevOgProd, IkkeOpphoerSkalSendeBrev),
    INSTITUSJONSOPPHOLD(SAKTYPE_BP_OMS, DevOgProd, IkkeOpphoerSkalSendeBrev),
    YRKESSKADE(SAKTYPE_BP_OMS, DevOgProd, IkkeOpphoerSkalSendeBrev),
    RETT_UTEN_TIDSBEGRENSNING(SAKTYPE_OMS, DevOgProd, IkkeOpphoerSkalSendeBrev),

    // Opphør
    ALDERSOVERGANG(SAKTYPE_BP_OMS, DevOgProd, OpphoerUtenBrev),
    DOEDSFALL(SAKTYPE_BP_OMS, DevOgProd, OpphoerUtenBrev),
    OPPHOER_UTEN_BREV(SAKTYPE_BP_OMS, DevOgProd, OpphoerUtenBrev),
    ADOPSJON(SAKTYPE_BP, DevOgProd, OpphoerMedBrev),
    SIVILSTAND(SAKTYPE_OMS, DevOgProd, OpphoerMedBrev),
    OMGJOERING_AV_FARSKAP(SAKTYPE_BP, DevOgProd, OpphoerMedBrev),

    // Opphør og endring
    EKSPORT(SAKTYPE_BP_OMS, DevOgProd, IkkeOpphoerSkalSendeBrev),
    IMPORT(SAKTYPE_BP_OMS, DevOgProd, IkkeOpphoerSkalSendeBrev),
    ANNEN(SAKTYPE_BP_OMS, DevOgProd, IkkeOpphoerSkalSendeBrev),
    ANNEN_UTEN_BREV(SAKTYPE_BP_OMS, DevOgProd, IkkeOpphoerSkalIkkeSendeBrev),

    // TODO vurdere disse
    OMGJOERING_ETTER_KLAGE(SAKTYPE_BP_OMS, KunIDev, IkkeOpphoerSkalSendeBrev),
    SLUTTBEHANDLING_UTLAND(SAKTYPE_BP_OMS, KunIDev, IkkeOpphoerSkalSendeBrev),

    // TODO ikke i noe miljø ennå
    FENGSELSOPPHOLD(SAKTYPE_BP, IngenMiljoe, IkkeOpphoerSkalSendeBrev),
    UT_AV_FENGSEL(SAKTYPE_BP, IngenMiljoe, IkkeOpphoerSkalSendeBrev),
    UTLAND(SAKTYPE_BP, IngenMiljoe, IkkeOpphoerSkalSendeBrev),
    BARN(SAKTYPE_BP, IngenMiljoe, IkkeOpphoerSkalSendeBrev),
    ANSVARLIGE_FORELDRE(SAKTYPE_BP, IngenMiljoe, IkkeOpphoerSkalSendeBrev),
    VERGEMAAL_ELLER_FREMTIDSFULLMAKT(SAKTYPE_BP, IngenMiljoe, IkkeOpphoerSkalSendeBrev),
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
