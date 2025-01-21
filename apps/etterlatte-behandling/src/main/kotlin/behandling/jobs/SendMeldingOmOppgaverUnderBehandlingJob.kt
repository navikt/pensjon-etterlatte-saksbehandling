package no.nav.etterlatte.behandling.jobs

import no.nav.etterlatte.Context
import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.Self
import no.nav.etterlatte.behandling.BehandlingHendelserKafkaProducer
import no.nav.etterlatte.behandling.hendelse.getUUID
import no.nav.etterlatte.common.ConnectionAutoclosing
import no.nav.etterlatte.common.DatabaseContext
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.jobs.LoggerInfo
import no.nav.etterlatte.jobs.fixedRateCancellableTimer
import no.nav.etterlatte.libs.common.Enhetsnummer
import no.nav.etterlatte.libs.common.TimerJob
import no.nav.etterlatte.libs.database.toList
import no.nav.etterlatte.libs.ktor.token.HardkodaSystembruker
import no.nav.etterlatte.oppgave.OppgaveDaoMedEndringssporing
import no.nav.etterlatte.sak.SakTilgangDao
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.Timer
import java.util.UUID
import javax.sql.DataSource

class SendMeldingOmOppgaverUnderBehandlingJob(
    private val erLeader: () -> Boolean,
    private val sendEndretEnhet: SendEndretEnhet,
    sakTilgangDao: SakTilgangDao,
    dataSource: DataSource,
) : TimerJob {
    private val jobbNavn = this::class.java.simpleName
    private val logger: Logger = LoggerFactory.getLogger(SendMeldingOmOppgaverUnderBehandlingJob::class.java)
    private val initialDelay = Duration.ofMinutes(3)
    private val period = Duration.ofMinutes(1)

    private var jobContext: Context =
        Context(
            Self(SendMeldingOmOppgaverUnderBehandlingJob::class.java.simpleName),
            DatabaseContext(dataSource),
            sakTilgangDao,
            HardkodaSystembruker.doedshendelse,
        )

    override fun schedule(): Timer {
        logger.info("$jobbNavn er satt til å kjøre med oppstart=$initialDelay og periode $period")

        return fixedRateCancellableTimer(
            name = jobbNavn,
            initialDelay = initialDelay.toMillis(),
            loggerInfo = LoggerInfo(logger = logger, loggTilSikkerLogg = false),
            period = period.toMillis(),
        ) {
            if (erLeader()) {
                sendEndretEnhet
                    .setupKontekstAndRun(jobContext)
            }
        }
    }
}

class SendEndretEnhet(
    private val behandlingHendelserKafkaProducer: BehandlingHendelserKafkaProducer,
    private val oppgaveEnhetEndretDao: OppgaveEnhetEndretDao,
    private val oppgaveEndringerDao: OppgaveDaoMedEndringssporing,
) {
    private val logger: Logger = LoggerFactory.getLogger(SendMeldingOmOppgaverUnderBehandlingJob::class.java)

    fun setupKontekstAndRun(jobContext: Context) {
        Kontekst.set(jobContext)
        sendMeldingForEnheter()
    }

    private fun sendMeldingForEnheter() {
        val oppgaverAaSendeMeldingFor =
            try {
                inTransaction {
                    oppgaveEnhetEndretDao.hentOppgaverUnderBehandlingManglerMelding()
                }
            } catch (e: Exception) {
                logger.warn("Kunne ikke hente ut oppgaver under behandling med enhet", e)
                return
            }
        oppgaverAaSendeMeldingFor.forEach { oppgaveOgEnhet ->
            try {
                inTransaction {
                    val endringerForOppgaveSistFoerst =
                        oppgaveEndringerDao.hentEndringerForOppgave(oppgaveOgEnhet.oppgaveId).sortedByDescending {
                            it.tidspunkt
                        }
                    val tidspunktForEndring =
                        endringerForOppgaveSistFoerst
                            .firstOrNull { it.oppgaveFoer.enhet != oppgaveOgEnhet.enhet }
                            ?.tidspunkt
                    behandlingHendelserKafkaProducer.sendMeldingForEndretEnhet(
                        oppgaveReferanse = oppgaveOgEnhet.referanse,
                        enhet = oppgaveOgEnhet.enhet,
                        overstyrtTekniskTid = tidspunktForEndring,
                    )
                }
            } catch (e: Exception) {
                logger.warn(
                    "Kunne ikke sende endret enhet for oppgave med id=${oppgaveOgEnhet.oppgaveId}, " +
                        "referanse=${oppgaveOgEnhet.referanse}",
                    e,
                )
            }
        }
    }
}

data class OppgaveEndretEnhet(
    val oppgaveId: UUID,
    val referanse: String,
    val enhet: Enhetsnummer,
)

class OppgaveEnhetEndretDao(
    private val connectionAutoclosing: ConnectionAutoclosing,
) {
    private val logger: Logger = LoggerFactory.getLogger(OppgaveEnhetEndretDao::class.java)

    fun hentOppgaverUnderBehandlingManglerMelding(): List<OppgaveEndretEnhet> =
        connectionAutoclosing.hentConnection {
            val statement =
                it.prepareStatement(
                    """
                    SELECT oppgave_id, referanse, enhet from send_melding_om_enhet where not sendt limit 10
                    """.trimIndent(),
                )
            statement.executeQuery().toList {
                val oppgaveId = getUUID("oppgave_id")
                val referanse = getString("referanse")
                val enhet = Enhetsnummer(getString("enhet"))
                OppgaveEndretEnhet(
                    oppgaveId = oppgaveId,
                    referanse = referanse,
                    enhet = enhet,
                )
            }
        }

    fun oppdaterSendtMelding(oppgaveId: UUID) {
        connectionAutoclosing.hentConnection {
            val statement =
                it.prepareStatement(
                    """
                    UPDATE send_melding_om_enhet SET sendt = true where oppgave_id = ?
                    """.trimIndent(),
                )
            statement.setObject(1, oppgaveId)
            val updated = statement.executeUpdate()
            if (updated != 1) {
                logger.warn("Kunne ikke oppdatere sendt enhet for oppgave med id=$oppgaveId")
            }
        }
    }
}
