
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
import vilkaar.*
import java.util.UUID

internal class BehovBesvart(
    rapidsConnection: RapidsConnection,
    private val vilkaar: VurderVilkaar
) : River.PacketListener {
    private val handterReturHendleser = HandterVilkarsVurerngHendelse()

    init {
        River(rapidsConnection).apply {
            validate { it.demandAny("@behov", vilkaar.interessantGrunnlag.map { type -> type.name }) }
            validate { it.requireKey("behandling", "opplysning") }
            validate { it.interestedIn("@correlation_id") }
        }.register(this)
    }
    override fun onPacket(packet: JsonMessage, context: MessageContext) =
        withLogContext(packet.correlationId()) {
            val grunnlagListe: Behandlingsopplysning<ObjectNode> = objectMapper.readValue(packet["opplysning"].toJson())!!
            val behandling = UUID.fromString(packet["behandling"].textValue())
            vilkaar.handleHendelse(HendelseNyttGrunnlag(behandling, listOf(grunnlagListe))).forEach { handterReturHendleser.handterHendelse(it, packet, context, behandling) }

        }
}

private fun JsonMessage.correlationId(): String? = get("@correlation_id").textValue()