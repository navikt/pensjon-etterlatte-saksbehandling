package no.nav.etterlatte.migrering

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import no.nav.etterlatte.behandling.BehandlingService
import no.nav.etterlatte.migrering.Migreringshendelser
import no.nav.etterlatte.rapidsandrivers.BEHANDLING_ID_KEY
import no.nav.etterlatte.rapidsandrivers.Kontekst
import no.nav.etterlatte.rapidsandrivers.ListenerMedLoggingOgFeilhaandtering
import no.nav.etterlatte.rapidsandrivers.behandlingId
import org.slf4j.LoggerFactory

internal class AvbrytBehandlingHvisMigreringFeilaRiver(
    rapidsConnection: RapidsConnection,
    private val behandlingService: BehandlingService,
) : ListenerMedLoggingOgFeilhaandtering() {
    private val logger = LoggerFactory.getLogger(this::class.java)

    init {
        initialiserRiver(rapidsConnection, Migreringshendelser.AVBRYT_BEHANDLING) {
            validate { it.requireKey(BEHANDLING_ID_KEY) }
        }
    }

    override fun haandterPakke(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        logger.info("Avbryter behandling ${packet.behandlingId} fordi den feila under migrering")
        behandlingService.avbryt(packet.behandlingId)
        logger.info("Har avbrutt behandling ${packet.behandlingId} fordi den feila under migrering")
    }

    override fun kontekst() = Kontekst.MIGRERING
}
