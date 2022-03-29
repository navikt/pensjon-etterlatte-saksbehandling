import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.jacksonTypeRef
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.treeToValue
import no.nav.etterlatte.libs.common.behandling.BehandlingSammendragListe
import no.nav.etterlatte.libs.common.behandling.Behandlingsopplysning
import no.nav.etterlatte.libs.common.logging.withLogContext
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.vikaar.VilkaarOpplysning
import no.nav.etterlatte.model.VilkaarService
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import org.slf4j.LoggerFactory

internal class LesVilkaarsmelding(
    rapidsConnection: RapidsConnection,
    private val vilkaar: VilkaarService
) : River.PacketListener {
    private val logger = LoggerFactory.getLogger(LesVilkaarsmelding::class.java)
    init {
        River(rapidsConnection).apply {
            validate { it.demandValue("@event", "BEHANDLING:GRUNNLAGENDRET") }
            validate { it.requireKey("grunnlag") }
            validate { it.rejectKey("@vilkaarsvurdering") }
            validate { it.interestedIn("@correlation_id") }

        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) =
        withLogContext(packet.correlationId()) {


            val grunnlagListe = packet["grunnlag"].toString()
            try {
                val hmm = objectMapper.readValue<List<VilkaarOpplysning<ObjectNode>>>(grunnlagListe)
                val vilkaarsVurdering = vilkaar.mapVilkaar(hmm)
                packet["@vilkaarsvurdering"] = vilkaarsVurdering
                context.publish(packet.toJson())
                //TODO
                logger.info("Vurdert Vilkår")
            } catch (e: Exception){
                //TODO endre denne
                println("spiser en melding fordi" +e)
            }


        }
}

private fun JsonMessage.correlationId(): String? = get("@correlation_id").textValue()
