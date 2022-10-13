package no.nav.etterlatte.grunnlag

import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.event.BehandlingGrunnlagEndret
import no.nav.etterlatte.libs.common.event.BehandlingGrunnlagEndretMedGrunnlag
import no.nav.etterlatte.libs.common.logging.withLogContext
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.rapidsandrivers.correlationId
import no.nav.etterlatte.libs.common.rapidsandrivers.eventName
import no.nav.etterlatte.libs.common.toJson
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class BehandlingEndretHendlese(
    rapidsConnection: RapidsConnection,
    private val grunnlagService: GrunnlagService
) : River.PacketListener {
    private val logger: Logger = LoggerFactory.getLogger(GrunnlagHendelser::class.java)

    init {
        River(rapidsConnection).apply {
            eventName(BehandlingGrunnlagEndret.eventName)
            correlationId()
            validate { it.requireKey(BehandlingGrunnlagEndret.sakIdKey) }
            validate { it.requireKey(BehandlingGrunnlagEndret.persongalleriKey) }
            validate { it.rejectKey(BehandlingGrunnlagEndretMedGrunnlag.grunnlagKey) }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) =
        withLogContext(packet.correlationId) {
            try {
                val sakId = packet[BehandlingGrunnlagEndret.sakIdKey].asLong()
                val persongalleri = objectMapper.readValue(
                    packet[BehandlingGrunnlagEndret.persongalleriKey].toJson(),
                    Persongalleri::class.java
                )

                val grunnlag = grunnlagService.hentOpplysningsgrunnlag(sakId, persongalleri)

                packet[BehandlingGrunnlagEndretMedGrunnlag.grunnlagKey] = grunnlag

                context.publish(packet.toJson())
            } catch (e: Exception) {
                logger.error("Feil ved henting av grunnlag", e)
            }
        }
}