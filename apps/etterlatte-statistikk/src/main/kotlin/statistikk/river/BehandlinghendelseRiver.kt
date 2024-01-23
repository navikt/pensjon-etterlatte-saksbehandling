package no.nav.etterlatte.statistikk.river

import com.fasterxml.jackson.module.kotlin.treeToValue
import no.nav.etterlatte.libs.common.behandling.BEHANDLING_RIVER_KEY
import no.nav.etterlatte.libs.common.behandling.BehandlingHendelseType
import no.nav.etterlatte.libs.common.behandling.StatistikkBehandling
import no.nav.etterlatte.libs.common.behandling.lagEventnameForType
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.rapidsandrivers.EVENT_NAME_KEY
import no.nav.etterlatte.libs.common.rapidsandrivers.TEKNISK_TID_KEY
import no.nav.etterlatte.libs.common.rapidsandrivers.correlationId
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.statistikk.service.StatistikkService
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import org.slf4j.LoggerFactory
import rapidsandrivers.migrering.ListenerMedLogging

class BehandlinghendelseRiver(
    rapidsConnection: RapidsConnection,
    private val service: StatistikkService,
) : ListenerMedLogging() {
    private val behandlingshendelser = BehandlingHendelseType.entries.map { it.lagEventnameForType() }

    private val logger = LoggerFactory.getLogger(this::class.java)

    init {
        initialiserRiverUtenEventName(rapidsConnection) {
            validate { it.demandAny(EVENT_NAME_KEY, behandlingshendelser) }
            validate { it.interestedIn(BEHANDLING_RIVER_KEY) }
            validate { it.requireKey("behandling.id") }
            validate { it.requireKey("behandling.sak.id") }
            validate { it.requireKey("behandling.sak.ident") }
            validate { it.requireKey("behandling.behandlingOpprettet") }
            validate { it.requireKey("behandling.sistEndret") }
            validate { it.requireKey("behandling.status") }
            validate { it.requireKey("behandling.type") }
            validate { it.interestedIn(TEKNISK_TID_KEY) }
        }
    }

    override fun haandterPakke(
        packet: JsonMessage,
        context: MessageContext,
    ) = try {
        val behandling: StatistikkBehandling =
            objectMapper.treeToValue(packet[BEHANDLING_RIVER_KEY])
        val hendelse: BehandlingHendelse = enumValueOf(packet[EVENT_NAME_KEY].textValue().split(":")[1])
        val tekniskTid = parseTekniskTid(packet, logger)

        service.registrerStatistikkForBehandlinghendelse(behandling, hendelse, tekniskTid)
            ?.also {
                context.publish(
                    mapOf(
                        "@event_name" to "STATISTIKK:REGISTRERT",
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
        logger.error("Feilet på behandlingid ${packet["behandling.id"]}")
        throw e
    }
}

enum class BehandlingHendelse {
    OPPRETTET,
    AVBRUTT,
}
