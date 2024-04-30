package no.nav.etterlatte.utbetaling.simulering

import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import no.nav.etterlatte.libs.common.tidspunkt.norskTidssone
import no.nav.etterlatte.libs.common.vedtak.VedtakInnholdDto
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import no.nav.etterlatte.utbetaling.VedtaksvurderingKlient
import no.nav.etterlatte.utbetaling.common.OppdragDefaults
import no.nav.etterlatte.utbetaling.iverksetting.oppdrag.tilKodeFagomraade
import no.nav.etterlatte.utbetaling.iverksetting.oppdrag.tilKodeklassifikasjon
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.Attestasjon
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.Utbetaling
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.UtbetalingDao
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.UtbetalingMapper
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.Utbetalingslinje
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.Utbetalingslinjetype
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.Utbetalingsvedtak
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.VedtakFattet
import no.nav.system.os.entiteter.oppdragskjema.Enhet
import no.nav.system.os.entiteter.typer.simpletypes.FradragTillegg
import no.nav.system.os.entiteter.typer.simpletypes.KodeStatusLinje
import no.nav.system.os.tjenester.simulerfpservice.simulerfpserviceservicetypes.Oppdrag
import no.nav.system.os.tjenester.simulerfpservice.simulerfpserviceservicetypes.Oppdragslinje
import no.nav.system.os.tjenester.simulerfpservice.simulerfpserviceservicetypes.SimulerBeregningRequest
import no.nav.system.os.tjenester.simulerfpservice.simulerfpserviceservicetypes.SimulerBeregningResponse
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.UUID

