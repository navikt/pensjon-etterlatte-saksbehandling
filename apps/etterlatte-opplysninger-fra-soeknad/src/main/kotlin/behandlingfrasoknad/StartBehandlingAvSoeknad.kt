package no.nav.etterlatte.behandlingfrasoknad

import com.fasterxml.jackson.databind.JsonNode
import no.nav.etterlatte.libs.common.logging.withLogContext
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import java.util.*

internal class StartBehandlingAvSoeknad(
    rapidsConnection: RapidsConnection,
) : River.PacketListener {

    init {
        River(rapidsConnection).apply {
            validate { it.demandValue("@event_name", "ey_fordelt") }
            validate { it.requireKey("@skjema_info") }
            validate { it.requireValue("@skjema_info.versjon", "2") }
            validate { it.requireValue("@soeknad_fordelt", true) }
            validate { it.requireKey("@sak_id") }
            validate { it.requireKey("@behandling_id") }
            validate { it.interestedIn("@correlation_id") }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) =
        withLogContext(packet.correlationId()) {

            // TODO lage melding med opplysninger

            context.publish(packet.toJson())
        }
    }

//TODO slette kobling mot behandling
interface Behandling {
    fun initierBehandling(sak: Long, jsonNode: JsonNode, jsonNode1: Long): UUID
    fun skaffSak(person:String, saktype:String): Long
}

private fun JsonMessage.correlationId(): String? = get("@correlation_id").textValue()