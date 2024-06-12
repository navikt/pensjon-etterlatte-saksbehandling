package no.nav.etterlatte.statistikk.river

import com.fasterxml.jackson.module.kotlin.treeToValue
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.rapidsandrivers.EVENT_NAME_KEY
import no.nav.etterlatte.libs.common.rapidsandrivers.TEKNISK_TID_KEY
import no.nav.etterlatte.libs.common.rapidsandrivers.correlationId
import no.nav.etterlatte.libs.common.rapidsandrivers.lagParMedEventNameKey
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.libs.common.vedtak.VedtakKafkaHendelseHendelseType
import no.nav.etterlatte.rapidsandrivers.ListenerMedLogging
import no.nav.etterlatte.rapidsandrivers.migrering.FIKS_BREV_MIGRERING
import no.nav.etterlatte.statistikk.service.StatistikkService
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class VedtakhendelserRiver(
    rapidsConnection: RapidsConnection,
    private val service: StatistikkService,
) : ListenerMedLogging() {
    private val vedtakshendelser =
        listOf(
            VedtakKafkaHendelseHendelseType.FATTET.lagEventnameForType(),
            VedtakKafkaHendelseHendelseType.ATTESTERT.lagEventnameForType(),
            VedtakKafkaHendelseHendelseType.UNDERKJENT.lagEventnameForType(),
            VedtakKafkaHendelseHendelseType.IVERKSATT.lagEventnameForType(),
        )

    val logger: Logger = LoggerFactory.getLogger(this::class.java)

    init {
        initialiserRiverUtenEventName(rapidsConnection) {
            validate { it.demandAny(EVENT_NAME_KEY, vedtakshendelser) }
            validate { it.requireKey("vedtak") }
            validate { it.interestedIn(TEKNISK_TID_KEY) }
            validate { it.rejectKey(FIKS_BREV_MIGRERING) }
        }
    }

    override fun haandterPakke(
        packet: JsonMessage,
        context: MessageContext,
    ) = try {
        val vedtakshendelse = enumValueOf<VedtakKafkaHendelseHendelseType>(packet[EVENT_NAME_KEY].textValue().split(":")[1])
        val tekniskTid = parseTekniskTid(packet, logger)
        service
            .registrerStatistikkForVedtak(
                objectMapper.treeToValue(packet["vedtak"]),
                vedtakshendelse,
                tekniskTid,
            ).also { (sakRad, stoenadRad) ->
                if (sakRad == null && stoenadRad == null) {
                    logger.info(
                        "Ingen statistikk registrert for pakken med korrelasjonsid ${packet.correlationId}",
                    )
                    return@also
                }
                context.publish(
                    listOfNotNull(
                        StatistikkhendelseType.REGISTRERT.lagParMedEventNameKey(),
                        sakRad?.let { "sak_rad" to it },
                        stoenadRad?.let { "stoenad_rad" to it },
                    ).toMap()
                        .toJson(),
                )
            }
    } catch (e: Exception) {
        logger.error(
            """Kunne ikke mappe ut statistikk for vedtaket i pakken med korrelasjonsid ${packet.correlationId}. 
                        |Dette betyr at vi ikke får oppdatert statistikken for ytelsen i denne saken, og stopper videre 
                        |prosessering av statistikk. Må sees på snarest!
            """.trimMargin(),
            e,
        )
        throw e
    }
}
