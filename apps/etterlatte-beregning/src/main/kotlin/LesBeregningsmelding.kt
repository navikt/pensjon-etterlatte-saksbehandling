
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.etterlatte.libs.common.logging.withLogContext
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.rapidsandrivers.correlationId
import no.nav.etterlatte.libs.common.rapidsandrivers.eventName
import no.nav.etterlatte.model.BeregningService
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import org.slf4j.LoggerFactory
import java.time.YearMonth

internal class LesBeregningsmelding(
    rapidsConnection: RapidsConnection,
    private val beregning: BeregningService
) : River.PacketListener {
    private val logger = LoggerFactory.getLogger(LesBeregningsmelding::class.java)

    init {
        River(rapidsConnection).apply {
            eventName("BEHANDLING:GRUNNLAGENDRET")
            validate { it.requireKey("grunnlag") }
            validate { it.requireKey("vilkaarsvurdering") }
            validate { it.requireKey("virkningstidspunkt") }
            validate { it.rejectKey("beregning") }
            correlationId()

        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) =
        withLogContext(packet.correlationId()) {

            val grunnlag = packet["grunnlag"].toString()
            try {
                //TODO fremtidig funksjonalitet for å støtte periodisering av vilkaar
                val tom = YearMonth.now().plusMonths(3)

                val beregningsResultat = beregning.beregnResultat(objectMapper.readValue(grunnlag),
                    YearMonth.parse(packet["virkningstidspunkt"].asText()),
                    tom)
                packet["beregning"] = beregningsResultat
                context.publish(packet.toJson())
                logger.info("Publisert en beregning")
            } catch (e: Exception){
                //TODO endre denne
                println("spiser en melding fordi: $e")
            }


        }
}

private fun JsonMessage.correlationId(): String? = get("@correlation_id").textValue()
