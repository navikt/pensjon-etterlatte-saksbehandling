package no.nav.etterlatte.behandling.etteroppgjoer

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

    fun kanOppretteRevurdering() = status in listOf(EtteroppgjoerStatus.VENTER_PAA_SVAR, EtteroppgjoerStatus.FERDIGSTILT)

    fun erFerdigstilt() = status == EtteroppgjoerStatus.FERDIGSTILT
}

enum class EtteroppgjoerStatus {
    VENTER_PAA_SKATTEOPPGJOER,
    MOTTATT_SKATTEOPPGJOER,
    VENTER_PAA_SVAR,

    UNDER_FORBEHANDLING,
    UNDER_REVURDERING,

    FERDIGSTILT,
}
