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
    private val behandlinger: Behandling
) : River.PacketListener {

    init {
        River(rapidsConnection).apply {
            validate { it.demandValue("@event_name", "ey_fordelt") }
            validate { it.requireKey("@skjema_info") }
            validate { it.requireValue("@skjema_info.versjon", "2") }
            validate { it.requireKey("@lagret_soeknad_id") }
            validate { it.requireKey("@fnr_soeker") }
            validate { it.requireValue("@soeknad_fordelt", true) }
            validate { it.rejectKey("@sak_id") }
            validate { it.rejectKey("@behandling_id") }
            validate { it.interestedIn("@correlation_id") }

        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) =
        withLogContext(packet.correlationId()) {
            val sak = behandlinger.skaffSak(packet["@fnr_soeker"].asText(), packet["@skjema_info"]["type"].asText())
            val behandlingsid = behandlinger.initierBehandling(sak, packet["@skjema_info"], packet["@lagret_soeknad_id"].longValue())

            packet["@sak_id"] = sak
            packet["@behandling_id"] = behandlingsid

            context.publish(packet.toJson())
        }
    }

interface Behandling {
    fun initierBehandling(sak: Long, jsonNode: JsonNode, jsonNode1: Long): UUID
    fun skaffSak(person:String, saktype:String): Long
}

private fun JsonMessage.correlationId(): String? = get("@correlation_id").textValue()