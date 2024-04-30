package no.nav.etterlatte.utbetaling.simulering

import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import no.nav.etterlatte.libs.common.tidspunkt.norskTidssone
import no.nav.etterlatte.libs.common.toUUID30
import no.nav.etterlatte.libs.common.vedtak.Utbetalingsperiode
import no.nav.etterlatte.libs.common.vedtak.UtbetalingsperiodeType
import no.nav.etterlatte.libs.common.vedtak.VedtakDto
import no.nav.etterlatte.libs.common.vedtak.VedtakInnholdDto
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import no.nav.etterlatte.utbetaling.VedtaksvurderingKlient
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

        if (vedtak.innhold is VedtakInnholdDto.VedtakBehandlingDto) {
            val request = mapTilSimuleringRequest(vedtak, brukerTokenInfo)

            return simuleringOsKlient.simuler(request).also {
                it.infomelding?.beskrMelding?.trim().let { melding -> logger.info(melding) }
            }
        } else {
            throw IkkeStoettetSimulering(vedtak.type, behandlingId)
        }
    }

    private fun mapTilSimuleringRequest(
        vedtak: VedtakDto,
        brukerTokenInfo: BrukerTokenInfo,
    ): SimulerBeregningRequest {
        val request =
            SimulerBeregningRequest().apply {
                oppdrag = tilOppdrag(vedtak, brukerTokenInfo)
                simuleringsPeriode = simuleringsperiode(vedtak.virkningstidspunkt)
            }
        return request
    }

    private fun simuleringsperiode(virkningstidspunkt: YearMonth) =
        SimulerBeregningRequest.SimuleringsPeriode().apply {
            datoSimulerFom = virkningstidspunkt.atDay(1).toOppdragDate()
        }

    private fun tilOppdrag(
        vedtak: VedtakDto,
        brukerTokenInfo: BrukerTokenInfo,
    ): Oppdrag {
        return Oppdrag().apply {
            fagsystemId = vedtak.sak.id.toString()
            oppdragGjelderId = vedtak.sak.ident
            saksbehId = brukerTokenInfo.ident()

            utbetFrekvens = "MND"
            kodeEndring = vedtak.type.toKodeEndring()
            kodeFagomraade = vedtak.sak.sakType.toKodeFagomrade()

            val innhold = vedtak.innhold
            if (innhold is VedtakInnholdDto.VedtakBehandlingDto) {
                datoOppdragGjelderFom = innhold.virkningstidspunkt.atDay(1).toOppdragDate()
                oppdragslinje.addAll(
                    innhold.utbetalingsperioder.map {
                        tilOppdragsLinje(
                            it,
                            vedtak,
                            brukerTokenInfo,
                        )
                    },
                )
                enhet.add(
                    Enhet().apply {
                        typeEnhet = "BOS"
                        enhet = "4819"
                        datoEnhetFom = LocalDate.parse("1900-01-01").toOppdragDate()
                    },
                )
            }
        }
    }

    private fun tilOppdragsLinje(
        up: Utbetalingsperiode,
        vedtak: VedtakDto,
        brukerTokenInfo: BrukerTokenInfo,
    ): Oppdragslinje =
        Oppdragslinje().apply {
            vedtakId = vedtak.id.toString()
            delytelseId = up.id.toString()
            datoVedtakFom = up.periode.fom.atDay(1).toOppdragDate()
            datoVedtakTom = up.periode.tom?.atEndOfMonth()?.toOppdragDate()
            utbetalesTilId = vedtak.sak.ident
            henvisning = vedtak.behandlingId.toUUID30().value
            saksbehId = brukerTokenInfo.ident()

            kodeEndringLinje = vedtak.type.toKodeEndring()
            kodeKlassifik = vedtak.sak.sakType.toKodeKlassifikasjon()
            sats = up.beloep
            typeSats = "MND"
            fradragTillegg = FradragTillegg.T
            brukKjoreplan = "N"

            if (up.type == UtbetalingsperiodeType.OPPHOER) {
                kodeStatusLinje = KodeStatusLinje.OPPH
                datoStatusFom = up.periode.fom.atDay(1).toOppdragDate()
            }
        }

    private fun SakType.toKodeFagomrade() =
        when (this) {
            SakType.BARNEPENSJON -> "BARNEPE"
            SakType.OMSTILLINGSSTOENAD -> "OMSTILL"
        }

    private fun SakType.toKodeKlassifikasjon() =
        when (this) {
            SakType.BARNEPENSJON -> "BARNEPENSJON-OPTP"
            SakType.OMSTILLINGSSTOENAD -> "OMSTILLINGOR"
        }

    private fun VedtakType.toKodeEndring() =
        when (this) {
            VedtakType.INNVILGELSE,
            VedtakType.AVSLAG,
            -> "NY"
            else -> "ENDR"
        }
}

private val VedtakDto.virkningstidspunkt: YearMonth
    get() = (this.innhold as VedtakInnholdDto.VedtakBehandlingDto).virkningstidspunkt

private fun LocalDate.toOppdragDate(): String =
    DateTimeFormatter.ofPattern("yyyy-MM-dd")
        .withZone(norskTidssone).format(this)

class IkkeStoettetSimulering(vedtakType: VedtakType, behandlingId: UUID) : UgyldigForespoerselException(
    code = "SIMULERING_IKKE_STOETTET",
    detail = "Kan ikke simulere for vedtak av type $vedtakType",
    meta = mapOf("behandlingId" to behandlingId),
)
