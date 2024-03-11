package no.nav.etterlatte.oppgave

import no.nav.etterlatte.behandling.BehandlingHendelserKafkaProducer
import no.nav.etterlatte.funksjonsbrytere.FeatureToggle
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.jobs.LoggerInfo
import no.nav.etterlatte.jobs.TimerJob
import no.nav.etterlatte.jobs.fixedRateCancellableTimer
import no.nav.etterlatte.libs.common.behandling.BehandlingHendelseType
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.Timer
import java.util.UUID

// TODO Midlertidig klasse som skal kun kjøre en gang før den slettes..
class MigrerOppgavePaaVentStatistikk(
    private val dao: OppgaveDaoImpl,
    private val hendelser: BehandlingHendelserKafkaProducer,
    private val featureToggleService: FeatureToggleService,
    private val erLeader: () -> Boolean,
    private val initialDelay: Long,
    private val periode: Duration,
) : TimerJob {
    private val logger = LoggerFactory.getLogger(this::class.java)
    private val jobbNavn = this::class.simpleName

    override fun schedule(): Timer {
        return fixedRateCancellableTimer(
            name = jobbNavn,
            initialDelay = initialDelay,
            loggerInfo = LoggerInfo(logger = logger, loggTilSikkerLogg = false),
            period = periode.toMillis(),
        ) {
            if (erLeader()) {
                migrer()
            }
        }
    }

    fun migrer() {
        if (featureToggleService.isEnabled(MigrerPaaVentStatistikkFeatureToggle.MigrerPaaVentStatistikk, false)) {
            logger.info("Kjører $jobbNavn")
            val behandlingsIder = dao.hentAllePaaVent()
            behandlingsIder.forEach {
                hendelser.sendMeldingForHendelsePaaVent(
                    UUID.fromString(it),
                    BehandlingHendelseType.PAA_VENT,
                )
            }
        } else {
            logger.info("Kjører ikke $jobbNavn. Skru på feature toggle.")
        }
    }
}

enum class MigrerPaaVentStatistikkFeatureToggle(private val key: String) : FeatureToggle {
    MigrerPaaVentStatistikk("pensjon-etterlatte.migrer-paa-vent-statistikk"),
    ;

    override fun key() = key
}
