package no.nav.etterlatte.grunnlag

import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.Self
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.common.logging.withLogContext
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class BehandlingEndretHendlese (
    rapidsConnection: RapidsConnection,
    private val grunnlag: GrunnlagFactory,
) : River.PacketListener {


    private val logger: Logger = LoggerFactory.getLogger(GrunnlagHendelser::class.java)

    init {
        River(rapidsConnection).apply {
            validate { it.demandValue("@event", "BEHANDLING:GRUNNLAGENDRET") }
            validate { it.requireKey("sak") }
            validate { it.rejectKey("grunnlag") }
            validate { it.interestedIn("@correlation_id") }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) =
        withLogContext(packet.correlationId()) {
            if(Kontekst.get().AppUser !is Self){ logger.warn("AppUser i kontekst er ikke Self i R&R-flyten") }
            packet["grunnlag"]= inTransaction {
                grunnlag.hent(packet["sak"].asLong())
            }.serialiserbarUtgave().grunnlag
            context.publish(packet.toJson())
        }

    private fun JsonMessage.correlationId(): String? = get("@correlation_id").textValue()
}
