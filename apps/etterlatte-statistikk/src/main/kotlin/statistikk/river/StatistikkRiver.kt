package no.nav.etterlatte.statistikk.river
import com.fasterxml.jackson.module.kotlin.treeToValue
import no.nav.etterlatte.libs.common.logging.withLogContext
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.rapidsandrivers.correlationId
import no.nav.etterlatte.libs.common.rapidsandrivers.eventName
import no.nav.etterlatte.statistikk.statistikk.StatistikkService
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import org.slf4j.LoggerFactory

class StatistikkRiver(
    rapidsConnection: RapidsConnection,
    private val service: StatistikkService
) : River.PacketListener {

    val logger = LoggerFactory.getLogger(this::class.java)
    init {
        River(rapidsConnection).apply {
            eventName("VEDTAK:ATTESTERT")
            validate { it.requireKey("vedtak") }
            correlationId()
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) =
        withLogContext(packet.correlationId) {
            try {
                service.registrerStatistikkForVedtak(objectMapper.treeToValue(packet["vedtak"]))
                    ?.also {
                        context.publish(
                            objectMapper.writeValueAsString(
                                mapOf(
                                    "@event_name" to "STATISTIKK:REGISTRERT",
                                    "soeknad_rad" to objectMapper.writeValueAsString(it)
                                )
                            )
                        )
                    } ?: logger.info("Registrerte ikke statistikk på grunn av whatever")
            } catch (e: Exception) {
                logger.error(
                    """Kunne ikke mappe ut statistikk for vedtaket i pakken med korrelasjonsid ${packet.correlationId}. 
                        |Dette betyr at vi ikke får oppdatert statistikken for ytelsen i denne saken, og stopper videre 
                        |prosessering av statistikk. Må sees på snarest!
                    """.trimMargin(),
                    e
                )
                throw e
            }
        }
}