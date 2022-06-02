package no.nav.etterlatte.rivers

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.etterlatte.VedtaksvurderingService
import no.nav.etterlatte.libs.common.avkorting.AvkortingsResultat
import no.nav.etterlatte.libs.common.logging.withLogContext
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import org.slf4j.LoggerFactory
import java.util.*

internal class LagreAvkorting(
    rapidsConnection: RapidsConnection,
    val vedtaksvurderingService: VedtaksvurderingService
) : River.PacketListener {
    private val logger = LoggerFactory.getLogger(LagreAvkorting::class.java)

    init {
        River(rapidsConnection).apply {
            validate { it.demandValue("@event", "BEHANDLING:GRUNNLAGENDRET") }
            validate { it.requireKey("sak") }
            validate { it.requireKey("id") }
            validate { it.requireKey("@avkorting") }
            validate { it.requireKey("soeker") }
            validate { it.requireKey("@avkorting") }
            validate { it.requireKey("@avkorting") }
            validate { it.interestedIn("@correlation_id") }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) =
        withLogContext(packet.correlationId()) {
            val behandlingId = UUID.fromString(packet["id"].textValue())
            val sakId = packet["sak"].toString()
            val avkorting = objectMapper.readValue<AvkortingsResultat>(packet["@avkorting"].toString())

            try {
                vedtaksvurderingService.lagreAvkorting(sakId, behandlingId, packet["soeker"].textValue(), avkorting)
            } catch (e: Exception){
                //TODO endre denne
                println("spiser en melding fordi: " +e)
            }

        }
}

private fun JsonMessage.correlationId(): String? = get("@correlation_id").textValue()