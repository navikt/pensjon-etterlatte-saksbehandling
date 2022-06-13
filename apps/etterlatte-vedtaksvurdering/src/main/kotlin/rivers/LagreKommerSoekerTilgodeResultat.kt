package no.nav.etterlatte.rivers

import no.nav.etterlatte.KanIkkeEndreFattetVedtak
import no.nav.etterlatte.VedtaksvurderingService
import no.nav.etterlatte.libs.common.logging.withLogContext
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.vikaar.KommerSoekerTilgode
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import org.slf4j.LoggerFactory

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
            validate { it.requireKey("soeker") }

            validate { it.requireKey("@kommersoekertilgode") }
            validate { it.interestedIn("@correlation_id") }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) =
        withLogContext(packet.correlationId()) {
            val behandlingId = packet["id"].asUUID()
            val sakId = packet["sak"].toString()
            val kommerSoekerTilgodeResultat = objectMapper.readValue(packet["@kommersoekertilgode"].toString(), KommerSoekerTilgode::class.java)
            try {
                vedtaksvurderingService.lagreKommerSoekerTilgodeResultat(sakId, behandlingId, packet["soeker"].textValue(), kommerSoekerTilgodeResultat)
            } catch (e: KanIkkeEndreFattetVedtak){
                packet["@event"] = "VEDTAK:ENDRING_FORKASTET"
                packet["@vedtakId"] = e.vedtakId
                packet["@forklaring"] = "Kommer søker tilgode forkastet fordi vedtak allerede er fattet"
                context.publish(
                    packet.toJson()
                )
            } catch (e: Exception){
                logger.warn("Kunne ikke oppdatere vedtak",e)
            }

        }
}