package no.nav.etterlatte.statistikk.river

import com.fasterxml.jackson.module.kotlin.treeToValue
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.logging.withLogContext
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.rapidsandrivers.correlationId
import no.nav.etterlatte.libs.common.rapidsandrivers.eventNameKey
import no.nav.etterlatte.statistikk.service.StatistikkService
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.UUID

class BehandlinghendelseRiver(
    rapidsConnection: RapidsConnection,
    private val service: StatistikkService
) : River.PacketListener {
    val behandlingshendelser = listOf(
        "BEHANDLING:AVBRUTT",
        "BEHANDLING:OPPRETTET"
    )

    val logger = LoggerFactory.getLogger(this::class.java)

    init {
        River(rapidsConnection).apply {
            validate { it.demandAny(eventNameKey, behandlingshendelser) }
            validate { it.requireKey("behandling.id") }
            validate { it.requireKey("behandling.sak") }
            validate { it.requireKey("behandling.behandlingOpprettet") }
            validate { it.requireKey("behandling.sistEndret") }
            validate { it.requireKey("behandling.status") }
            validate { it.requireKey("behandling.type") }
            validate { it.requireKey("behandling.persongalleri") }
            correlationId()
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) =
        withLogContext(packet.correlationId) {
            try {
                val behandling: Behandling = objectMapper.treeToValue(packet["behandling"])
                val hendelse: BehandlingHendelse = enumValueOf(packet[eventNameKey].textValue().split(":")[1])
                service.registrerStatistikkForBehandlinghendelse(behandling, hendelse)
                    ?.also {
                        context.publish(
                            objectMapper.writeValueAsString(
                                mapOf(
                                    "@event_name" to "STATISTIKK:REGISTRERT",
                                    "sak_rad" to objectMapper.writeValueAsString(it)
                                )
                            )
                        )
                    } ?: logger.info("Ikke registrert statistikk på pakken ${packet.correlationId}")
            } catch (e: Exception) {
                logger.error(
                    """
                    Kunne ikke mappe ut statistikk for behandlingen i pakken med korrelasjonsid ${packet.correlationId}. 
                    Dette betyr at vi ikke får oppdatert saksstatistikk for denne saken, og stopper videre 
                    prosessering av statistikk. Må sees på snarest!
                    """.trimIndent(),
                    e
                )
                throw e
            }
        }
}

data class Behandling(
    val id: UUID,
    val sak: Long,
    val behandlingOpprettet: LocalDateTime,
    val sistEndret: LocalDateTime,
    val status: BehandlingStatus,
    val type: BehandlingType,
    val persongalleri: Persongalleri
)

enum class BehandlingHendelse {
    OPPRETTET, AVBRUTT
}