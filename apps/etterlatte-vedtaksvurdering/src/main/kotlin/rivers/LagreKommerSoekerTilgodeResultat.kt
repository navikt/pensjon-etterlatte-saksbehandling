package no.nav.etterlatte.rivers

import no.nav.etterlatte.VedtaksvurderingService
import no.nav.etterlatte.libs.common.logging.withLogContext
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.vikaar.KommerSoekerTilgode
import no.nav.etterlatte.libs.common.vikaar.VilkaarResultat
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import org.slf4j.LoggerFactory
import java.util.*

internal class LagreKommerSoekerTilgodeResultat(
    rapidsConnection: RapidsConnection,
    val vedtaksvurderingService: VedtaksvurderingService
) : River.PacketListener {
    private val logger = LoggerFactory.getLogger(LagreKommerSoekerTilgodeResultat::class.java)

    init {
        River(rapidsConnection).apply {
            validate { it.demandValue("@event","BEHANDLING:GRUNNLAGENDRET") }
            validate { it.requireKey("sak") }
            validate { it.requireKey("id") }
            validate { it.requireKey("@kommersoekertilgode") }
            validate { it.interestedIn("@correlation_id") }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) =
        withLogContext(packet.correlationId()) {
            val behandlingId = UUID.fromString(packet["id"].asText())
            val sakId = packet["sak"].toString()
            val kommerSoekerTilgodeResultat = objectMapper.readValue(packet["@kommersoekertilgode"].toString(), KommerSoekerTilgode::class.java)
            try {
                vedtaksvurderingService.lagreKommerSoekerTilgodeResultat(sakId, behandlingId, kommerSoekerTilgodeResultat)
            } catch (e: Exception){
                println("spiser en melding fordi: " +e)
            }

        }
}

private fun JsonMessage.correlationId(): String? = get("@correlation_id").textValue()