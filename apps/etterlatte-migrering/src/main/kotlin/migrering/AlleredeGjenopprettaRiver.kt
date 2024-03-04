package no.nav.etterlatte.migrering

import no.nav.etterlatte.rapidsandrivers.ListenerMedLoggingOgFeilhaandtering
import no.nav.etterlatte.rapidsandrivers.migrering.Migreringshendelser
import no.nav.etterlatte.rapidsandrivers.migrering.PESYS_ID_KEY
import no.nav.etterlatte.rapidsandrivers.migrering.pesysId
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import org.slf4j.Logger
import org.slf4j.LoggerFactory

internal class AlleredeGjenopprettaRiver(
    rapidsConnection: RapidsConnection,
    private val pesysRepository: PesysRepository,
) : ListenerMedLoggingOgFeilhaandtering() {
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)

    init {
        initialiserRiver(rapidsConnection, Migreringshendelser.ALLEREDE_GJENOPPRETTA) {
            validate { it.requireKey(PESYS_ID_KEY) }
        }
    }

    override fun haandterPakke(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        logger.info("Allerde gjenoppretta pesys-sak ${packet.pesysId} i Gjenny. Gjør ingenting mer.")
        pesysRepository.oppdaterStatus(packet.pesysId, Migreringsstatus.ALLEREDE_GJENOPPRETTA)
    }
}
