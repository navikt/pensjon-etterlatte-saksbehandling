package no.nav.etterlatte.rivers

import no.nav.etterlatte.KanIkkeEndreFattetVedtak
import no.nav.etterlatte.VedtaksvurderingService
import no.nav.etterlatte.libs.common.logging.withLogContext
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.vikaar.VilkaarResultat
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import org.slf4j.LoggerFactory
import java.time.LocalDate

internal class LagreVilkaarsresultat(
    rapidsConnection: RapidsConnection,
    val vedtaksvurderingService: VedtaksvurderingService
) : River.PacketListener {
    private val logger = LoggerFactory.getLogger(LagreVilkaarsresultat::class.java)

    init {
        River(rapidsConnection).apply {
            validate { it.demandValue("@event","BEHANDLING:GRUNNLAGENDRET") }
            validate { it.requireKey("sak") }
            validate { it.requireKey("id") }
            validate { it.requireKey("soeker") }
            validate { it.requireKey("@virkningstidspunkt") }
            validate { it.requireKey("@vilkaarsvurdering") }
            validate { it.interestedIn("@correlation_id") }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) =
        withLogContext(packet.correlationId()) {
            val behandlingId = packet["id"].asUUID()
            val sakId = packet["sak"].toString()
            val vilkaarsResultat = objectMapper.readValue(packet["@vilkaarsvurdering"].toString(), VilkaarResultat::class.java)
            try {
                vedtaksvurderingService.lagreVilkaarsresultat(sakId, behandlingId, packet["soeker"].textValue(), vilkaarsResultat, LocalDate.parse(packet["@virkningstidspunkt"].textValue()) )
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


