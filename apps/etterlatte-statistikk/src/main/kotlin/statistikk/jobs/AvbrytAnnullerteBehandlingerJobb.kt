package no.nav.etterlatte.statistikk.jobs

import no.nav.etterlatte.jobs.LoggerInfo
import no.nav.etterlatte.jobs.fixedRateCancellableTimer
import no.nav.etterlatte.libs.common.TimerJob
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.feilhaandtering.krev
import no.nav.etterlatte.libs.database.toList
import no.nav.etterlatte.libs.jobs.LeaderElection
import no.nav.etterlatte.statistikk.database.SakRepository
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.temporal.ChronoUnit
import java.util.Timer
import java.util.UUID
import javax.sql.DataSource

class AnnulerteDao(
    private val datasource: DataSource,
) {
    fun hentIkkeAnnullerteBehandlinger(): List<UUID> =
        datasource.connection.use {
            val statement =
                it.prepareStatement(
                    """
                    SELECT behandling_id FROM tilbakestilte_behandlinger WHERE ryddet IS FALSE LIMIT 10
                    """.trimIndent(),
                )

            statement
                .executeQuery()
                .toList { getObject("behandling_id") as UUID }
        }

    fun lagreFikset(behandlingId: UUID) {
        datasource.connection.use {
            val statement =
                it.prepareStatement(
                    """
                    UPDATE tilbakestilte_behandlinger SET ryddet = TRUE WHERE behandling_id = ?
                    """.trimIndent(),
                )
            statement.setObject(1, behandlingId)
            val oppdatert = statement.executeUpdate()
            krev(oppdatert == 1) {
                "Kunne ikke oppdatere ryddet status for behandling med id=$behandlingId"
            }
        }
    }
}

class AvbrytAnnullerteBehandlingerJobb(
    private val sakRepository: SakRepository,
    private val annulerteDao: AnnulerteDao,
    private val leaderElection: LeaderElection,
) : TimerJob {
    private val logger: Logger = LoggerFactory.getLogger(AvbrytAnnullerteBehandlingerJobb::class.java)
    private val initialDelay = Duration.of(2, ChronoUnit.MINUTES)
    private val period = Duration.of(5, ChronoUnit.MINUTES)
    private val jobbNavn = this::class.simpleName

    override fun schedule(): Timer {
        logger.info("Jobb $jobbNavn startet med initialdelay $initialDelay og periode $period")

        return fixedRateCancellableTimer(
            name = null,
            initialDelay = initialDelay.toMillis(),
            period = period.toMillis(),
            loggerInfo = LoggerInfo(logger = logger, sikkerLogg = null, loggTilSikkerLogg = false),
        ) {
            AvbrytAnnullertBehandling(
                sakRepository = sakRepository,
                annulerteDao = annulerteDao,
                leaderElection = leaderElection,
            ).avbrytBehandlinger()
        }
    }

    class AvbrytAnnullertBehandling(
        private val sakRepository: SakRepository,
        private val annulerteDao: AnnulerteDao,
        private val leaderElection: LeaderElection,
    ) {
        private val logger: Logger = LoggerFactory.getLogger(AvbrytAnnullertBehandling::class.java)

        fun avbrytBehandlinger() {
            if (!leaderElection.isLeader()) {
                return
            }
            try {
                annulerteDao
                    .hentIkkeAnnullerteBehandlinger()
                    .forEach {
                        try {
                            val rader = sakRepository.hentRaderForBehandlingId(it)
                            val aktuellRad = rader.singleOrNull()
                            if (aktuellRad == null) {
                                logger.warn(
                                    "Kunne ikke avbryte behandling som er rullet tilbake i behandling, siden vi har " +
                                        "${rader.size} rader registrert for id=$it.",
                                )
                                annulerteDao.lagreFikset(it)
                                return@forEach
                            }
                            aktuellRad.copy(
                                status = BehandlingStatus.AVBRUTT.name,
                                resultat = BehandlingStatus.AVBRUTT.name,
                                resultatBegrunnelse = "BEHANDLING_RULLET_TILBAKE",
                                ferdigbehandletTidspunkt = aktuellRad.tekniskTid,
                            )
                            sakRepository.lagreRad(aktuellRad)
                            annulerteDao.lagreFikset(it)
                        } catch (e: Exception) {
                            logger.warn(
                                "Kunne ikke avbryte behandlingen som er rullet tilbake i behandling med id=$it",
                                e,
                            )
                        }
                    }
            } catch (e: Exception) {
                logger.warn("Kunne ikke hente ut behandlinger som skal annulleres", e)
            }
        }
    }
}
