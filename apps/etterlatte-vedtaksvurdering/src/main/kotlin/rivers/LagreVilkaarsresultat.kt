package no.nav.etterlatte.rivers

import no.nav.etterlatte.KanIkkeEndreFattetVedtak
import no.nav.etterlatte.VedtaksvurderingService
import no.nav.etterlatte.libs.common.logging.withLogContext
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.rapidsandrivers.correlationId
import no.nav.etterlatte.libs.common.rapidsandrivers.eventNameKey
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.vikaar.VilkaarResultat
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import org.slf4j.LoggerFactory
import java.time.YearMonth

internal class LagreVilkaarsresultat(
    rapidsConnection: RapidsConnection,
    val vedtaksvurderingService: VedtaksvurderingService
) : River.PacketListener {
    private val logger = LoggerFactory.getLogger(LagreVilkaarsresultat::class.java)

    init {
        River(rapidsConnection).apply {
            validate { it.demandAny(eventNameKey, listOf("BEHANDLING:OPPRETTET", "BEHANDLING:GRUNNLAGENDRET")) }
            validate { it.requireKey("sakId") }
            validate { it.requireKey("behandlingId") }
            validate { it.requireKey("fnrSoeker") }
            validate { it.requireKey("virkningstidspunkt") }
            validate { it.requireKey("vilkaarsvurdering") }
            correlationId()
            validate { it.rejectKey("beregning") }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) =
        withLogContext(packet.correlationId) {
            val behandlingId = packet["behandlingId"].asUUID()
            val sakId = packet["sakId"].toString()
            val vilkaarsResultat = objectMapper.readValue(packet["vilkaarsvurdering"].toString(),
                VilkaarResultat::class.java)
            val virkningstidspunktFraMelding = packet["virkningstidspunkt"].textValue()
            val virkningstidspunkt = if (virkningstidspunktFraMelding != null) YearMonth.parse(virkningstidspunktFraMelding).atDay(1) else null

            try {
                vedtaksvurderingService.lagreVilkaarsresultat(sakId,
                    behandlingId,
                    packet["fnrSoeker"].textValue(),
                    vilkaarsResultat,
                    virkningstidspunkt)
                requireNotNull(vedtaksvurderingService.hentVedtak(sakId, behandlingId)).also {
                    context.publish(JsonMessage.newMessage(
                        mapOf(
                            eventNameKey to "VEDTAK:VILKAARSVURDERT",
                            "sakId" to it.sakId.toLong(),
                            "behandlingId" to it.behandlingId.toString(),
                            "vedtakId" to it.id,
                            "eventtimestamp" to Tidspunkt.now(),
                        )
                    ).toJson())
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


