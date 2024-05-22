package no.nav.etterlatte.statistikk.jobs

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.jobs.LoggerInfo
import no.nav.etterlatte.jobs.fixedRateCancellableTimer
import no.nav.etterlatte.libs.common.TimerJob
import no.nav.etterlatte.libs.jobs.LeaderElection
import no.nav.etterlatte.statistikk.clients.BeregningKlient
import no.nav.etterlatte.statistikk.database.RefreshBeregningDao
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.Timer

class RefreshBeregningJob(
    private val beregningKlient: BeregningKlient,
    private val refreshBeregningDao: RefreshBeregningDao,
    private val leaderElection: LeaderElection,
    private val initialDelay: Long,
    private val periode: Duration,
) : TimerJob {
    private val logger = LoggerFactory.getLogger(RefreshBeregningJob::class.java)
    private val jobbNavn = this::class.simpleName

    override fun schedule(): Timer {
        logger.info("$jobbNavn er satt til å kjøre etter $initialDelay med periode $periode")

        return fixedRateCancellableTimer(
            name = jobbNavn,
            initialDelay = initialDelay,
            loggerInfo = LoggerInfo(logger = logger, loggTilSikkerLogg = false),
            period = periode.toMillis(),
        ) {
            FikseOppTing(
                leaderElection = leaderElection,
                refreshBeregningDao = refreshBeregningDao,
                beregningKlient = beregningKlient,
            ).run()
        }
    }

    class FikseOppTing(
        private val leaderElection: LeaderElection,
        private val refreshBeregningDao: RefreshBeregningDao,
        private val beregningKlient: BeregningKlient,
    ) {
        private val logger: Logger = LoggerFactory.getLogger(this::class.java)
        private val sakerAvGangen = 10
        private val antallKjoeringer = 10

        fun run() {
            if (!leaderElection.isLeader()) {
                return
            }

            repeat(antallKjoeringer) {
                hentNyBeregning()
            }
            repeat(antallKjoeringer) {
                oppdaterMedNyBeregning()
            }
        }

        private fun hentNyBeregning() {
            val behandlingerMedManglendeUtland =
                refreshBeregningDao.hentBehandlingerUtenOppdatertBeregning(sakerAvGangen)
            behandlingerMedManglendeUtland.map {
                it to
                    try {
                        runBlocking { beregningKlient.hentBeregningForBehandling(it) }
                    } catch (e: Exception) {
                        logger.warn(
                            "Kunne ikke hente beregning på nytt for for behandlingId=$it. " +
                                "Setter at vi ikke fikk hentet ny beregning.",
                        )
                        null
                    }
            }.forEach { (behandlingId, beregning) ->
                refreshBeregningDao.lagreBeregning(
                    behandlingId,
                    beregning,
                )
            }
        }

        private fun oppdaterMedNyBeregning() {
            val behandlingerSomManglerPatching = refreshBeregningDao.hentBehandlingerSomIkkeErRefreshet(sakerAvGangen)
            behandlingerSomManglerPatching.forEach { (behandlingId, beregning) ->
                if (beregning != null) {
                    refreshBeregningDao.patchRaderForBehandling(behandlingId, beregning)
                } else {
                    logger.warn("Hentet ut en refreshet beregning som ble null, behandlingId=$behandlingId")
                }
            }
        }
    }
}
