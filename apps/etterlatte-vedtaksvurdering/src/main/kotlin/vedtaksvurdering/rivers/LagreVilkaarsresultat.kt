package no.nav.etterlatte.vedtaksvurdering.rivers

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.etterlatte.KanIkkeEndreFattetVedtak
import no.nav.etterlatte.VedtaksvurderingService
import no.nav.etterlatte.libs.common.logging.withLogContext
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.rapidsandrivers.correlationId
import no.nav.etterlatte.libs.common.rapidsandrivers.eventNameKey
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.vedtak.Behandling
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingDto
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
            validate { it.demandAny(eventNameKey, listOf("BEHANDLING:OPPRETTET", "BEHANDLING:GRUNNLAGENDRET")) }
            validate { it.requireKey("sak.id") }
            validate { it.requireKey("sak.sakType") }
            validate { it.requireKey("behandling") }
            validate { it.requireKey("fnrSoeker") }
            validate { it.requireKey("virkningstidspunkt") }
            validate { it.requireKey("vilkaarsvurdering") }
            correlationId()
            validate { it.rejectKey("beregning") }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) =
        withLogContext(packet.correlationId) {
            val behandling = objectMapper.readValue<Behandling>(packet["behandling"].toString())

            val sakId = packet["sak.id"].toString()
            val sakType = packet["sak.sakType"].textValue()
            val vilkaarsvurdering: VilkaarsvurderingDto = objectMapper.readValue(
                packet["vilkaarsvurdering"].toString()
            )
            try {
                vedtaksvurderingService.lagreVilkaarsresultat(
                    sakId,
                    sakType,
                    behandling,
                    packet["fnrSoeker"].textValue(),
                    vilkaarsvurdering,
                    vilkaarsvurdering.virkningstidspunkt.dato.atDay(1)
                )
                requireNotNull(vedtaksvurderingService.hentVedtak(sakId, behandling.id)).also {
                    context.publish(
                        JsonMessage.newMessage(
                            mapOf(
                                eventNameKey to "VEDTAK:VILKAARSVURDERT",
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
                packet["forklaring"] = "Vilkaarsvurdering forkastet fordi vedtak allerede er fattet"
                context.publish(
                    packet.toJson()
                )
            } catch (e: Exception) {
                logger.warn("Kunne ikke oppdatere vedtak", e)
            }
        }
}