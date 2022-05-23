
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.etterlatte.libs.common.logging.withLogContext
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.model.AvkortingService
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import org.slf4j.LoggerFactory

internal class LesAvkortingsmelding(
    rapidsConnection: RapidsConnection,
    private val avkorting: AvkortingService
) : River.PacketListener {
    private val logger = LoggerFactory.getLogger(LesAvkortingsmelding::class.java)
    init {
        River(rapidsConnection).apply {
            validate { it.demandValue("@event", "BEHANDLING:GRUNNLAGENDRET") }
            validate { it.requireKey("grunnlag") }
            validate { it.requireKey("@vilkaarsvurdering") }
            //TODO se på logikk for å 'samle' rivers
            //validate { it.requireKey("@gyldighetsvurdering") }
            validate { it.rejectKey("@beregning") }
            validate { it.interestedIn("@correlation_id") }

        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) =
        withLogContext(packet.correlationId()) {

            //TODO her må jeg sikkert ha noe anna info
            val grunnlagListe = packet["grunnlag"].toString()
            try {
                val beregningsResultat = avkorting.avkortingsResultat(objectMapper.readValue(grunnlagListe))
                packet["@beregning"] = beregningsResultat
                context.publish(packet.toJson())
                logger.info("Publisert en beregning")
            } catch (e: Exception){
                //TODO endre denne
                println("spiser en melding fordi: " +e)
            }


        }
}

private fun JsonMessage.correlationId(): String? = get("@correlation_id").textValue()
