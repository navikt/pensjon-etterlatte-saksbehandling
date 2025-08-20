package no.nav.etterlatte.behandling.etteroppgjoer

import no.nav.etterlatte.libs.common.sak.SakId

data class Etteroppgjoer(
    val sakId: SakId,
    val inntektsaar: Int,
    val status: EtteroppgjoerStatus,
    val harSanksjon: Boolean,
    val harInstitusjonsEllerFengselsopphold: Boolean,
    val harOpphoer: Boolean,
    val harBosattUtland: Boolean,
) {
    fun skalHaEtteroppgjoer(): Boolean =
        status == EtteroppgjoerStatus.VENTER_PAA_SKATTEOPPGJOER ||
            status == EtteroppgjoerStatus.MOTTATT_SKATTEOPPGJOER
}

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
