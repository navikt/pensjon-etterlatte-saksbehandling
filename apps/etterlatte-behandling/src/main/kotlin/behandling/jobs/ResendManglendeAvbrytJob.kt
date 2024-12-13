package no.nav.etterlatte.behandling.jobs

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.behandling.BehandlingDao
import no.nav.etterlatte.behandling.BehandlingHendelserKafkaProducer
import no.nav.etterlatte.behandling.SendManglendeMeldingerDao
import no.nav.etterlatte.behandling.domain.toStatistikkBehandling
import no.nav.etterlatte.behandling.hendelse.HendelseDao
import no.nav.etterlatte.behandling.klienter.GrunnlagKlient
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.jobs.LoggerInfo
import no.nav.etterlatte.jobs.fixedRateCancellableTimer
import no.nav.etterlatte.libs.common.TimerJob
import no.nav.etterlatte.libs.common.behandling.BehandlingHendelseType
import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.tidspunkt.toTidspunkt
import no.nav.etterlatte.libs.ktor.token.HardkodaSystembruker
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.LocalDate
import java.util.Timer
import java.util.UUID

class ResendManglendeAvbrytJob(
    private val erLeader: () -> Boolean,
    private val kafkaProducer: BehandlingHendelserKafkaProducer,
    private val sendManglendeMeldingerDao: SendManglendeMeldingerDao,
    private val behandlingDao: BehandlingDao,
    private val grunnlagKlient: GrunnlagKlient,
    private val hendelseDao: HendelseDao,
) : TimerJob {
    private val logger = LoggerFactory.getLogger(ResendManglendeAvbrytJob::class.java)
    private val jobbNavn = ResendManglendeAvbrytJob::class.java.simpleName
    private val initialDelay = Duration.ofMinutes(3L)
    private val period = Duration.ofMinutes(3L)

    override fun schedule(): Timer {
        logger.info("Jobb $jobbNavn startet med initialDelay $initialDelay med periode $period")
        return fixedRateCancellableTimer(
            name = jobbNavn,
            initialDelay = initialDelay.toMillis(),
            period = period.toMillis(),
            loggerInfo =
                LoggerInfo(
                    logger = logger,
                    loggTilSikkerLogg = false,
                ),
        ) {
            SendManglendeAvbrytMelding(
                erLeader,
                sendManglendeMeldingerDao,
                kafkaProducer,
                behandlingDao,
                grunnlagKlient,
                hendelseDao,
            ).send()
        }
    }

    class SendManglendeAvbrytMelding(
        private val erLeader: () -> Boolean,
        private val sendManglendeMeldingerDao: SendManglendeMeldingerDao,
        private val kafkaProducer: BehandlingHendelserKafkaProducer,
        private val behandlingDao: BehandlingDao,
        private val grunnlagKlient: GrunnlagKlient,
        private val hendelseDao: HendelseDao,
    ) {
        private val logger = LoggerFactory.getLogger(SendManglendeAvbrytMelding::class.java)

        fun send() {
            if (!erLeader()) {
                logger.info("Er ikke leader, kjører ikke ${SendManglendeAvbrytMelding::class.java.simpleName}-jobb")
                return
            }
            try {
                val behandlinger = inTransaction { sendManglendeMeldingerDao.hentManglendeAvslagBehandling() }
                behandlinger.forEach {
                    try {
                        inTransaction {
                            val behandling =
                                behandlingDao.hentBehandling(it.behandlingId)
                                    ?: throw InternfeilException("Behandling ${it.behandlingId} ble ikke funnet")
                            val inntruffet = behandling.sistEndret.toTidspunkt()
                            val persongalleri =
                                runBlocking {
                                    grunnlagKlient.hentPersongalleri(
                                        behandling.id,
                                        HardkodaSystembruker.oppgave,
                                    )
                                }
                                    ?: throw InternfeilException("Kunne ikke hente persongalleri for behandling ${behandling.id}")
                            if (it.manglerHendelse) {
                                hendelseDao.behandlingAvbrutt(
                                    behandling = behandling,
                                    saksbehandler = "EY",
                                    kommentar =
                                        "Behandling ble avbrutt igjennom flyt som ikke lagret hendelse. " +
                                            "Hendelse ble lagt til ${LocalDate.now()}",
                                    valgtBegrunnelse = "RYDDING_HENDELSER",
                                )
                            }
                            kafkaProducer.sendMeldingForHendelseStatistikk(
                                statistikkBehandling = behandling.toStatistikkBehandling(persongalleri.opplysning),
                                hendelseType = BehandlingHendelseType.AVBRUTT,
                                overstyrtTekniskTid = inntruffet,
                            )
                            sendManglendeMeldingerDao.oppdaterSendtMelding(behandling.id)
                        }
                    } catch (e: Exception) {
                        logger.warn(
                            "Kunne ikke sende sende hendelse for behandling med id = ${it.behandlingId} i " +
                                "sak ${it.sakId}",
                        )
                    }
                }
            } catch (e: Exception) {
                logger.warn("Kunne ikke sende avbryt melding for behandling som mangler", e)
            }
        }
    }
}

data class BehandlingSomIkkeErAvbruttIStatistikk(
    val behandlingId: UUID,
    val sakId: SakId,
    val manglerHendelse: Boolean,
)
