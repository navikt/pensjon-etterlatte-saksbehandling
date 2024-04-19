package no.nav.etterlatte.vedtaksvurdering.simulering

import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import no.nav.etterlatte.libs.common.tidspunkt.norskTidssone
import no.nav.etterlatte.libs.common.toUUID30
import no.nav.etterlatte.libs.common.vedtak.Utbetalingsperiode
import no.nav.etterlatte.libs.common.vedtak.UtbetalingsperiodeType
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import no.nav.etterlatte.vedtaksvurdering.Vedtak
import no.nav.etterlatte.vedtaksvurdering.VedtakBehandlingService
import no.nav.etterlatte.vedtaksvurdering.VedtakInnhold
import no.nav.etterlatte.vedtaksvurdering.VedtaksvurderingService
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
    private val vedtaksvurderingService: VedtaksvurderingService,
    private val vedtakBehandlingService: VedtakBehandlingService,
    private val simuleringOsKlient: SimuleringOsKlient,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    suspend fun simuler(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): SimulerBeregningResponse {
        val vedtak =
            vedtaksvurderingService.hentVedtakMedBehandlingId(behandlingId)
                ?: vedtakBehandlingService.opprettEllerOppdaterVedtak(behandlingId, brukerTokenInfo)

        if (vedtak.innhold is VedtakInnhold.Behandling) {
            val request = mapTilSimuleringRequest(vedtak, brukerTokenInfo)

            return simuleringOsKlient.simuler(request).also {
                logger.info(it.infomelding.beskrMelding.trim())
            }
        } else {
            throw IkkeStoettetSimulering(vedtak.type, behandlingId)
        }
    }

    private fun mapTilSimuleringRequest(
        vedtak: Vedtak,
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
        vedtak: Vedtak,
        brukerTokenInfo: BrukerTokenInfo,
    ): Oppdrag {
        return Oppdrag().apply {
            fagsystemId = vedtak.sakId.toString()
            oppdragGjelderId = vedtak.soeker.value
            saksbehId = brukerTokenInfo.ident()

            utbetFrekvens = "MND"
            kodeEndring = vedtak.type.toKodeEndring()
            kodeFagomraade = vedtak.sakType.toKodeFagomrade()

            if (vedtak.innhold is VedtakInnhold.Behandling) {
                datoOppdragGjelderFom = vedtak.innhold.virkningstidspunkt.atDay(1).toOppdragDate()
                oppdragslinje.addAll(
                    vedtak.innhold.utbetalingsperioder.map {
                        tilOppdragsLinje(
                            it,
                            vedtak,
                            brukerTokenInfo,
                        )
                    },
                )
            }
        }
    }

    private fun tilOppdragsLinje(
        up: Utbetalingsperiode,
        vedtak: Vedtak,
        brukerTokenInfo: BrukerTokenInfo,
    ): Oppdragslinje =
        Oppdragslinje().apply {
            vedtakId = vedtak.id.toString()
            delytelseId = up.id.toString()
            datoVedtakFom = up.periode.fom.atDay(1).toOppdragDate()
            datoVedtakTom = up.periode.tom?.atEndOfMonth()?.toOppdragDate()
            utbetalesTilId = vedtak.soeker.value
            henvisning = vedtak.behandlingId.toUUID30().value
            saksbehId = brukerTokenInfo.ident()

            kodeEndringLinje = vedtak.type.toKodeEndring()
            kodeKlassifik = vedtak.sakType.toKodeKlassifikasjon()
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

private val Vedtak.virkningstidspunkt: YearMonth
    get() = (this.innhold as VedtakInnhold.Behandling).virkningstidspunkt

private fun LocalDate.toOppdragDate(): String =
    DateTimeFormatter.ofPattern("yyyy-MM-dd")
        .withZone(norskTidssone).format(this)

class IkkeStoettetSimulering(vedtakType: VedtakType, behandlingId: UUID) : UgyldigForespoerselException(
    code = "SIMULERING_IKKE_STOETTET",
    detail = "Kan ikke simulere for vedtak av type $vedtakType",
    meta = mapOf("behandlingId" to behandlingId),
)
