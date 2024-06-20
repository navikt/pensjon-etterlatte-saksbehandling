package no.nav.etterlatte.statistikk.jobs

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.jobs.LoggerInfo
import no.nav.etterlatte.jobs.fixedRateCancellableTimer
import no.nav.etterlatte.libs.common.TimerJob
import no.nav.etterlatte.libs.common.vedtak.VedtakInnholdDto
import no.nav.etterlatte.libs.jobs.LeaderElection
import no.nav.etterlatte.statistikk.clients.VedtaksvurderingsKlient
import no.nav.etterlatte.statistikk.database.OppdaterVedtakRepo
import no.nav.etterlatte.statistikk.domain.tilStoenadUtbetalingsperiode
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.Timer

class VedtakOppdateringJob(
    private val oppdaterVedtakRepo: OppdaterVedtakRepo,
    private val vedtakKlient: VedtaksvurderingsKlient,
    private val leaderElection: LeaderElection,
    private val initialDelay: Long,
    private val periode: Duration,
) : TimerJob {
    private val logger = LoggerFactory.getLogger(this::class.java)
    private val jobbNavn = this::class.simpleName

    override fun schedule(): Timer {
        logger.info("$jobbNavn er satt til å kjøre med periode $periode")

        return fixedRateCancellableTimer(
            name = jobbNavn,
            initialDelay = initialDelay,
            loggerInfo = LoggerInfo(logger = logger, loggTilSikkerLogg = false),
            period = periode.toMillis(),
        ) {
            HentOgOppdaterVedtak(
                leaderElection = leaderElection,
                oppdaterVedtakRepo = oppdaterVedtakRepo,
                vedtakKlient = vedtakKlient,
            ).run()
        }
    }

    class HentOgOppdaterVedtak(
        private val leaderElection: LeaderElection,
        private val oppdaterVedtakRepo: OppdaterVedtakRepo,
        private val vedtakKlient: VedtaksvurderingsKlient,
    ) {
        private val logger: Logger = LoggerFactory.getLogger(HentOgOppdaterVedtak::class.java)

        fun run() {
            if (leaderElection.isLeader()) {
                hentOgLagreOppdatertVedtak(200)
                lagrePatchetStatus(200)
            }
        }

        private fun lagrePatchetStatus(limit: Int) {
            val vedtak = oppdaterVedtakRepo.vedtakSomIkkeErPatchet(limit)
            vedtak.forEach {
                try {
                    oppdaterVedtakRepo.patchVedtak(it)
                } catch (e: Exception) {
                    logger.info("kunne ikke patche vedtak for behandlingId = ${it.behandlingId}")
                }
            }
        }

        private fun hentOgLagreOppdatertVedtak(limit: Int) {
            val behandlingIder = oppdaterVedtakRepo.vedtakSomIkkeErHentet(limit)
            behandlingIder.forEach { behandlingId ->
                try {
                    val vedtak = runBlocking { vedtakKlient.hentVedtak(behandlingId.toString()) }
                    if (vedtak == null) {
                        oppdaterVedtakRepo.oppdaterIkkeFunnetVedtak(behandlingId)
                    } else {
                        val vedtakInnhold = vedtak.innhold as VedtakInnholdDto.VedtakBehandlingDto
                        val perioder =
                            vedtakInnhold
                                .utbetalingsperioder
                                .map {
                                    tilStoenadUtbetalingsperiode(it)
                                }
                        oppdaterVedtakRepo.oppdaterHentetVedtak(
                            behandlingId,
                            vedtak.id.toInt(),
                            vedtakInnhold.opphoerFraOgMed,
                            perioder,
                        )
                    }
                } catch (e: Exception) {
                    logger.info("Kunne ikke hente vedtakDto for behandling: $behandlingId", e)
                    oppdaterVedtakRepo.oppdaterIkkeFunnetVedtak(behandlingId)
                }
            }
        }
    }
}
