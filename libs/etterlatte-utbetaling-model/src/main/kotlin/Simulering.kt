package no.nav.etterlatte.utbetaling.common

import no.nav.etterlatte.libs.common.Enhetsnummer
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import java.math.BigDecimal
import java.time.LocalDate

data class SimulertBeregning(
    val gjelderId: Folkeregisteridentifikator,
    val datoBeregnet: LocalDate,
    val infomelding: String? = null,
    val beloep: BigDecimal,
    val kommendeUtbetalinger: List<SimulertBeregningsperiode>,
    val etterbetaling: List<SimulertBeregningsperiode>,
    val tilbakekreving: List<SimulertBeregningsperiode>,
)

data class SimulertBeregningsperiode(
    val fom: LocalDate,
    val tom: LocalDate? = null,
    val gjelderId: Folkeregisteridentifikator,
    val utbetalesTilId: String,
    val forfall: LocalDate,
    val feilkonto: Boolean,
    val kodeFaggruppe: String,
    val enhet: Enhetsnummer,
    val konto: String,
    val behandlingskode: String,
    val beloep: BigDecimal,
    val tilbakefoering: Boolean,
    val klassekode: String,
    val klassekodeTekniskArt: Boolean,
    val klassekodeBeskrivelse: String,
    val klasseType: KlasseType,
)

enum class KlasseType(
    val beskrivelse: String,
) {
    YTEL("Ytelse"),
    SKAT("Forskuddsskatt"),
    FEIL("Feilutbetaling"),
    MOTP("Motpostering"),
    JUST("Justering"),
    TREK("Trekk"),
}
