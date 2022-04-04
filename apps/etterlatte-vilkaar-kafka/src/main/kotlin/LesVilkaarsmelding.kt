
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.readValue
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
            validate { it.rejectKey("@gyldighetsvurdering") }
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
                println("spiser en melding fordi: " +e)
            }


        }
}

private fun JsonMessage.correlationId(): String? = get("@correlation_id").textValue()
