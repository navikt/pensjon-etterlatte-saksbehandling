package no.nav.etterlatte.behandling.etteroppgjoer

import no.nav.etterlatte.libs.common.feilhaandtering.sjekk
import no.nav.etterlatte.libs.common.sak.SakId
import java.util.UUID

data class Etteroppgjoer(
    val sakId: SakId,
    val inntektsaar: Int,
    val status: EtteroppgjoerStatus,
    val harSanksjon: Boolean = false,
    val harInstitusjonsopphold: Boolean = false,
    val harOpphoer: Boolean = false,
    val harAdressebeskyttelseEllerSkjermet: Boolean = false,
    val harAktivitetskrav: Boolean = false,
    val harBosattUtland: Boolean = false,
    val harUtlandstilsnitt: Boolean = false,
    val harOverstyrtBeregning: Boolean = false,
    val sisteFerdigstilteForbehandling: UUID? = null,
) {
    fun venterPaaSkatteoppgjoer() = status == EtteroppgjoerStatus.VENTER_PAA_SKATTEOPPGJOER

    fun mottattSkatteoppgjoer() = status == EtteroppgjoerStatus.MOTTATT_SKATTEOPPGJOER

    fun kanOppretteRevurdering() =
        status in listOf(EtteroppgjoerStatus.VENTER_PAA_SVAR, EtteroppgjoerStatus.FERDIGSTILT, EtteroppgjoerStatus.OMGJOERING)

    fun kanOppretteForbehandling() =
        status in listOf(EtteroppgjoerStatus.MANGLER_SKATTEOPPGJOER, EtteroppgjoerStatus.MOTTATT_SKATTEOPPGJOER)

    fun kanOppdateresMedSkatteoppgjoerMottatt(): Boolean = venterPaaSkatteoppgjoer() || mottattSkatteoppgjoer()

    fun kanTilbakestilles() = status in listOf(EtteroppgjoerStatus.UNDER_REVURDERING, EtteroppgjoerStatus.OMGJOERING)

    fun tilbakestill(erEndringTilUgunst: Boolean): Etteroppgjoer {
        sjekk(kanTilbakestilles()) {
            "Kan ikke tilbakestille etteroppgjør for sakId=$sakId: " +
                "forventet status ${EtteroppgjoerStatus.UNDER_REVURDERING} " +
                "eller ${EtteroppgjoerStatus.OMGJOERING}, fant $status"
        }
        val nyStatus = if (erEndringTilUgunst) EtteroppgjoerStatus.MOTTATT_SKATTEOPPGJOER else EtteroppgjoerStatus.VENTER_PAA_SVAR
        return copy(status = nyStatus)
    }
}

enum class EtteroppgjoerStatus {
    VENTER_PAA_SKATTEOPPGJOER,
    MOTTATT_SKATTEOPPGJOER,
    MANGLER_SKATTEOPPGJOER,
    VENTER_PAA_SVAR,

    UNDER_FORBEHANDLING,
    UNDER_REVURDERING,

    FERDIGSTILT,
    OMGJOERING, // midlertidig status for omgjøring av revurdering
}
