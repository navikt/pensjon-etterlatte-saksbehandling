package no.nav.etterlatte.statistikk.river

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import no.nav.etterlatte.libs.common.Enhetsnummer
import no.nav.etterlatte.libs.common.behandling.BehandlingHendelseType
import no.nav.etterlatte.libs.common.behandling.NY_ENHET_KEY
import no.nav.etterlatte.libs.common.behandling.REFERANSE_ENDRET_ENHET_KEY
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.rapidsandrivers.TEKNISK_TID_KEY
import no.nav.etterlatte.libs.common.rapidsandrivers.correlationId
import no.nav.etterlatte.libs.common.rapidsandrivers.lagParMedEventNameKey
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.rapidsandrivers.ListenerMedLogging
import no.nav.etterlatte.statistikk.service.StatistikkService
import org.slf4j.LoggerFactory
import java.util.UUID

class BehandlingEndretEnhetRiver(
    rapidsConnection: RapidsConnection,
    private val service: StatistikkService,
) : ListenerMedLogging() {
    private val logger = LoggerFactory.getLogger(this::class.java)

    init {
        initialiserRiver(rapidsConnection, BehandlingHendelseType.ENDRET_ENHET) {
            validate { it.requireKey(TEKNISK_TID_KEY) }
            validate { it.requireKey(REFERANSE_ENDRET_ENHET_KEY) }
            validate { it.requireKey(NY_ENHET_KEY) }
        }
    }

    override fun haandterPakke(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        try {
            val tekniskTid = parseTekniskTid(packet, logger)
            val referanse =
                try {
                    UUID.fromString(packet[REFERANSE_ENDRET_ENHET_KEY].textValue())
                } catch (e: Exception) {
                    logger.warn(
                        "Referanse for oppgave med endret enhet er ikke en uuid " +
                            "(fikk ${packet[REFERANSE_ENDRET_ENHET_KEY].textValue()}). " +
                            "Hopper over endret enhet-meldingen.",
                    )
                    return
                }
            val nyEnhet = Enhetsnummer(packet[NY_ENHET_KEY].textValue())

            service
                .registrerEndretEnhetForReferanse(referanse, nyEnhet, tekniskTid)
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
                Kunne ikke mappe ut statistikk for endret enhet-hendelse i pakken med korrelasjonsId ${packet.correlationId}
                """.trimIndent(),
            )
        }
    }
}
