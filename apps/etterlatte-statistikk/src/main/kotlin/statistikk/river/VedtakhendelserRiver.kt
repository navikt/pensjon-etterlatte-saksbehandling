package no.nav.etterlatte.statistikk.river
import com.fasterxml.jackson.module.kotlin.treeToValue
import no.nav.etterlatte.libs.common.logging.withLogContext
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.rapidsandrivers.correlationId
import no.nav.etterlatte.libs.common.rapidsandrivers.eventNameKey
import no.nav.etterlatte.libs.common.vedtak.KafkaHendelseType
import no.nav.etterlatte.statistikk.service.StatistikkService
import no.nav.etterlatte.statistikk.service.VedtakHendelse
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import org.slf4j.LoggerFactory

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

    val logger = LoggerFactory.getLogger(this::class.java)
    init {
        River(rapidsConnection).apply {
            validate { it.demandAny(eventNameKey, vedtakshendelser) }
            validate { it.requireKey("vedtak") }
            correlationId()
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) =
        withLogContext(packet.correlationId) {
            try {
                val vedtakshendelse = enumValueOf<VedtakHendelse>(packet[eventNameKey].textValue().split(":")[1])
                service.registrerStatistikkForVedtak(objectMapper.treeToValue(packet["vedtak"]), vedtakshendelse)
                    .also { (sakRad, stoenadRad) ->
                        if (sakRad == null && stoenadRad == null) {
                            logger.info(
                                "Ingen statistikk registrert for pakken med korrelasjonsid ${packet.correlationId}"
                            )
                            return@also
                        }
                        context.publish(
                            objectMapper.writeValueAsString(
                                listOfNotNull(
                                    "@event_name" to "STATISTIKK:REGISTRERT",
                                    sakRad?.let { "sak_rad" to objectMapper.writeValueAsString(it) },
                                    stoenadRad?.let { "stoenad_rad" to objectMapper.writeValueAsString(it) }
                                ).toMap()
                            )
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