package no.nav.etterlatte.brev.batch

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.Context
import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.Self
import no.nav.etterlatte.common.DatabaseContext
import no.nav.etterlatte.jobs.LoggerInfo
import no.nav.etterlatte.jobs.fixedRateCancellableTimer
import no.nav.etterlatte.libs.common.TimerJob
import no.nav.etterlatte.libs.ktor.token.HardkodaSystembruker
import no.nav.etterlatte.sak.SakTilgangDao
import org.slf4j.LoggerFactory
import java.util.Timer
import javax.sql.DataSource

class BrevBatchJob(
    private val service: BrevBatchJobService,
    private val config: BrevBatchConfig,
    private val erLeader: () -> Boolean,
    private val dataSource: DataSource,
    private val sakTilgangDao: SakTilgangDao,
) : TimerJob {
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun schedule(): Timer =
        fixedRateCancellableTimer(
            name = "BrevBatchJob",
            initialDelay = config.intervall.toMillis(),
            period = config.intervall.toMillis(),
            loggerInfo = LoggerInfo(logger, loggTilSikkerLogg = false),
        ) {
            if (config.aktivert && erLeader()) {
                val context = Context(Self("BrevBatchJob"), DatabaseContext(dataSource), sakTilgangDao, HardkodaSystembruker.brevBatch)
                Kontekst.set(context)
                try {
                    runBlocking { service.kjoer(HardkodaSystembruker.brevBatch, erLeader) }
                } catch (_: Exception) {
                    logger.warn("Brevbatch feilkode=BREV_BATCH_JOBB_FEIL")
                } finally {
                    Kontekst.remove()
                }
            }
        }
}
