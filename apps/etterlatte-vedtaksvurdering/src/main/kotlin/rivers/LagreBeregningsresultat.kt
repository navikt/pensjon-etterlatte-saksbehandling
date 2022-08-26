package no.nav.etterlatte.rivers

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.etterlatte.KanIkkeEndreFattetVedtak
import no.nav.etterlatte.VedtaksvurderingService
import no.nav.etterlatte.domene.vedtak.Behandling
import no.nav.etterlatte.libs.common.beregning.BeregningsResultat
import no.nav.etterlatte.libs.common.logging.withLogContext
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.rapidsandrivers.correlationId
import no.nav.etterlatte.libs.common.rapidsandrivers.eventNameKey
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import org.slf4j.LoggerFactory

internal class LagreBeregningsresultat(
    rapidsConnection: RapidsConnection,
    val vedtaksvurderingService: VedtaksvurderingService
) : River.PacketListener {
    private val logger = LoggerFactory.getLogger(LagreBeregningsresultat::class.java)

    init {
        River(rapidsConnection).apply {
            validate { it.demandAny(eventNameKey, listOf("BEHANDLING:OPPRETTET", "BEHANDLING:GRUNNLAGENDRET")) }
            validate { it.requireKey("sakId") }
            validate { it.requireKey("behandling") }
            validate { it.requireKey("fnrSoeker") }
            validate { it.requireKey("beregning") }
            correlationId()
            validate { it.rejectKey("avkorting") }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) =
        withLogContext(packet.correlationId) {
            val behandling = objectMapper.readValue<Behandling>(packet["behandling"].toString())
            val sakId = packet["sakId"].toString()
            val beregningsResultat = objectMapper.readValue(
                packet["beregning"].toString(),
                BeregningsResultat::class.java
            )

            try {
                vedtaksvurderingService.lagreBeregningsresultat(
                    sakId,
                    behandling,
                    packet["fnrSoeker"].textValue(),
                    beregningsResultat
                )
                requireNotNull(vedtaksvurderingService.hentVedtak(sakId, behandling.id)).also {
                    context.publish(
                        JsonMessage.newMessage(
                            mapOf(
                                eventNameKey to "VEDTAK:BEREGNET",
                                "sakId" to it.sakId.toLong(),
                                "behandlingId" to it.behandlingId.toString(),
                                "vedtakId" to it.id,
                                "eventtimestamp" to Tidspunkt.now()
                            )
                        ).toJson()
                    )
                }
            } catch (e: KanIkkeEndreFattetVedtak) {
                packet[eventNameKey] = "VEDTAK:ENDRING_FORKASTET"
                packet["vedtakId"] = e.vedtakId
                packet["forklaring"] = "Beregning forkastet fordi vedtak allerede er fattet"
                context.publish(
                    packet.toJson()
                )
            } catch (e: Exception) {
                logger.warn("Kunne ikke oppdatere vedtak", e)
            }
        }
}