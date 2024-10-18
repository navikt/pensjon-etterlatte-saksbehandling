package no.nav.etterlatte.utbetaling.simulering

import no.nav.etterlatte.libs.common.Enhetsnummer
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.utbetaling.common.KlasseType
import no.nav.etterlatte.utbetaling.common.SimulertBeregning
import no.nav.etterlatte.utbetaling.common.SimulertBeregningsperiode
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.OppdragKlassifikasjonskode
import no.nav.system.os.entiteter.beregningskjema.Beregning
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

fun Beregning.tilSimulertBeregning(infomelding: String?): SimulertBeregning {
    val simulertPaaDato = LocalDate.parse(this.datoBeregnet, dateTimeFormatter)
    val perioder = mutableListOf<SimulertBeregningsperiode>()

    for (bp in this.beregningsPeriode) {
        for (stn in bp.beregningStoppnivaa) {
            for (det in stn.beregningStoppnivaaDetaljer) {
                perioder.add(
                    SimulertBeregningsperiode(
                        fom = LocalDate.parse(bp.periodeFom, dateTimeFormatter),
                        tom = bp.periodeTom?.let { tom -> LocalDate.parse(tom, dateTimeFormatter) },
                        gjelderId = Folkeregisteridentifikator.of(this.gjelderId),
                        forfall = LocalDate.parse(stn.forfall, dateTimeFormatter),
                        utbetalesTilId = stn.utbetalesTilId,
                        feilkonto = stn.isFeilkonto,
                        kodeFaggruppe = this.kodeFaggruppe,
                        enhet = Enhetsnummer(stn.behandlendeEnhet),
                        konto = det.kontoStreng,
                        behandlingskode = det.behandlingskode,
                        beloep = det.belop,
                        tilbakefoering = det.isTilbakeforing,
                        klassekode = OppdragKlassifikasjonskode.fraString(det.klassekode).toString(),
                        klassekodeBeskrivelse = det.klasseKodeBeskrivelse,
                        klasseType = KlasseType.valueOf(det.typeKlasse),
                    ),
                )
            }
        }
    }

    // Slår sammen linjer tilbake i tid av samme klassifikasjon og summerer beløp
    val etterbetalinger =
        perioder
            .asSequence()
            .filter { !it.forfall.isAfter(simulertPaaDato) }
            .filter { !OppdragKlassifikasjonskode.fraString(it.klassekode).tekniskArt }
            .filter { it.klasseType != KlasseType.FEIL }
            .toList()

    val tilbakekreving =
        perioder
            .asSequence()
            .filter { !it.forfall.isAfter(simulertPaaDato) }
            .filter { !OppdragKlassifikasjonskode.fraString(it.klassekode).tekniskArt }
            .filter { it.klasseType == KlasseType.FEIL }
            .toList()

    val kommendeUtbetalinger = perioder.filter { it.forfall.isAfter(simulertPaaDato) }

    return SimulertBeregning(
        gjelderId = Folkeregisteridentifikator.of(this.gjelderId),
        datoBeregnet = LocalDate.parse(this.datoBeregnet, dateTimeFormatter),
        infomelding = infomelding,
        beloep = kommendeUtbetalinger.sumOf { it.beloep },
        kommendeUtbetalinger = kommendeUtbetalinger,
        etterbetaling = etterbetalinger,
        tilbakekreving = tilbakekreving,
    )
}
