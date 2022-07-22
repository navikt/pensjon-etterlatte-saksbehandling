package no.nav.etterlatte.rivers

import no.nav.etterlatte.KanIkkeEndreFattetVedtak
import no.nav.etterlatte.VedtaksvurderingService
import no.nav.etterlatte.libs.common.logging.withLogContext
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.rapidsandrivers.correlationId
import no.nav.etterlatte.libs.common.rapidsandrivers.eventNameKey
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
            validate { it.demandAny(eventNameKey, listOf("BEHANDLING:OPPRETTET", "BEHANDLING:GRUNNLAGENDRET")) }
            validate { it.requireKey("sakId") }
            validate { it.requireKey("id") }
            validate { it.requireKey("persongalleri.soeker") }

            validate { it.requireKey("kommersoekertilgode") }
            correlationId()
            validate { it.rejectKey("beregning") }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) =
        withLogContext(packet.correlationId) {
            val behandlingId = packet["id"].asUUID()
            val sakId = packet["sakId"].toString()
            val kommerSoekerTilgodeResultat = objectMapper.readValue(packet["kommersoekertilgode"].toString(),
                KommerSoekerTilgode::class.java)
            try {
                vedtaksvurderingService.lagreKommerSoekerTilgodeResultat(sakId,
                    behandlingId,
                    packet["persongalleri.soeker"].textValue(),
                    kommerSoekerTilgodeResultat)
            } catch (e: KanIkkeEndreFattetVedtak) {
                packet[eventNameKey] = "VEDTAK:ENDRING_FORKASTET"
                packet["vedtakId"] = e.vedtakId
                packet["forklaring"] = "Kommer søker tilgode forkastet fordi vedtak allerede er fattet"
                context.publish(
                    packet.toJson()
                )
            } catch (e: Exception) {
                logger.warn("Kunne ikke oppdatere vedtak", e)
            }

        }
}
