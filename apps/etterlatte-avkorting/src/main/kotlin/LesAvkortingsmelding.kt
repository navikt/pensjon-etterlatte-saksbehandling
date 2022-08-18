import com.fasterxml.jackson.module.kotlin.readValue
import model.AvkortingService
import no.nav.etterlatte.libs.common.logging.withLogContext
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.rapidsandrivers.correlationId
import no.nav.etterlatte.libs.common.rapidsandrivers.eventName
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import org.slf4j.LoggerFactory

internal class LesAvkortingsmelding(
    rapidsConnection: RapidsConnection,
    private val avkorting: AvkortingService
) : River.PacketListener {
    private val logger = LoggerFactory.getLogger(LesAvkortingsmelding::class.java)

    init {
        River(rapidsConnection).apply {
            eventName("BEHANDLING:GRUNNLAGENDRET")
            validate { it.requireKey("grunnlag") }
            validate { it.requireKey("vilkaarsvurdering") }
            validate { it.requireKey("beregning") }
            validate { it.rejectKey("avkorting") }
            correlationId()
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) =
        withLogContext(packet.correlationId) {
            // TODO her må jeg sikkert ha noe anna info
            val beregningsResultat = packet["beregning"].toString()
            try {
                val avkortingsResultat = avkorting.avkortingsResultat(objectMapper.readValue(beregningsResultat))

                packet["avkorting"] = avkortingsResultat
                context.publish(packet.toJson())
                logger.info("Publisert en beregning")
            } catch (e: Exception) {
                // TODO endre denne
                println("spiser en melding fordi: " + e)
            }
        }
}