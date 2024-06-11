package no.nav.etterlatte.statistikk.river

import com.fasterxml.jackson.module.kotlin.treeToValue
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.rapidsandrivers.EVENT_NAME_KEY
import no.nav.etterlatte.libs.common.rapidsandrivers.TEKNISK_TID_KEY
import no.nav.etterlatte.libs.common.rapidsandrivers.correlationId
import no.nav.etterlatte.libs.common.rapidsandrivers.lagParMedEventNameKey
import no.nav.etterlatte.libs.common.tilbakekreving.StatistikkTilbakekrevingDto
import no.nav.etterlatte.libs.common.tilbakekreving.TILBAKEKREVING_STATISTIKK_RIVER_KEY
import no.nav.etterlatte.libs.common.tilbakekreving.TilbakekrevingHendelseType
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.rapidsandrivers.ListenerMedLogging
import no.nav.etterlatte.statistikk.service.StatistikkService
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import org.slf4j.LoggerFactory

class TilbakekrevinghendelseRiver(
    rapidsConnection: RapidsConnection,
    private val service: StatistikkService,
) : ListenerMedLogging() {
    private val logger = LoggerFactory.getLogger(this::class.java)

    private val tilbakekrevinghendelser = TilbakekrevingHendelseType.entries.map { it.lagEventnameForType() }

    init {
        initialiserRiverUtenEventName(rapidsConnection) {
            validate { it.demandAny(EVENT_NAME_KEY, tilbakekrevinghendelser) }
            validate { it.requireKey(TILBAKEKREVING_STATISTIKK_RIVER_KEY) }
            validate { it.requireKey(TEKNISK_TID_KEY) }
        }
    }

    override fun haandterPakke(
        packet: JsonMessage,
        context: MessageContext,
    ): Any {
        try {
            val tilbakekreving: StatistikkTilbakekrevingDto = objectMapper.treeToValue(packet[TILBAKEKREVING_STATISTIKK_RIVER_KEY])
            val tekniskTid = parseTekniskTid(packet, logger)
            val hendelse: TilbakekrevingHendelseType = enumValueOf(packet[EVENT_NAME_KEY].textValue().split(":")[1])
            return service
                .registrerStatistikkFortilbakkrevinghendelse(tilbakekreving, tekniskTid, hendelse)
                ?.also {
                    context.publish(
                        mapOf(
                            StatistikkhendelseType.REGISTRERT.lagParMedEventNameKey(),
                            "sak_rad" to objectMapper.writeValueAsString(it),
                        ).toJson(),
                    )
                } ?: logger.info("Ikke registrert statistikk på pakken ${packet.correlationId}")
        } catch (e: Exception) {
            logger.error(
                """
                Kunne ikke mappe ut statistikk for tilbakkrevingen i pakken med korrelasjonsid ${packet.correlationId}. 
                Dette betyr at vi ikke får oppdatert saksstatistikk for denne saken, og stopper videre 
                prosessering av statistikk. Må sees på snarest!
                """.trimIndent(),
                e,
            )
            logger.error("Feilet på tilbakekreving av hendelse ${packet[EVENT_NAME_KEY]}")
            throw e
        }
    }
}
