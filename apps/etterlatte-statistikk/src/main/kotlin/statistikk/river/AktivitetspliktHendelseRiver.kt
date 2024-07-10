package no.nav.etterlatte.statistikk.river

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.etterlatte.libs.common.aktivitetsplikt.AKTIVITETSPLIKT_DTO_RIVER_KEY
import no.nav.etterlatte.libs.common.aktivitetsplikt.AktivitetspliktDto
import no.nav.etterlatte.libs.common.aktivitetsplikt.AktivitetspliktHendelse
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.rapidsandrivers.correlationId
import no.nav.etterlatte.rapidsandrivers.ListenerMedLogging
import no.nav.etterlatte.statistikk.service.AktivitetspliktService
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import org.slf4j.LoggerFactory

class AktivitetspliktHendelseRiver(
    rapidsConnection: RapidsConnection,
    private val aktivitetspliktService: AktivitetspliktService,
) : ListenerMedLogging() {
    private val logger = LoggerFactory.getLogger(this.javaClass)

    init {
        initialiserRiver(rapidsConnection, AktivitetspliktHendelse.OPPDATERT) {
            validate { it.requireKey(AKTIVITETSPLIKT_DTO_RIVER_KEY) }
        }
    }

    override fun haandterPakke(
        packet: JsonMessage,
        context: MessageContext,
    ) = try {
        val aktivitetspliktDto: AktivitetspliktDto = objectMapper.readValue(packet[AKTIVITETSPLIKT_DTO_RIVER_KEY].toString())
        aktivitetspliktService.oppdaterVurderingAktivitetsplikt(aktivitetspliktDto, null)
    } catch (e: Exception) {
        logger.error(
            """
            Kunne ikke lese oppdatert melding om aktivitetsplikt for i pakken med ${packet.correlationId}. 
            Dette betyr at vi ikke får utledet riktig statistikk for aktivitetsplikt for saken det gjelder,
            med mindre en senere oppdatering løser det.
            
            Informasjonen kan eventuelt patches inn.
            """.trimIndent(),
            e,
        )
    }
}
