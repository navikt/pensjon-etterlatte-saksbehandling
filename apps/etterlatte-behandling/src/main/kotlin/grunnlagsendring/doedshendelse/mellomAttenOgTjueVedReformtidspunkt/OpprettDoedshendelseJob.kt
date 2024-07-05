package no.nav.etterlatte.grunnlagsendring.doedshendelse.mellom18og20PaaReformtidspunkt

import no.nav.etterlatte.Context
import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.Self
import no.nav.etterlatte.common.DatabaseContext
import no.nav.etterlatte.grunnlagsendring.doedshendelse.mellomAttenOgTjueVedReformtidspunkt.OpprettDoedshendelseService
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.jobs.LoggerInfo
import no.nav.etterlatte.jobs.fixedRateCancellableTimer
import no.nav.etterlatte.libs.common.TimerJob
import no.nav.etterlatte.libs.common.logging.getCorrelationId
import no.nav.etterlatte.libs.common.logging.withLogContext
import no.nav.etterlatte.libs.common.person.maskerFnr
import no.nav.etterlatte.sak.SakTilgangDao
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.Timer
import javax.sql.DataSource

class OpprettDoedshendelseJob(
    private val mellom18og20PaaReformtidspunktDao: OpprettDoedshendelseDao,
    private val opprettDoedshendelseService: OpprettDoedshendelseService,
    private val erLeader: () -> Boolean,
    private val initialDelay: Long,
    private val interval: Duration,
    dataSource: DataSource,
    val sakTilgangDao: SakTilgangDao,
) : TimerJob {
    private val logger = LoggerFactory.getLogger(this::class.java)
    private val jobbNavn = this::class.simpleName

    private var jobContext: Context = Context(Self(this::class.java.simpleName), DatabaseContext(dataSource), sakTilgangDao)

    override fun schedule(): Timer {
        logger.info("$jobbNavn er satt til å kjøre med periode $interval")

        return fixedRateCancellableTimer(
            name = jobbNavn,
            initialDelay = 0,
            loggerInfo = LoggerInfo(logger = logger, loggTilSikkerLogg = false),
            period = interval.toMillis(),
        ) {
            if (erLeader()) {
                Kontekst.set(jobContext)
                run()
            }
        }
    }

    private fun run() {
        withLogContext(correlationId = getCorrelationId(), kv = mapOf("migrering" to "true")) {
            val listeOverAvdoede =
                inTransaction {
                    mellom18og20PaaReformtidspunktDao.hentAvdoede(
                        listOf(OpprettDoedshendelseStatus.NY),
                    )
                }
            listeOverAvdoede.forEach { fnr ->
                try {
                    inTransaction { mellom18og20PaaReformtidspunktDao.oppdater(fnr, OpprettDoedshendelseStatus.STARTET) }
                    logger.info("Starter håndtering av dødshendelse for person ${fnr.maskerFnr()}")
                    opprettDoedshendelseService.opprettDoedshendelse(fnr)
                    inTransaction { mellom18og20PaaReformtidspunktDao.oppdater(fnr, OpprettDoedshendelseStatus.OPPRETTET) }
                } catch (e: Exception) {
                    inTransaction {
                        mellom18og20PaaReformtidspunktDao.oppdater(
                            fnr,
                            OpprettDoedshendelseStatus.FEILET,
                            e.stackTraceToString(),
                        )
                    }
                }
            }
        }
    }
}
