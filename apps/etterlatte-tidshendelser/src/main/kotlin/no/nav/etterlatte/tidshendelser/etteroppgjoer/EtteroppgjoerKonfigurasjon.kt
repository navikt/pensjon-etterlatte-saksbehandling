package no.nav.etterlatte.tidshendelser.etteroppgjoer

import no.nav.etterlatte.libs.common.sak.SakId
import java.time.LocalDate

enum class EtteroppgjoerFilter(
    val harSanksjon: Boolean,
    val harInsitusjonsopphold: Boolean,
    val harOpphoer: Boolean,
    val harAdressebeskyttelseEllerSkjermet: Boolean,
    val harAktivitetskrav: Boolean,
    val harBosattUtland: Boolean,
) {
    ENKEL(false, false, false, false, false, false),
}

data class EtteroppgjoerKonfigurasjon(
    val antall: Int,
    val dato: LocalDate,
    val etteroppgjoerFilter: EtteroppgjoerFilter,
    val spesifikkeSaker: List<SakId>,
    val ekskluderteSaker: List<SakId>,
    val kjoeringId: String? = null,
)
