package no.nav.etterlatte.migrering

import no.nav.etterlatte.BehandlingService
import no.nav.etterlatte.rapidsandrivers.migrering.Migreringshendelser
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import org.slf4j.LoggerFactory
import rapidsandrivers.BEHANDLING_ID_KEY
import rapidsandrivers.behandlingId
import rapidsandrivers.migrering.ListenerMedLoggingOgFeilhaandtering

internal class AvbrytBehandlingHvisMigreringFeilaRiver(
    rapidsConnection: RapidsConnection,
    private val behandlingService: BehandlingService,
) :
    ListenerMedLoggingOgFeilhaandtering(Migreringshendelser.AVBRYT_BEHANDLING) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    init {
        initialiserRiver(rapidsConnection, hendelsestype) {
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
}
