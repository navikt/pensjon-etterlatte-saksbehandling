package no.nav.etterlatte.migrering

import no.nav.etterlatte.rapidsandrivers.ListenerMedLoggingOgFeilhaandtering
import no.nav.etterlatte.rapidsandrivers.migrering.Migreringshendelser
import no.nav.etterlatte.rapidsandrivers.migrering.PESYS_ID_KEY
import no.nav.etterlatte.rapidsandrivers.migrering.pesysId
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import org.slf4j.LoggerFactory

internal class PauseMigreringRiver(rapidsConnection: RapidsConnection, private val pesysRepository: PesysRepository) :
    ListenerMedLoggingOgFeilhaandtering() {
    private val logger = LoggerFactory.getLogger(this::class.java)

    init {
        initialiserRiver(rapidsConnection, Migreringshendelser.PAUSE) {
            validate { it.requireKey(PESYS_ID_KEY) }
        }
    }

    override fun haandterPakke(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        val pesysId = packet.pesysId
        logger.info("migrering har fattet vedtak og skal sette status til pause pesyssak=$pesysId")
        pesysRepository.oppdaterStatus(pesysId, Migreringsstatus.PAUSE)
        logger.info("migrering har satt status til pause pesyssak=$pesysId")
    }
}
