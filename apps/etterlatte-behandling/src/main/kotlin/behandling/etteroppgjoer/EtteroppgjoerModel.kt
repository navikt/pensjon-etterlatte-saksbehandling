package no.nav.etterlatte.behandling.etteroppgjoer

import no.nav.etterlatte.libs.common.sak.SakId

data class Etteroppgjoer(
    val sakId: SakId,
    val inntektsaar: Int,
    val status: EtteroppgjoerStatus,
    val harSanksjon: Boolean = false,
    val harInstitusjonsopphold: Boolean = false,
    val harOpphoer: Boolean = false,
    val harBosattUtland: Boolean = false,
    val harAdressebeskyttelse: Boolean = false,
    val harAktivitetskrav: Boolean = false,
)

enum class EtteroppgjoerStatus {
    AVBRUTT_FORBEHANDLING,
    VENTER_PAA_SKATTEOPPGJOER,
    MOTTATT_SKATTEOPPGJOER,
    UNDER_FORBEHANDLING,
    FERDIGSTILT_FORBEHANDLING,
    UNDER_REVURDERING,
    FERDIGSTILT_REVURDERING,
    FERDIGSTILT,
}
