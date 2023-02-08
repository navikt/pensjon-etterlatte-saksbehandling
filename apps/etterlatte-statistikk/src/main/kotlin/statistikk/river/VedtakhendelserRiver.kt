package no.nav.etterlatte.statistikk.river
import com.fasterxml.jackson.module.kotlin.treeToValue
import no.nav.etterlatte.libs.common.logging.withLogContext
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.rapidsandrivers.correlationId
import no.nav.etterlatte.libs.common.rapidsandrivers.eventNameKey
import no.nav.etterlatte.libs.common.rapidsandrivers.tekniskTidKey
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.libs.common.vedtak.KafkaHendelseType
import no.nav.etterlatte.statistikk.service.StatistikkService
import no.nav.etterlatte.statistikk.service.VedtakHendelse
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import org.slf4j.Logger
import org.slf4j.LoggerFactory

fun main() {
    val empty = listOfNotNull<Any>(null)

    val listOfNotNull = listOfNotNull(eventNameKey to "STATISTIKK:REGISTRERT")
    val toMap = listOfNotNull.toMap()
    println(toMap)
}

class VedtakhendelserRiver(
    rapidsConnection: RapidsConnection,
    private val service: StatistikkService
) : River.PacketListener {

    private val vedtakshendelser = listOf(
        KafkaHendelseType.FATTET.toString(),
        KafkaHendelseType.ATTESTERT.toString(),
        KafkaHendelseType.UNDERKJENT.toString(),
        KafkaHendelseType.IVERKSATT.toString()
    )

    val logger: Logger = LoggerFactory.getLogger(this::class.java)
    init {
        River(rapidsConnection).apply {
            validate { it.demandAny(eventNameKey, vedtakshendelser) }
            validate { it.requireKey("vedtak") }
            validate { it.interestedIn(tekniskTidKey) }
            correlationId()
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) =
        withLogContext(packet.correlationId) {
            try {
                val vedtakshendelse = enumValueOf<VedtakHendelse>(packet[eventNameKey].textValue().split(":")[1])
                val tekniskTid = parseTekniskTid(packet, logger)
                service.registrerStatistikkForVedtak(
                    objectMapper.treeToValue(packet["vedtak"]),
                    vedtakshendelse,
                    tekniskTid
                )
                    .also { (sakRad, stoenadRad) ->
                        if (sakRad == null && stoenadRad == null) {
                            logger.info(
                                "Ingen statistikk registrert for pakken med korrelasjonsid ${packet.correlationId}"
                            )
                            return@also
                        }
                        context.publish(
                            listOfNotNull(
                                eventNameKey to "STATISTIKK:REGISTRERT",
                                sakRad?.let { "sak_rad" to it },
                                stoenadRad?.let { "stoenad_rad" to it }
                            ).toMap()
                                .toJson()
                        )
                    }
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