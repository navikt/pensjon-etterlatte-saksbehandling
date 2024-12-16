package no.nav.etterlatte.statistikk.jobs

import no.nav.etterlatte.jobs.LoggerInfo
import no.nav.etterlatte.jobs.fixedRateCancellableTimer
import no.nav.etterlatte.libs.common.TimerJob
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import no.nav.etterlatte.libs.common.vedtak.VedtakKafkaHendelseHendelseType
import no.nav.etterlatte.libs.jobs.LeaderElection
import no.nav.etterlatte.libs.ktor.token.HardkodaSystembruker
import no.nav.etterlatte.statistikk.clients.VedtakKlient
import no.nav.etterlatte.statistikk.database.RyddVedtakResultatDao
import no.nav.etterlatte.statistikk.service.behandlingResultatFraVedtak
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.temporal.ChronoUnit
import java.util.Timer

class RyddVedtakResultatJob(
    private val dao: RyddVedtakResultatDao,
    private val vedtakKlient: VedtakKlient,
    private val leaderElection: LeaderElection,
) : TimerJob {
    private val logger = LoggerFactory.getLogger(this::class.java)
    private val jobbNavn = this::class.simpleName
    private val periode = Duration.of(5, ChronoUnit.MINUTES)
    private val initialDelay = Duration.of(2, ChronoUnit.MINUTES).toMillis()

    override fun schedule(): Timer {
        logger.info("$jobbNavn er satt til å kjøre med periode $periode etter $initialDelay ms")

        return fixedRateCancellableTimer(
            name = jobbNavn,
            period = periode.toMillis(),
            initialDelay = initialDelay,
            loggerInfo = LoggerInfo(logger = logger, loggTilSikkerLogg = false),
        ) {
            OppdaterResultatSakRad(
                leaderElection = leaderElection,
                dao = dao,
                klient = vedtakKlient,
            ).run()
        }
    }

    class OppdaterResultatSakRad(
        val leaderElection: LeaderElection,
        val dao: RyddVedtakResultatDao,
        val klient: VedtakKlient,
    ) {
        private val logger = LoggerFactory.getLogger(this::class.java)

        fun run() {
            if (!leaderElection.isLeader()) {
                logger.info("Er ikke leader, kjører ikke oppdater sak resultat jobb")
                return
            }

            try {
                val saker = dao.hentRaderMedPotensiellFeil().groupBy { it.behandlingId }
                saker.entries.forEach { (behandlingId, rader) ->
                    val vedtak =
                        try {
                            klient.hentVedtak(behandlingId, HardkodaSystembruker.statistikk)
                        } catch (e: Exception) {
                            logger.warn(
                                "Feilet i henting / oppdatering av behandling resultat " +
                                    "for vedtak til behandling med id = $behandlingId",
                            )
                            return@forEach
                        }
                    val resultat =
                        behandlingResultatFraVedtak(
                            vedtak,
                            VedtakKafkaHendelseHendelseType.ATTESTERT,
                            behandligStatus = BehandlingStatus.ATTESTERT,
                        ) ?: throw InternfeilException(
                            "Fikk ikke utledet resultat fra vedtak til " +
                                "behandling med id = $behandlingId",
                        )
                    rader.forEach {
                        try {
                            dao.oppdaterResultat(it, resultat)
                        } catch (e: Exception) {
                            logger.warn(
                                "Kunne ikke oppdatere resulatet for sak rad med id = ${it.id} for " +
                                    "behandling med id = $behandlingId",
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                logger.warn("Feilet i uthenting av rader med potensiell feil", e)
            }
        }
    }
}
