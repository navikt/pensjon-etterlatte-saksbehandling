package no.nav.etterlatte.rivers

import no.nav.etterlatte.KanIkkeEndreFattetVedtak
import no.nav.etterlatte.VedtaksvurderingService
import no.nav.etterlatte.libs.common.logging.withLogContext
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.vikaar.VilkaarResultat
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import org.slf4j.LoggerFactory
import java.time.YearMonth

internal class  LagreVilkaarsresultat(
    rapidsConnection: RapidsConnection,
    val vedtaksvurderingService: VedtaksvurderingService
) : River.PacketListener {
    private val logger = LoggerFactory.getLogger(LagreVilkaarsresultat::class.java)

    init {
        River(rapidsConnection).apply {
            validate { it.demandAny("@event", listOf("BEHANDLING:OPPRETTET", "BEHANDLING:GRUNNLAGENDRET")) }
            validate { it.requireKey("sak") }
            validate { it.requireKey("id") }
            validate { it.requireKey("soeker") }
            validate { it.requireKey("@virkningstidspunkt") }
            validate { it.requireKey("@vilkaarsvurdering") }
            validate { it.interestedIn("@correlation_id") }
            validate { it.rejectKey("@beregning") }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) =
        withLogContext(packet.correlationId()) {
            val behandlingId = packet["id"].asUUID()
            val sakId = packet["sak"].toString()
            val vilkaarsResultat = objectMapper.readValue(packet["@vilkaarsvurdering"].toString(), VilkaarResultat::class.java)
            try {
                vedtaksvurderingService.lagreVilkaarsresultat(sakId, behandlingId, packet["soeker"].textValue(), vilkaarsResultat, YearMonth.parse(packet["@virkningstidspunkt"].textValue()).atDay(1) )
                requireNotNull( vedtaksvurderingService.hentVedtak(sakId, behandlingId)).also {
                    context.publish(JsonMessage.newMessage(
                        mapOf(
                            "@event" to "VEDTAK:VILKAARSVURDERT",
                            "@sakId" to it.sakId.toLong(),
                            "@behandlingId" to it.behandlingId.toString(),
                            "@vedtakId" to it.id,
                            "@eventtimestamp" to Tidspunkt.now(),
                        )
                    ).toJson())
                }
            }catch (e: KanIkkeEndreFattetVedtak){
                packet["@event"] = "VEDTAK:ENDRING_FORKASTET"
                packet["@vedtakId"] = e.vedtakId
                packet["@forklaring"] = "Vilkaarsvurdering forkastet fordi vedtak allerede er fattet"
                context.publish(
                    packet.toJson()
                )
            } catch (e: Exception){
                logger.warn("Kunne ikke oppdatere vedtak",e)
            }

        }
}


