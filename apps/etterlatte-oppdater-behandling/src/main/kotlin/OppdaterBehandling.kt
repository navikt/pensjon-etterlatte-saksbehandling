package no.nav.etterlatte

import no.nav.etterlatte.libs.common.logging.withLogContext
import no.nav.etterlatte.libs.common.vikaar.VilkaarResultat
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import java.util.*

internal class OppdaterBehandling(
    rapidsConnection: RapidsConnection,
    private val behandlinger: Behandling,

    ) : River.PacketListener {

    init {
        River(rapidsConnection).apply {
            validate { it.demandValue("@event", "BEHANDLING:GRUNNLAGENDRET") }
            validate { it.requireKey("grunnlag") }
            validate { it.requireKey("id") }
            validate { it.requireKey("@vilkaarsvurdering") }
            validate { it.interestedIn("@correlation_id") }

        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) =
        withLogContext(packet.correlationId()) {
            behandlinger.leggTilVilkaarsresultat(UUID.fromString(packet["id"].asText()), packet["@vilkaarsvurdering"] as VilkaarResultat)
        }
    }


interface Behandling {
    fun leggTilVilkaarsresultat(behandling: UUID, vilkaarResultat: VilkaarResultat)
}

private fun JsonMessage.correlationId(): String? = get("@correlation_id").textValue()