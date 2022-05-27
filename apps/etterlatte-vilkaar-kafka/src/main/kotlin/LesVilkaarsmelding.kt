import com.fasterxml.jackson.module.kotlin.treeToValue
import no.nav.etterlatte.libs.common.grunnlag.Grunnlag
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
            validate { it.requireKey("@grunnlag") }
            validate { it.rejectKey("@vilkaarsvurdering") }
            validate { it.rejectKey("@kommersoekertilgode") }
            validate { it.rejectKey("@gyldighetsvurdering") }
            validate { it.interestedIn("@correlation_id") }

        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) =
        withLogContext(packet.correlationId()) {
            try {
                val grunnlag = requireNotNull(objectMapper.treeToValue<Grunnlag>(packet["@grunnlag"]))
                val grunnlagForVilkaar = grunnlag.grunnlag.map { VilkaarOpplysning(it.id, it.opplysningType, it.kilde, it.opplysning) }
                val vilkaarsVurdering = vilkaar.mapVilkaar(grunnlagForVilkaar)
                val kommerSoekerTilGodeVurdering = vilkaar.mapKommerSoekerTilGode(grunnlagForVilkaar)

                packet["@vilkaarsvurdering"] = vilkaarsVurdering
                packet["@vilkaarsvurderingGrunnlagRef"] = grunnlag.versjon
                packet["@kommersoekertilgode"] = kommerSoekerTilGodeVurdering
                context.publish(packet.toJson())

                logger.info("Vurdert Vilkår")
            } catch (e: Exception) {
                println("Vilkår kunne ikke vurderes: $e")
            }
        }
}

private fun JsonMessage.correlationId(): String? = get("@correlation_id").textValue()