class SimuleringOsService(
    private val utbetalingDao: UtbetalingDao,
    private val vedtaksvurderingKlient: VedtaksvurderingKlient,
    private val simuleringOsKlient: SimuleringOsKlient,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    suspend fun simuler(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): SimulerBeregningResponse {
        val vedtak =
            vedtaksvurderingKlient.hentVedtak(behandlingId, brukerTokenInfo)

        val innhold = vedtak.innhold
        if (innhold is VedtakInnholdDto.VedtakBehandlingDto) {
            val sakensUtbetalinger = utbetalingDao.hentUtbetalinger(vedtak.sak.id)
            val utbetalingsvedtak =
                Utbetalingsvedtak.fra(
                    vedtak = vedtak,
                    vedtakFattet =
                        VedtakFattet(
                            ansvarligSaksbehandler = brukerTokenInfo.ident(),
                            ansvarligEnhet = OppdragDefaults.OPPDRAGSENHET.enhet,
                        ),
                    attestasjon =
                        Attestasjon(
                            attestant = OppdragDefaults.SAKSBEHANDLER_ID_SYSTEM_ETTERLATTEYTELSER,
                            attesterendeEnhet = OppdragDefaults.OPPDRAGSENHET.enhet,
                        ),
                )
            val utbetalingMapper =
                UtbetalingMapper(
                    tidligereUtbetalinger = sakensUtbetalinger,
                    vedtak = utbetalingsvedtak,
                )
            val opprettetUtbetaling = utbetalingMapper.opprettUtbetaling()
            val erFoersteUtbetalingPaaSak = utbetalingMapper.tidligereUtbetalinger.isEmpty()
            val vedtakVirkFom = innhold.virkningstidspunkt

            val request =
                mapTilSimuleringRequest(
                    opprettetUtbetaling,
                    erFoersteUtbetalingPaaSak,
                    vedtakVirkFom,
                    brukerTokenInfo,
                )

            return simuleringOsKlient.simuler(request).also {
                it.infomelding?.beskrMelding?.trim().let { melding -> logger.info(melding) }
            }
        } else {
            throw IkkeStoettetSimulering(behandlingId)
        }
    }

    private fun mapTilSimuleringRequest(
        utbetaling: Utbetaling,
        erFoersteUtbetalingPaaSak: Boolean,
        vedtakVirkFom: YearMonth,
        brukerTokenInfo: BrukerTokenInfo,
    ): SimulerBeregningRequest {
        val request =
            SimulerBeregningRequest().apply {
                oppdrag = tilOppdrag(utbetaling, erFoersteUtbetalingPaaSak, vedtakVirkFom, brukerTokenInfo)
                simuleringsPeriode = simuleringsperiode(vedtakVirkFom, utbetaling.utbetalingslinjer)
            }
        return request
    }

    private fun simuleringsperiode(
        vedtakVirkFom: YearMonth,
        utbetalingsperioder: List<Utbetalingslinje>,
    ) = SimulerBeregningRequest.SimuleringsPeriode().apply {
        datoSimulerFom = vedtakVirkFom.atDay(1).toOppdragDate()
        datoSimulerTom = utbetalingsperioder.lastOrNull()?.periode?.til?.toOppdragDate()
    }

    private fun tilOppdrag(
        utbetaling: Utbetaling,
        erFoersteUtbetalingPaaSak: Boolean,
        vedtakVirkFom: YearMonth,
        brukerTokenInfo: BrukerTokenInfo,
    ): Oppdrag {
        return Oppdrag().apply {
            fagsystemId = utbetaling.sakId.value.toString()
            oppdragGjelderId = utbetaling.stoenadsmottaker.value
            saksbehId = brukerTokenInfo.ident()

            utbetFrekvens = OppdragDefaults.UTBETALINGSFREKVENS
            kodeEndring = if (erFoersteUtbetalingPaaSak) "NY" else "ENDR"
            kodeFagomraade = utbetaling.sakType.tilKodeFagomraade()

            datoOppdragGjelderFom = vedtakVirkFom.atDay(1).toOppdragDate()
            oppdragslinje.addAll(
                utbetaling.utbetalingslinjer.map {
                    tilOppdragsLinje(
                        utbetaling,
                        it,
                        brukerTokenInfo,
                    )
                },
            )
            enhet.add(
                Enhet().apply {
                    typeEnhet = OppdragDefaults.OPPDRAGSENHET.typeEnhet
                    enhet = OppdragDefaults.OPPDRAGSENHET.enhet
                    datoEnhetFom = OppdragDefaults.OPPDRAGSENHET_DATO_FOM.toOppdragDate()
                },
            )
        }
    }

    private fun tilOppdragsLinje(
        utbetaling: Utbetaling,
        utbetalingslinje: Utbetalingslinje,
        brukerTokenInfo: BrukerTokenInfo,
    ): Oppdragslinje =
        Oppdragslinje().apply {
            vedtakId = utbetaling.vedtak.vedtakId.toString()
            delytelseId = utbetalingslinje.id.value.toString()
            datoVedtakFom = utbetalingslinje.periode.fra.toOppdragDate()
            datoVedtakTom = utbetalingslinje.periode.til?.toOppdragDate()
            utbetalesTilId = utbetaling.stoenadsmottaker.value
            henvisning = utbetaling.behandlingId.shortValue.value
            saksbehId = brukerTokenInfo.ident()

            kodeEndringLinje = "NY"
            kodeKlassifik = utbetaling.sakType.tilKodeklassifikasjon()
            sats = utbetalingslinje.beloep
            typeSats = "MND"
            fradragTillegg = FradragTillegg.T
            brukKjoreplan = "N"

            if (utbetalingslinje.type == Utbetalingslinjetype.OPPHOER) {
                kodeStatusLinje = KodeStatusLinje.OPPH
                datoStatusFom = utbetalingslinje.periode.fra.toOppdragDate()
            }

            if (utbetalingslinje.erstatterId != null) {
                refFagsystemId = utbetaling.sakId.value.toString()
                refDelytelseId = utbetalingslinje.erstatterId.value.toString()
            }
        }
}

private fun LocalDate.toOppdragDate(): String =
    DateTimeFormatter.ofPattern("yyyy-MM-dd")
        .withZone(norskTidssone).format(this)

class IkkeStoettetSimulering(behandlingId: UUID) : UgyldigForespoerselException(
    code = "SIMULERING_IKKE_STOETTET",
    detail = "Kan ikke simulere for behandlingId=$behandlingId",
    meta = mapOf("behandlingId" to behandlingId),
)
