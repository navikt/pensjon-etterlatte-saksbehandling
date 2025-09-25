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
    val harOverstyrtBeregning: Boolean = false,
    val sisteFerdigstilteForbehandling: UUID? = null,
)

enum class EtteroppgjoerStatus {
    VENTER_PAA_SKATTEOPPGJOER,
    MOTTATT_SKATTEOPPGJOER,

    UNDER_FORBEHANDLING,
    VENTER_PAA_SVAR,
    UNDER_REVURDERING,

    FERDIGSTILT,
    ;

    companion object {
        // TODO: må være strengere her når vi går i prod
        val KLAR_TIL_FORBEHANDLING = setOf(VENTER_PAA_SKATTEOPPGJOER, MOTTATT_SKATTEOPPGJOER)
    }
}
