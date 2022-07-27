package no.nav.etterlatte.grunnlag

import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.Self
import no.nav.etterlatte.libs.common.event.BehandlingGrunnlagEndret
import no.nav.etterlatte.libs.common.event.BehandlingGrunnlagEndretMedGrunnlag
import no.nav.etterlatte.libs.common.logging.withLogContext
import no.nav.etterlatte.libs.common.rapidsandrivers.correlationId
import no.nav.etterlatte.libs.common.rapidsandrivers.eventName
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
            eventName(BehandlingGrunnlagEndret.eventName)
            correlationId()
            validate {  it.requireKey(BehandlingGrunnlagEndret.sakIdKey) }
            validate {  it.rejectKey(BehandlingGrunnlagEndretMedGrunnlag.grunnlagKey) }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) =
        withLogContext(packet.correlationId) {
            if (Kontekst.get().AppUser !is Self) {
                logger.warn("AppUser i kontekst er ikke Self i R&R-flyten")
            }

            try {
                val grunnlag = grunnlag.hentGrunnlag(packet[BehandlingGrunnlagEndret.sakIdKey].asLong())
                packet[BehandlingGrunnlagEndretMedGrunnlag.grunnlagKey] = grunnlag
                context.publish(
                    packet.toJson()
                )

            } catch (e: Exception) {
                logger.error("Feil ved henting av grunnlag", e)
            }
        }
}
