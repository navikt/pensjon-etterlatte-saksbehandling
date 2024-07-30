package no.nav.etterlatte.behandling.jobs

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.behandling.aktivitetsplikt.AktivitetspliktService
import no.nav.etterlatte.behandling.aktivitetsplikt.OppdaterAktivitetspliktRepo
import no.nav.etterlatte.behandling.aktivitetsplikt.OppdaterAktivitetspliktStatus
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.jobs.LoggerInfo
import no.nav.etterlatte.jobs.fixedRateCancellableTimer
import no.nav.etterlatte.libs.common.TimerJob
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.Timer

class SendTilStatistikkJob(
    private val aktivitetspliktService: AktivitetspliktService,
    private val oppdaterAktivitetspliktRepo: OppdaterAktivitetspliktRepo,
    private val initialDelay: Long,
    private val erLeader: () -> Boolean,
    private val interval: Duration,
) : TimerJob {
    private val logger = LoggerFactory.getLogger(this::class.java)
    private val jobbNavn = this::class.simpleName

    override fun schedule(): Timer {
        logger.info("$jobbNavn er satt til å kjøre med periode=$interval etter $initialDelay ms")

        return fixedRateCancellableTimer(
            name = jobbNavn,
            initialDelay = initialDelay,
            period = interval.toMillis(),
            loggerInfo = LoggerInfo(logger = logger, loggTilSikkerLogg = false),
        ) {
            if (erLeader()) {
                sendEnBatch()
            }
        }
    }

    private fun sendEnBatch() {
        val saker =
            inTransaction {
                oppdaterAktivitetspliktRepo
                    .hentSakerSomIkkeErSendt(100)
            }
        saker.forEach { sakId ->
            try {
                inTransaction {
                    runBlocking { aktivitetspliktService.sendMeldingOmAktivitetForSak(sakId) }
                    oppdaterAktivitetspliktRepo.oppdaterSakSendt(sakId, OppdaterAktivitetspliktStatus.SENDT)
                }
            } catch (e: Exception) {
                try {
                    logger.warn(
                        "Feil oppstod i sending av aktivitet til statistikk for sak med id=$sakId",
                        e,
                    )
                    inTransaction {
                        oppdaterAktivitetspliktRepo.oppdaterSakSendt(
                            sakId,
                            OppdaterAktivitetspliktStatus.FEIL_I_SENDING,
                        )
                    }
                } catch (inner: Exception) {
                    logger.warn(
                        "Kunne ikke oppdatere sak med feil i sending for sakId=$sakId, på grunn av feil",
                        inner,
                    )
                }
            }
        }
    }
}
