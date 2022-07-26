package no.nav.etterlatte.grunnlag

import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.Self
import no.nav.etterlatte.libs.common.logging.withLogContext
import no.nav.etterlatte.libs.common.rapidsandrivers.correlationId
import no.nav.etterlatte.libs.common.rapidsandrivers.eventName
import no.nav.etterlatte.libs.common.rapidsandrivers.eventNameKey
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class BehandlingEndretHendlese(
    rapidsConnection: RapidsConnection,
    private val grunnlag: GrunnlagService,
) : River.PacketListener {

    private val logger: Logger = LoggerFactory.getLogger(GrunnlagHendelser::class.java)

    init {
        River(rapidsConnection).apply {
            eventName("BEHANDLING:GRUNNLAGENDRET")
            correlationId()
            validate { it.requireKey("sakId") }
            validate { it.requireKey("behandlingId") }
            validate { it.requireKey("persongalleri.soeker") }
            validate { it.requireKey("behandlingOpprettet") }
            validate { it.rejectKey("grunnlag") }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) =
        withLogContext(packet.correlationId) {
            if (Kontekst.get().AppUser !is Self) {
                logger.warn("AppUser i kontekst er ikke Self i R&R-flyten")
            }

            try {
                val grunnlag = grunnlag.hentGrunnlag(packet["sakId"].asLong())
                context.publish(
                    JsonMessage.newMessage(
                        mapOf(
                            eventNameKey to packet[eventNameKey],
                            "grunnlag" to grunnlag,
                            "sakId" to packet["sakId"],
                            "behandlingId" to packet["behandlingId"],
                            "fnrSoeker" to packet["persongalleri.soeker"],
                            "behandlingOpprettet" to packet["behandlingOpprettet"],
                        )
                    ).toJson()
                )

            } catch (e: Exception) {
                logger.error("Feil ved henting av grunnlag", e)
            }
        }
}
