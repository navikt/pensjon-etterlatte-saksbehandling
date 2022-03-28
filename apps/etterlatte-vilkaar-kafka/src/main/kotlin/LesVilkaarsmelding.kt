import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import no.nav.etterlatte.libs.common.logging.withLogContext
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.vikaar.VilkaarOpplysning
import no.nav.etterlatte.model.VilkaarService
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import java.util.*

internal class LesVilkaarsmelding(
    rapidsConnection: RapidsConnection,
    private val vilkaar: VilkaarService
) : River.PacketListener {

    init {
        River(rapidsConnection).apply {
            validate { it.demandValue("@event", "BEHANDLING:GRUNNLAGENDRET") }
            //validate { it.requireKey("@vilkaarsopplysninger") }
            //validate { it.rejectKey("@sak_id") }
            //validate { it.rejectKey("@behandling_id") }
            validate {it.rejectKey("@vilkaarsvurdering")}
            validate { it.interestedIn("@correlation_id") }

        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) =
        withLogContext(packet.correlationId()) {

            //TODO Logikk for å kalle vilkarService
            val vilkaarsVurdering = vilkaar.mapVilkaar(
                objectMapper.treeToValue(
                    packet["grunnlag"],
                    JsonNodeDto::class.java
                ).opplysninger
            )
            //val sak = packet["@sak_id"]
            //val behandlingsid = packet["@behandling_id"]

            packet["@vilkaarsvurdering"] = vilkaarsVurdering

            context.publish(packet.toJson())
        }
}

private fun JsonMessage.correlationId(): String? = get("@correlation_id").textValue()


data class JsonNodeDto(val opplysninger: List<VilkaarOpplysning<ObjectNode>>)