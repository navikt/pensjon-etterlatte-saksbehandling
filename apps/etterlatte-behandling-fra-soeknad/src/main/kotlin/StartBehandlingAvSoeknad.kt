package no.nav.etterlatte

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import org.slf4j.LoggerFactory
import java.util.*

internal class StartBehandlingAvSoeknad(
    rapidsConnection: RapidsConnection,
    private val saker:Sak,
    private val behandlinger: Behandling
) : River.PacketListener {

    init {
        River(rapidsConnection).apply {
            validate { it.demandValue("@event_name", "ey_fordelt") }
            validate { it.requireKey("@skjema_info") }
            validate { it.requireKey("@lagret_soeknad_id") }
            validate { it.requireKey("@fnr_soeker") }
            validate { it.requireValue("@soeknad_fordelt", true) }
            validate { it.rejectKey("@sak_id") }
            validate { it.rejectKey("@behandling_id") }

        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        val sak = saker.skaffSak(packet["@fnr_soeker"].asText(), packet["@skjema_info"]["type"].asText())
        behandlinger.initierBehandling(sak, packet["@skjema_info"], packet["@lagret_soeknad_id"].longValue())

        packet["@sak_id"] = sak
        //packet["@behandling_id"] = behandlingid

        context.publish(packet.toJson())

    }
}

interface Sak {
    fun skaffSak(person:String, saktype:String): Long
}
interface Behandling {
    fun initierBehandling(sak: Long, jsonNode: JsonNode, jsonNode1: Long): UUID
}