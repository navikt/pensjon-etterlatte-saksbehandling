package no.nav.etterlatte

import no.nav.etterlatte.libs.common.logging.withLogContext
import no.nav.etterlatte.libs.common.rapidsandrivers.correlationId
import no.nav.etterlatte.libs.common.rapidsandrivers.eventName
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import org.slf4j.LoggerFactory

internal class TestHendelser(
    rapidsConnection: RapidsConnection,
    private val behandlinger: Behandling,

    ) : River.PacketListener {
    private val logger = LoggerFactory.getLogger(OppdaterBehandling::class.java)


    init {
        River(rapidsConnection).apply {
            eventName("HAIL_TO_THE_KING_BABY")
            validate { it.requireKey("sakId") }
            correlationId()

        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) =
        withLogContext(packet.correlationId) {
            val sak = packet["saksId"].textValue().toLong()

            logger.info("""Hail To the King, Baby!""")
            logger.info("""Sletter sak med id $sak med tilhørende behandlinger""")
            behandlinger.slettSakOgBehandlinger(sak)

        }
}
