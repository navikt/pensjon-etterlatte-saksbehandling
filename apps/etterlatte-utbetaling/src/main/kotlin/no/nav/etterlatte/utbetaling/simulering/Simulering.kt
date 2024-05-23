package no.nav.etterlatte.utbetaling.simulering

import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.system.os.entiteter.beregningskjema.Beregning
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class SimulertBeregning(
    val gjelderId: Folkeregisteridentifikator,
    val datoBeregnet: LocalDate,
    val beregningsperioder: List<SimulertBereningsperiode> = emptyList(),
)

data class SimulertBereningsperiode(
    val fom: LocalDate,
    val tom: LocalDate? = null,
    val fagsystemId: String,
    val gjelderId: Folkeregisteridentifikator,
    val utbetalesTilId: Folkeregisteridentifikator,
    val kodeFaggruppe: String,
    val enhet: String,
    val oppdragsId: Long,
    val delytelseId: String,
    val konto: String,
    val behandlingskode: String,
    val beloep: BigDecimal,
)

private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

fun Beregning.tilSimulertBeregning(): SimulertBeregning {
    val perioder = mutableListOf<SimulertBereningsperiode>()

    for (bp in this.beregningsPeriode) {
        for (stn in bp.beregningStoppnivaa) {
            for (det in stn.beregningStoppnivaaDetaljer) {
                perioder.add(
                    SimulertBereningsperiode(
                        fom = LocalDate.parse(bp.periodeFom, dateTimeFormatter),
                        tom = bp.periodeTom?.let { tom -> LocalDate.parse(tom, dateTimeFormatter) },
                        fagsystemId = stn.fagsystemId,
                        gjelderId = Folkeregisteridentifikator.of(this.gjelderId),
                        utbetalesTilId = Folkeregisteridentifikator.of(stn.utbetalesTilId),
                        kodeFaggruppe = this.kodeFaggruppe,
                        enhet = stn.behandlendeEnhet,
                        oppdragsId = stn.oppdragsId,
                        delytelseId = det.delytelseId,
                        konto = det.kontoStreng,
                        behandlingskode = det.behandlingskode,
                        beloep = det.belop,
                    ),
                )
            }
        }
    }

    return SimulertBeregning(
        gjelderId = Folkeregisteridentifikator.of(this.gjelderId),
        datoBeregnet = LocalDate.parse(this.datoBeregnet, dateTimeFormatter),
        beregningsperioder = perioder,
    )
}
