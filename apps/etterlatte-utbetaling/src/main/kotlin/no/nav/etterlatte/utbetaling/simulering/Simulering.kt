package no.nav.etterlatte.utbetaling.simulering

import no.nav.etterlatte.libs.common.Enhetsnummer
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.utbetaling.common.KlasseType
import no.nav.etterlatte.utbetaling.common.SimulertBeregning
import no.nav.etterlatte.utbetaling.common.SimulertBeregningOppsummering
import no.nav.etterlatte.utbetaling.common.SimulertBeregningOppsummeringer
import no.nav.etterlatte.utbetaling.common.SimulertBeregningPerAar
import no.nav.etterlatte.utbetaling.common.SimulertBeregningsperiode
import no.nav.etterlatte.utbetaling.common.SimulertEtterbetalingOppsummering
import no.nav.etterlatte.utbetaling.common.SimulertFeilutbetalingOppsummering
import no.nav.etterlatte.utbetaling.common.SimulertKlasseTypeOppsummering
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
                        klassekode = det.klassekode,
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
            .filter { it.klassekode != OppdragKlassifikasjonskode.MOTPOSTERING.oppdragVerdi } // filterer på det som ikke er av tekniskArt
            .filter { it.klasseType != KlasseType.FEIL }
            .toList()

    val tilbakekreving =
        perioder
            .asSequence()
            .filter { !it.forfall.isAfter(simulertPaaDato) }
            .filter { it.klassekode != OppdragKlassifikasjonskode.MOTPOSTERING.oppdragVerdi } // filterer på det som ikke er av tekniskArt
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
        oppsummeringer = oppsummerSimulering(etterbetalinger, tilbakekreving),
    )
}

internal fun oppsummerSimulering(
    etterbetaling: List<SimulertBeregningsperiode>,
    tilbakekreving: List<SimulertBeregningsperiode>,
): SimulertBeregningOppsummeringer {
    val aarstall =
        (etterbetaling + tilbakekreving)
            .map { it.fom.year }
            .distinct()
            .sortedDescending()

    val oppsummeringPerAar =
        aarstall.map { aar ->
            val etterbetalingForAar = etterbetaling.filter { it.fom.year == aar }
            val tilbakekrevingForAar = tilbakekreving.filter { it.fom.year == aar }

            SimulertBeregningPerAar(
                aarstall = aar,
                oppsummering = oppsummerPerioder(etterbetalingForAar, tilbakekrevingForAar),
            )
        }

    return SimulertBeregningOppsummeringer(
        perAar = oppsummeringPerAar,
        forPerioden = oppsummerPerioder(etterbetaling, tilbakekreving),
    )
}

private fun oppsummerPerioder(
    etterbetaling: List<SimulertBeregningsperiode>,
    tilbakekreving: List<SimulertBeregningsperiode>,
) = SimulertBeregningOppsummering(
    etterbetaling = summerEtterbetaling(etterbetaling),
    feilutbetaling = tilbakekreving.takeIf { it.isNotEmpty() }?.let { summerFeilutbetaling(etterbetaling, it) },
)

private fun summerEtterbetaling(perioder: List<SimulertBeregningsperiode>): SimulertEtterbetalingOppsummering {
    val brutto = perioder.filter { it.klasseType == KlasseType.YTEL }.sumOf { it.beloep }
    val beloepPerKlasseType =
        perioder
            .groupBy { it.klasseType }
            .map { (klasseType, linjer) ->
                SimulertKlasseTypeOppsummering(
                    klasseType = klasseType,
                    beloep = linjer.sumOf { it.beloep },
                )
            }.sortedBy { it.klasseType.ordinal }
    val netto = perioder.sumOf { it.beloep }

    return SimulertEtterbetalingOppsummering(
        brutto = brutto,
        beloepPerKlasseType = beloepPerKlasseType,
        netto = netto,
    )
}

private fun summerFeilutbetaling(
    etterbetaling: List<SimulertBeregningsperiode>,
    tilbakekreving: List<SimulertBeregningsperiode>,
): SimulertFeilutbetalingOppsummering {
    val netto = tilbakekreving.sumOf { it.beloep }
    val brutto =
        etterbetaling
            .filter { it.tilbakefoering && it.klasseType == KlasseType.YTEL }
            .sumOf { it.beloep }
            .abs()

    return SimulertFeilutbetalingOppsummering(
        brutto = brutto,
        beloepBrukerenSkulleHatt = brutto - netto,
        netto = netto,
    )
}
