package no.nav.etterlatte.statistikk.river

import com.fasterxml.jackson.module.kotlin.treeToValue
import no.nav.etterlatte.libs.common.behandling.BehandlingHendelseType
import no.nav.etterlatte.libs.common.behandling.STATISTIKKBEHANDLING_RIVER_KEY
import no.nav.etterlatte.libs.common.behandling.StatistikkBehandling
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.rapidsandrivers.EVENT_NAME_KEY
import no.nav.etterlatte.libs.common.rapidsandrivers.TEKNISK_TID_KEY
import no.nav.etterlatte.libs.common.rapidsandrivers.correlationId
import no.nav.etterlatte.libs.common.rapidsandrivers.lagParMedEventNameKey
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.rapidsandrivers.ListenerMedLogging
import no.nav.etterlatte.statistikk.service.StatistikkService
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import org.slf4j.LoggerFactory

class AvbruttOpprettetBehandlinghendelseRiver(
    rapidsConnection: RapidsConnection,
    private val service: StatistikkService,
) : ListenerMedLogging() {
    private val opprettetAvbruttHendelser =
        listOf(
            BehandlingHendelseType.OPPRETTET.lagEventnameForType(),
            BehandlingHendelseType.AVBRUTT.lagEventnameForType(),
        )

    private val logger = LoggerFactory.getLogger(this::class.java)

    private val behandlingIdKey = "$STATISTIKKBEHANDLING_RIVER_KEY.id"

    init {
        initialiserRiverUtenEventName(rapidsConnection) {
            validate { it.demandAny(EVENT_NAME_KEY, opprettetAvbruttHendelser) }
            validate { it.requireKey(STATISTIKKBEHANDLING_RIVER_KEY) }
            validate { it.requireKey(TEKNISK_TID_KEY) }
            validate { it.interestedIn(behandlingIdKey) }
        }
    }

    override fun haandterPakke(
        packet: JsonMessage,
        context: MessageContext,
    ) = try {
        val behandling: StatistikkBehandling =
            objectMapper.treeToValue(packet[STATISTIKKBEHANDLING_RIVER_KEY])
        val hendelse: BehandlingHendelseType = enumValueOf(packet[EVENT_NAME_KEY].textValue().split(":")[1])
        val tekniskTid = parseTekniskTid(packet, logger)

        service
            .registrerStatistikkForBehandlinghendelse(behandling, hendelse, tekniskTid)
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
            Kunne ikke mappe ut statistikk for behandlingen i pakken med korrelasjonsid ${packet.correlationId}. 
            Dette betyr at vi ikke får oppdatert saksstatistikk for denne saken, og stopper videre 
            prosessering av statistikk. Må sees på snarest!
            """.trimIndent(),
            e,
        )
        logger.error("Feilet på behandlingid ${packet[behandlingIdKey]}")
        throw e
    }
}
