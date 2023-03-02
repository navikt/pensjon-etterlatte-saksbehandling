package no.nav.etterlatte.statistikk.river

import com.fasterxml.jackson.module.kotlin.treeToValue
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.logging.withLogContext
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.rapidsandrivers.EVENT_NAME_KEY
import no.nav.etterlatte.libs.common.rapidsandrivers.TEKNISK_TID_KEY
import no.nav.etterlatte.libs.common.rapidsandrivers.correlationId
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.statistikk.clients.toPersongalleri
import no.nav.etterlatte.statistikk.service.StatistikkService
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.*

class BehandlinghendelseRiver(
    rapidsConnection: RapidsConnection,
    private val service: StatistikkService
) : River.PacketListener {
    private val behandlingshendelser = listOf(
        "BEHANDLING:AVBRUTT",
        "BEHANDLING:OPPRETTET"
    )

    private val logger = LoggerFactory.getLogger(this::class.java)

    init {
        River(rapidsConnection).apply {
            validate { it.demandAny(EVENT_NAME_KEY, behandlingshendelser) }
            validate { it.interestedIn("behandling") }
            validate { it.requireKey("behandling.id") }
            validate { it.requireKey("behandling.sak") }
            validate { it.requireKey("behandling.behandlingOpprettet") }
            validate { it.requireKey("behandling.sistEndret") }
            validate { it.requireKey("behandling.status") }
            validate { it.requireKey("behandling.type") }
            validate { it.requireKey("behandling.persongalleri") }
            validate { it.interestedIn(TEKNISK_TID_KEY) }
            correlationId()
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) =
        withLogContext(packet.correlationId) {
            try {
                val behandling: DetaljertBehandling = objectMapper.treeToValue(packet["behandling"])
                val behandlingIntern: BehandlingIntern = behandling.toInternBehandling()
                val hendelse: BehandlingHendelse = enumValueOf(packet[EVENT_NAME_KEY].textValue().split(":")[1])
                val tekniskTid = parseTekniskTid(packet, logger)
                service.registrerStatistikkForBehandlinghendelse(behandlingIntern, hendelse, tekniskTid)
                    ?.also {
                        context.publish(
                            mapOf(
                                "@event_name" to "STATISTIKK:REGISTRERT",
                                "sak_rad" to objectMapper.writeValueAsString(it)
                            ).toJson()
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
                logger.error(
                    """
                    Feilet på behandlingid ${packet["behandling.id"]}
                    """.trimIndent()
                )
                throw e
            }
        }
}

data class BehandlingIntern(
    val id: UUID,
    val sakId: Long,
    val behandlingOpprettet: LocalDateTime,
    val sistEndret: LocalDateTime,
    val status: BehandlingStatus,
    val type: BehandlingType,
    val persongalleri: Persongalleri
)

private fun DetaljertBehandling.toInternBehandling() =
    BehandlingIntern(
        id = id,
        sakId = sak,
        behandlingOpprettet = behandlingOpprettet,
        sistEndret = sistEndret,
        status = status,
        type = behandlingType,
        persongalleri = toPersongalleri()
    )

enum class BehandlingHendelse {
    OPPRETTET, AVBRUTT
}