
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.readValue
import model.GyldigSoeknadService
import no.nav.etterlatte.libs.common.logging.withLogContext
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.vikaar.VilkaarOpplysning
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import org.slf4j.LoggerFactory

internal class LesGyldigSoeknadsmelding(
    rapidsConnection: RapidsConnection,
    private val gyldigSoeknad: GyldigSoeknadService
) : River.PacketListener {
    private val logger = LoggerFactory.getLogger(LesGyldigSoeknadsmelding::class.java)
    init {
        River(rapidsConnection).apply {
            validate { it.demandValue("@event", "BEHANDLING:GRUNNLAGENDRET") }
            validate { it.requireKey("grunnlag") }
            validate { it.rejectKey("@gyldighetsvurdering") }
            validate { it.rejectKey("@vilkaarsvurdering") }
            validate { it.interestedIn("@correlation_id") }

        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) =
        withLogContext(packet.correlationId()) {

            val grunnlagListe = packet["grunnlag"].toString()
            try {
                val grunnlag = objectMapper.readValue<List<VilkaarOpplysning<ObjectNode>>>(grunnlagListe)
                val gyldighetsVurdering = gyldigSoeknad.mapOpplysninger(grunnlag)
                logger.info("Gyldighetsvurdering I lesGyldigsoeknad: {}", gyldighetsVurdering)
                packet["@gyldighetsvurdering"] = gyldighetsVurdering
                context.publish(packet.toJson())
                //TODO
                logger.info("Vurdert gyldighet av søknad")
            } catch (e: Exception){
                println("Gyldighetsvurdering feilet " +e)
            }


        }
}

private fun JsonMessage.correlationId(): String? = get("@correlation_id").textValue()
