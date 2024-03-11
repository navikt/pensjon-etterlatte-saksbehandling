package no.nav.etterlatte.oppgave

import no.nav.etterlatte.behandling.BehandlingHendelserKafkaProducer
import no.nav.etterlatte.funksjonsbrytere.FeatureToggle
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.libs.common.behandling.BehandlingHendelseType
import java.util.UUID

// TODO Midlertidig klasse som skal kun kjøre en gang før den slettes..
class MigrerOppgavePaaVentStatistikk(
    private val dao: OppgaveDaoImpl,
    private val hendelser: BehandlingHendelserKafkaProducer,
    private val featureToggleService: FeatureToggleService,
) {
    init {
        if (featureToggleService.isEnabled(MigrerPaaVentStatistikkFeatureToggle.MigrerPaaVentStatistikk, false)) {
            migrer()
        }
    }

    fun migrer() {
        val behandlingsIder = dao.hentAllePaaVent()
        behandlingsIder.forEach {
            hendelser.sendMeldingForHendelsePaaVent(
                UUID.fromString(it),
                BehandlingHendelseType.PAA_VENT,
            )
        }
    }
}

enum class MigrerPaaVentStatistikkFeatureToggle(private val key: String) : FeatureToggle {
    MigrerPaaVentStatistikk("pensjon-etterlatte.migrer-paa-vent-statistikk"),
    ;

    override fun key() = key
}
