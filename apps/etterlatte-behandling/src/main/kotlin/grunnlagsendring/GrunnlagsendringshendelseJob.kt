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
import no.nav.etterlatte.common.LeaderElection
import no.nav.etterlatte.libs.common.logging.withLogContext
import org.slf4j.LoggerFactory
import java.time.Duration
import javax.sql.DataSource
import kotlin.concurrent.fixedRateTimer

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
    fun schedule() =
        fixedRateTimer(
            name = jobbNavn,
            daemon = true,
            initialDelay = initialDelay,
            period = periode.toMillis()
        ) {
            try {
                runBlocking {
                    SjekkKlareGrunnlagsendringshendelser(
                        grunnlagsendringshendelseService = grunnlagsendringshendelseService,
                        leaderElection = leaderElection,
                        jobbNavn = jobbNavn!!,
                        minutterGamleHendelser = minutterGamleHendelser,
                        datasource = datasource
                    ).run()
                }
            } catch (throwable: Throwable) {
                logger.error("Jobb for aa sjekke klare grunnlagsendringshendelser feilet", throwable)
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

        suspend fun run() {
            if (leaderElection.isLeader()) {
                withLogContext {
                    log.info("Starter jobb: $jobbNavn")
                }
                coroutineScope {
                    launch {
                        withContext(
                            Dispatchers.Default + Kontekst.asContextElement(
                                value = Context(Self("GrunnlagsendringshendelseJob"), DatabaseContext(datasource))
                            )
                        ) {
                            withLogContext {
                                grunnlagsendringshendelseService.sjekkKlareGrunnlagsendringshendelser(
                                    minutterGamleHendelser
                                )
                            }
                        }
                    }
                }
            } else {
                withLogContext { log.info("Ikke leader, saa kjoerer ikke jobb.") }
            }
        }
    }
}