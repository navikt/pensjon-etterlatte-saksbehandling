package no.nav.etterlatte.statistikk.jobs

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.jobs.LoggerInfo
import no.nav.etterlatte.jobs.fixedRateCancellableTimer
import no.nav.etterlatte.libs.common.TimerJob
import no.nav.etterlatte.libs.jobs.LeaderElection
import no.nav.etterlatte.statistikk.clients.BehandlingKlient
import no.nav.etterlatte.statistikk.clients.KunneIkkeHenteFraBehandling
import no.nav.etterlatte.statistikk.database.RyddUtlandstilsnittDao
import no.nav.etterlatte.statistikk.domain.SakUtland
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.Timer

class RyddUtlandstilsnittJob(
    private val behandlingKlient: BehandlingKlient,
    private val ryddUtlandstilsnittDao: RyddUtlandstilsnittDao,
    private val leaderElection: LeaderElection,
    private val initialDelay: Long,
    private val periode: Duration,
) : TimerJob {
    private val logger = LoggerFactory.getLogger(RyddUtlandstilsnittJob::class.java)
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
                ryddUtlandstilsnittDao = ryddUtlandstilsnittDao,
                behandlingKlient = behandlingKlient,
            ).run()
        }
    }

    class FikseOppTing(
        private val leaderElection: LeaderElection,
        private val ryddUtlandstilsnittDao: RyddUtlandstilsnittDao,
        private val behandlingKlient: BehandlingKlient,
    ) {
        private val logger: Logger = LoggerFactory.getLogger(this::class.java)
        private val sakerAvGangen = 10
        private val antallKjoeringer = 10

        fun run() {
            if (!leaderElection.isLeader()) {
                return
            }

            repeat(antallKjoeringer) {
                hentManglendeUtlandstilknytning()
            }
            repeat(antallKjoeringer) {
                patchManglendeUtlandstilknytning()
            }
        }

        private fun hentManglendeUtlandstilknytning() {
            val behandlingerMedManglendeUtland = ryddUtlandstilsnittDao.hentBehandlingerMedManglendeUtlandstilsnitt(sakerAvGangen)
            behandlingerMedManglendeUtland.map {
                it to
                    try {
                        runBlocking { behandlingKlient.hentUtlandstilknytning(it) }
                    } catch (e: KunneIkkeHenteFraBehandling) {
                        logger.warn(
                            "Kunne ikke hente utlandstilknyting fra behandling for behandlingId=$it. " +
                                "Setter at vi ikke fikk hentet noe for behandlingen.",
                        )
                        null
                    }
            }.forEach { (behandlingId, utlandstilknytning) ->
                ryddUtlandstilsnittDao.lagreUtlandstilknytning(
                    behandlingId,
                    utlandstilknytning?.type,
                )
            }
        }

        private fun patchManglendeUtlandstilknytning() {
            val behandlingerSomManglerPatching = ryddUtlandstilsnittDao.hentBehandlingerSomIkkeErPatchet(sakerAvGangen)
            behandlingerSomManglerPatching.forEach { (behandlingId, utlandstilknytning) ->
                when (val statistikkUtlandstilknytning = SakUtland.fraUtlandstilknytningType(utlandstilknytning)) {
                    SakUtland.NASJONAL -> {
                        // Unødvendig å patche disse, siden de allerede er satt opp med NASJONAL
                        ryddUtlandstilsnittDao.lagrePatchetStatus(behandlingId, 0, 0)
                    }
                    else -> ryddUtlandstilsnittDao.patchRaderForBehandling(behandlingId, statistikkUtlandstilknytning)
                }
            }
        }
    }
}
