package no.nav.etterlatte.behandling.etteroppgjoer

import no.nav.etterlatte.behandling.etteroppgjoer.forbehandling.EtteroppgjoerForbehandling
import no.nav.etterlatte.libs.common.behandling.etteroppgjoer.EtteroppgjoerForbehandlingStatus
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
    fun venterPaaSkatteoppgjoer() =
        status in
            listOf(
                EtteroppgjoerStatus.VENTER_PAA_SKATTEOPPGJOER,
                EtteroppgjoerStatus.MOTTATT_SKATTEOPPGJOER,
            )

    fun mottattSkatteoppgjoer() = status == EtteroppgjoerStatus.MOTTATT_SKATTEOPPGJOER

    fun kanTilbakestillesMedNyForbehandling(forbehandling: EtteroppgjoerForbehandling) {
        if (status !in listOf(EtteroppgjoerStatus.FERDIGSTILT, EtteroppgjoerStatus.OMGJOERING, EtteroppgjoerStatus.VENTER_PAA_SVAR)) {
            throw IllegalStateException(
                "Kan ikke tilbakestille etteroppgjoer med status $status",
            )
        }

        if (forbehandling.status !in
            listOf(EtteroppgjoerForbehandlingStatus.FERDIGSTILT, EtteroppgjoerForbehandlingStatus.AVBRUTT)
        ) {
            throw IllegalStateException(
                "Kan ikke tilbakestille forbehandling med status $status, ta kontakt for manuell håndtering.",
            )
        }
    }

    fun kanOppretteRevurdering() =
        status in listOf(EtteroppgjoerStatus.VENTER_PAA_SVAR, EtteroppgjoerStatus.FERDIGSTILT, EtteroppgjoerStatus.OMGJOERING)

    fun erFerdigstilt() = status == EtteroppgjoerStatus.FERDIGSTILT
}

enum class EtteroppgjoerStatus {
    VENTER_PAA_SKATTEOPPGJOER,
    MOTTATT_SKATTEOPPGJOER,
    VENTER_PAA_SVAR,

    UNDER_FORBEHANDLING,
    UNDER_REVURDERING,

    FERDIGSTILT,
    OMGJOERING, // midlertidig status for omgjøring av revurdering
}
