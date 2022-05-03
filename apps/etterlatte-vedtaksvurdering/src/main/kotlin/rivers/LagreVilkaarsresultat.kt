package no.nav.etterlatte.rivers

import no.nav.etterlatte.VedtaksvurderingService
import no.nav.etterlatte.libs.common.logging.withLogContext
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.vikaar.VilkaarResultat
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import org.slf4j.LoggerFactory

internal class LagreVilkaarsresultat(
    rapidsConnection: RapidsConnection,
    val vedtaksvurderingService: VedtaksvurderingService
) : River.PacketListener {
    private val logger = LoggerFactory.getLogger(LagreVilkaarsresultat::class.java)

    init {
        River(rapidsConnection).apply {
            validate { it.demandValue("@event", "BEHANDLING:GRUNNLAGENDRET") }
            validate { it.requireKey("@sak_id") }
            validate { it.requireKey("@behandling_id") }
            validate { it.requireKey("@vilkaarsvurdering") }
            validate { it.interestedIn("@correlation_id") }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) =
        withLogContext(packet.correlationId()) {

            val behandlingId = packet["@behandling_id"].toString()
            val sakId = packet["@sak_id"].toString()
            val vilkaarsResultat = objectMapper.readValue(packet["@vilkaarsvurdering"].toString(), VilkaarResultat::class.java)
            try {
                vedtaksvurderingService.lagreVilkaarsresultat(sakId, behandlingId, vilkaarsResultat)
            } catch (e: Exception){
                //TODO endre denne
                println("spiser en melding fordi: " +e)
            }

        }
}

private fun JsonMessage.correlationId(): String? = get("@correlation_id").textValue()