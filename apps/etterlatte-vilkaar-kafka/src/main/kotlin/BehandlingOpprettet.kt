
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.etterlatte.libs.common.behandling.Behandlingsopplysning
import no.nav.etterlatte.libs.common.logging.withLogContext
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.toJson
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import org.slf4j.LoggerFactory
import vilkaar.HendelseBehandlingOpprettet
import vilkaar.HendelseGrunnlagsbehov
import vilkaar.HendelseVilkaarsvureringOpprettet
import vilkaar.VurderVilkaar
import java.util.UUID

internal class BehandlingOpprettet(
    rapidsConnection: RapidsConnection,
    private val vilkaar: VurderVilkaar
) : River.PacketListener {
    private val logger = LoggerFactory.getLogger(LesVilkaarsmelding::class.java)
    init {
        River(rapidsConnection).apply {
            validate { it.demandValue("@event", "BEHANDLING:OPPRETTET") }
            validate { it.requireKey("grunnlag", "id") }
            validate { it.rejectKey("@vilkaarsvurdering") }
            validate { it.interestedIn("@correlation_id") }
        }.register(this)
    }
    override fun onPacket(packet: JsonMessage, context: MessageContext) =
        withLogContext(packet.correlationId()) {
            val grunnlagListe: List<Behandlingsopplysning<ObjectNode>> = objectMapper.readValue(packet["grunnlag"].toJson())!!
            val behandling = UUID.fromString(packet["id"].textValue())
            try {
                vilkaar.handleHendelse(HendelseBehandlingOpprettet(behandling, grunnlagListe)).forEach { hendelse ->
                    if(hendelse is HendelseVilkaarsvureringOpprettet) {
                        packet["@vilkaarsvurdering"] = hendelse.vurderVilkaar
                        context.publish(packet.toJson())
                        logger.info("Vurdert Vilkår")

                    }
                    if (hendelse is HendelseGrunnlagsbehov){
                        context.publish(JsonMessage.newMessage(
                            mapOf<String, Any>(
                                "@behov" to hendelse.grunnlagstype,
                                "behandling" to hendelse.behandling
                            ).let {
                                hendelse.person?.let {person -> it + ("fnr" to person) }?: it
                            }
                        ).toJson())
                    }
                }

            } catch (e: Exception){
                //TODO endre denne
                println("spiser en melding fordi: " +e)
            }


        }
}

private fun JsonMessage.correlationId(): String? = get("@correlation_id").textValue()