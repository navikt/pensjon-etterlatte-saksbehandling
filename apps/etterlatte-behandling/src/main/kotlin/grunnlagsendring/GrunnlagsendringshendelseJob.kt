package no.nav.etterlatte.grunnlagsendring

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asContextElement
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import no.nav.etterlatte.Context
import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.Self
import no.nav.etterlatte.common.DatabaseContext
import no.nav.etterlatte.jobs.fixedRateCancellableTimer
import no.nav.etterlatte.jobs.shuttingDown
import no.nav.etterlatte.libs.common.logging.withLogContext
import no.nav.etterlatte.libs.jobs.LeaderElection
import no.nav.etterlatte.sikkerLogg
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.temporal.ChronoUnit
import java.util.*
import javax.sql.DataSource

class GrunnlagsendringshendelseJob(
    private val datasource: DataSource,
    private val grunnlagsendringshendelseService: GrunnlagsendringshendelseService,
    private val leaderElection: LeaderElection,
    private val initialDelay: Long,
    private val periode: Duration,
    private val minutterGamleHendelser: Long
) {
    private val jobbNavn = this::class.simpleName
    private val logger = LoggerFactory.getLogger(this::class.java)

    fun schedule(): Timer {
        logger.info(
            "Setter opp GrunnlagsendringshendelseJob. LeaderElection: ${leaderElection.isLeader()} " +
                ", initialDelay: ${Duration.of(1, ChronoUnit.MINUTES).toMillis()}" +
                ", periode: ${periode.toMinutes()}" +
                ", minutterGamleHendelser: $minutterGamleHendelser "
        )
        return fixedRateCancellableTimer(
            name = jobbNavn,
            initialDelay = initialDelay,
            period = periode.toMillis(),
            logger = logger,
            sikkerLogg = sikkerLogg
        ) {
            runBlocking {
                SjekkKlareGrunnlagsendringshendelser(
                    grunnlagsendringshendelseService = grunnlagsendringshendelseService,
                    leaderElection = leaderElection,
                    jobbNavn = jobbNavn!!,
                    minutterGamleHendelser = minutterGamleHendelser,
                    datasource = datasource
                ).run(it)
            }
        }
    }

    class SjekkKlareGrunnlagsendringshendelser(
        val grunnlagsendringshendelseService: GrunnlagsendringshendelseService,
        val leaderElection: LeaderElection,
        val jobbNavn: String,
        val minutterGamleHendelser: Long,
        val datasource: DataSource
    ) {
        private val log = LoggerFactory.getLogger(this::class.java)

        suspend fun run(correlationId: String) {
            if (leaderElection.isLeader() && !shuttingDown.get()) {
                log.info("Starter jobb: $jobbNavn")

                coroutineScope {
                    launch {
                        withContext(
                            Dispatchers.Default + Kontekst.asContextElement(
                                value = Context(Self("GrunnlagsendringshendelseJob"), DatabaseContext(datasource))
                            )
                        ) {
                            withLogContext(correlationId) {
                                grunnlagsendringshendelseService.sjekkKlareGrunnlagsendringshendelser(
                                    minutterGamleHendelser
                                )
                            }
                        }
                    }
                }
            } else {
                log.debug("Ikke leader, saa kjoerer ikke jobb: $jobbNavn.")
            }
        }
    }
}