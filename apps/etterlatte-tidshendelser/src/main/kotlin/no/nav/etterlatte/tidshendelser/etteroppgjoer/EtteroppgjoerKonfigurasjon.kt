package no.nav.etterlatte.tidshendelser.etteroppgjoer

import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.libs.common.Enhetsnummer
import no.nav.etterlatte.libs.common.sak.SakId
import java.time.LocalDate

enum class EtteroppgjoerFilter {
    ENKEL,
}

data class EtteroppgjoerKonfigurasjon(
    val inntektsaar: Int,
    val antall: Int,
    val dato: LocalDate,
    val etteroppgjoerFilter: EtteroppgjoerFilter,
    val spesifikkeSaker: List<SakId>,
    val ekskluderteSaker: List<SakId>,
    val spesifikkeEnheter: List<String>,
    val kjoeringId: String? = null,
)
