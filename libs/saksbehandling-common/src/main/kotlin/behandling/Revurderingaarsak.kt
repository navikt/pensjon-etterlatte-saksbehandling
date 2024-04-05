package no.nav.etterlatte.libs.common.behandling

import no.nav.etterlatte.libs.common.behandling.KanBrukesIMiljoe.DevOgProd
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
    INNTEKTSENDRING(SAKTYPE_OMS, DevOgProd, IkkeOpphoerSkalSendeBrev),
    INSTITUSJONSOPPHOLD(SAKTYPE_BP_OMS, DevOgProd, IkkeOpphoerSkalSendeBrev),
    NY_SOEKNAD(SAKTYPE_BP_OMS, DevOgProd, IkkeOpphoerSkalSendeBrev),
    REGULERING(SAKTYPE_BP_OMS, DevOgProd, IkkeOpphoerSkalIkkeSendeBrev),
    RETT_UTEN_TIDSBEGRENSNING(SAKTYPE_OMS, DevOgProd, IkkeOpphoerSkalSendeBrev),
    SOESKENJUSTERING(SAKTYPE_BP, DevOgProd, IkkeOpphoerSkalSendeBrev),
    YRKESSKADE(SAKTYPE_BP_OMS, DevOgProd, IkkeOpphoerSkalSendeBrev),

    // Opphør
    ADOPSJON(SAKTYPE_BP, DevOgProd, OpphoerMedBrev),
    ALDERSOVERGANG(SAKTYPE_BP_OMS, DevOgProd, OpphoerUtenBrev),
    DOEDSFALL(SAKTYPE_BP_OMS, DevOgProd, OpphoerUtenBrev),
    OMGJOERING_AV_FARSKAP(SAKTYPE_BP, DevOgProd, OpphoerMedBrev),
    OPPHOER_UTEN_BREV(SAKTYPE_BP_OMS, DevOgProd, OpphoerUtenBrev),
    SIVILSTAND(SAKTYPE_OMS, DevOgProd, OpphoerMedBrev),

    // Opphør og endring
    ANNEN(SAKTYPE_BP_OMS, DevOgProd, IkkeOpphoerSkalSendeBrev),
    ANNEN_UTEN_BREV(SAKTYPE_BP_OMS, DevOgProd, IkkeOpphoerSkalIkkeSendeBrev),
    EKSPORT(SAKTYPE_BP_OMS, DevOgProd, IkkeOpphoerSkalSendeBrev),
    IMPORT(SAKTYPE_BP_OMS, DevOgProd, IkkeOpphoerSkalSendeBrev),

    // TODO vurdere disse
    OMGJOERING_ETTER_KLAGE(SAKTYPE_BP_OMS, DevOgProd, IkkeOpphoerSkalSendeBrev),
    SLUTTBEHANDLING_UTLAND(SAKTYPE_BP_OMS, DevOgProd, IkkeOpphoerSkalSendeBrev),

    // TODO ikke i noe miljø ennå
    AKTIVITETSPLIKT(SAKTYPE_OMS, DevOgProd, IkkeOpphoerSkalSendeBrev),
    ANSVARLIGE_FORELDRE(SAKTYPE_BP, DevOgProd, IkkeOpphoerSkalSendeBrev),
    AVKORTING_MOT_UFOERETRYGD(SAKTYPE_BP, DevOgProd, IkkeOpphoerSkalSendeBrev), // NY
    BARN(SAKTYPE_BP, DevOgProd, IkkeOpphoerSkalSendeBrev),
    ETTEROPPGJOER(SAKTYPE_OMS, DevOgProd, IkkeOpphoerSkalSendeBrev), // NY
    FENGSELSOPPHOLD(SAKTYPE_BP, DevOgProd, IkkeOpphoerSkalSendeBrev),
    FRA_EN_TIL_TO_FORELDRE_DOED(SAKTYPE_BP, DevOgProd, IkkeOpphoerSkalSendeBrev), // NY
    OMGJOERING_ETTER_ANKE(SAKTYPE_BP_OMS, DevOgProd, IkkeOpphoerSkalSendeBrev), // NY
    OMGJOERING_PAA_EGET_INITIATIV(SAKTYPE_BP_OMS, DevOgProd, IkkeOpphoerSkalSendeBrev), // NY
    OMGJOERING_ETTER_KRAV_FRA_BRUKER(SAKTYPE_BP_OMS, DevOgProd, IkkeOpphoerSkalSendeBrev), // NY
    OPPHOER_3_AAR_ETTER_DOEDSFALL(SAKTYPE_OMS, DevOgProd, OpphoerUtenBrev), // NY
    OPPHOER_AV_2_UTVIDEDE_AAR(SAKTYPE_OMS, DevOgProd, OpphoerMedBrev), // NY
    SANKSJON_PGA_MANGLENDE_OPPLYSNINGER(SAKTYPE_OMS, DevOgProd, IkkeOpphoerSkalSendeBrev), // NY
    SOEKNAD_OM_GJENOPPTAK(SAKTYPE_OMS, DevOgProd, IkkeOpphoerSkalSendeBrev), // NY
    UT_AV_FENGSEL(SAKTYPE_BP, DevOgProd, IkkeOpphoerSkalSendeBrev),
    UTLAND(SAKTYPE_BP, DevOgProd, IkkeOpphoerSkalSendeBrev),
    UTSENDELSE_AV_KRAVPAKKE(SAKTYPE_BP_OMS, DevOgProd, IkkeOpphoerSkalSendeBrev), // NY
    UTSENDELSE_AV_SED(SAKTYPE_BP_OMS, DevOgProd, IkkeOpphoerSkalSendeBrev), // NY, MEN USIKKER BEHOVET
    VERGEMAAL_ELLER_FREMTIDSFULLMAKT(SAKTYPE_BP_OMS, DevOgProd, IkkeOpphoerSkalSendeBrev), // ENDRET
    ;

    // Må vurdere om vi trenger sjekk om det kan brukes i miljø når alle vil vises i dev og prod
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
