package no.nav.etterlatte.rivers

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.etterlatte.KanIkkeEndreFattetVedtak
import no.nav.etterlatte.VedtaksvurderingService
import no.nav.etterlatte.libs.common.avkorting.AvkortingsResultat
import no.nav.etterlatte.libs.common.logging.withLogContext
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import org.slf4j.LoggerFactory

internal class LagreAvkorting(
    rapidsConnection: RapidsConnection,
    val vedtaksvurderingService: VedtaksvurderingService
) : River.PacketListener {
    private val logger = LoggerFactory.getLogger(LagreAvkorting::class.java)

    init {
        River(rapidsConnection).apply {
            validate { it.demandValue("@event", "BEHANDLING:GRUNNLAGENDRET") }
            validate { it.requireKey("sak") }
            validate { it.requireKey("id") }
            validate { it.requireKey("@avkorting") }
            validate { it.requireKey("soeker") }
            validate { it.requireKey("@avkorting") }
            validate { it.interestedIn("@correlation_id") }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) =
        withLogContext(packet.correlationId()) {
            val behandlingId = packet["id"].asUUID()
            val sakId = packet["sak"].toString()
            val avkorting = objectMapper.readValue<AvkortingsResultat>(packet["@avkorting"].toString())

            try {
                vedtaksvurderingService.lagreAvkorting(sakId, behandlingId, packet["soeker"].textValue(), avkorting)
                requireNotNull( vedtaksvurderingService.hentVedtak(sakId, behandlingId)).also {
                    context.publish(JsonMessage.newMessage(
                        mapOf(
                            "@event" to "VEDTAK:AVKORTET",
                            "@sakId" to it.sakId.toLong(),
                            "@behandlingId" to it.behandlingId.toString(),
                            "@vedtakId" to it.id,
                            "@eventtimestamp" to Tidspunkt.now()
                        )
                    ).toJson())
                }
            }catch (e: KanIkkeEndreFattetVedtak){
                packet["@event"] = "VEDTAK:ENDRING_FORKASTET"
                packet["@vedtakId"] = e.vedtakId
                packet["@forklaring"] = "Avkorting forkastet fordi vedtak allerede er fattet"
                context.publish(
                    packet.toJson()
                )
            }
            catch (e: Exception){
                logger.warn("Kunne ikke oppdatere vedtak",e)
            }

        }
}