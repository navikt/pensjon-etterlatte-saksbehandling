package no.nav.etterlatte

import no.nav.etterlatte.common.objectMapper
import no.nav.etterlatte.libs.common.logging.withLogContext
import no.nav.etterlatte.libs.common.pdlhendelse.Doedshendelse
import no.nav.etterlatte.libs.common.rapidsandrivers.correlationId
import no.nav.etterlatte.libs.common.rapidsandrivers.eventName
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import org.slf4j.LoggerFactory

internal class PdlHendelser(
    rapidsConnection: RapidsConnection,
    private val behandlinger: Behandling,

    ) : River.PacketListener {
    private val logger = LoggerFactory.getLogger(OppdaterBehandling::class.java)

    init {
        logger.info("initierer rapid for pdlHendelser")
        River(rapidsConnection).apply {
            eventName("PDL:PERSONHENDELSE")

            correlationId()
            validate { it.requireKey("hendelse") }
            validate { it.interestedIn("hendelse_data") }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) =
        withLogContext(packet.correlationId) {
            logger.info("Mottatt hendelse fra pdl: ${packet["hendelse"]}")
            try {
                if (packet["hendelse"].asText() == "DOEDSFALL_V1") {
                    logger.info("Doedshendelse mottatt")
                    val doedshendelse = objectMapper.treeToValue(packet["hendelse_data"], Doedshendelse::class.java)
                    behandlinger.sendDoedshendelse(doedshendelse)
                } else {
                    logger.info("Pdl-hendelsestypen mottatt håndteres ikke av applikasjonen")
                }
            } catch (e: Exception) {
                logger.error("kunne ikke deserialisere pdl-hendelse: ", e)
            }
        }
}
