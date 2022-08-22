package no.nav.etterlatte.grunnlagsendring

import no.nav.etterlatte.behandling.common.LeaderElection
import no.nav.etterlatte.libs.common.logging.withLogContext
import org.slf4j.LoggerFactory
import java.time.Duration
import kotlin.concurrent.fixedRateTimer

class GrunnlagsendringshendelseJob(
    private val grunnlagsendringshendelseService: GrunnlagsendringshendelseService,
    private val leaderElection: LeaderElection,
    private val initialDelay: Long,
    private val periode: Duration,
    private val minutterGamleHendelser: Long
) {
    private val jobbNavn = this::class.simpleName
    private val logger = LoggerFactory.getLogger(this::class.java)
    fun schedule() =
        fixedRateTimer(
            name = jobbNavn,
            daemon = true,
            initialDelay = initialDelay,
            period = periode.toMillis()
        ) {
            try {
                logger.info(
                    "Forbereder GrunnlagsendringshendelseJob med jobbavn: $jobbNavn, initialDelay: $initialDelay og " +
                        "periode i ms: ${periode.toMillis()}"
                )
                SjekkKlareGrunnlagsendringshendelser(
                    grunnlagsendringshendelseService = grunnlagsendringshendelseService,
                    leaderElection = leaderElection,
                    jobbNavn = jobbNavn!!,
                    minutterGamleHendelser = minutterGamleHendelser
                ).run()
            } catch (throwable: Throwable) {
                logger.error("Jobb for aa sjekke klare grunnlagsendringshendelser feilet", throwable)
            }
        }

    class SjekkKlareGrunnlagsendringshendelser(
        val grunnlagsendringshendelseService: GrunnlagsendringshendelseService,
        val leaderElection: LeaderElection,
        val jobbNavn: String,
        val minutterGamleHendelser: Long
    ) {
        private val log = LoggerFactory.getLogger(this::class.java)

        fun run() {
            withLogContext {
                if (leaderElection.isLeader()) {
                    log.info("Starter jobb: $jobbNavn")
                    grunnlagsendringshendelseService.sjekkKlareGrunnlagsendringshendelser(minutterGamleHendelser)
                } else {
                    log.info("Ikke leader, saa kjoerer ikke jobb.")
                }
            }
        }
    }
}