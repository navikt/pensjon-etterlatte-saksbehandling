package no.nav.etterlatte

import com.fasterxml.jackson.module.kotlin.treeToValue
import no.nav.etterlatte.libs.common.logging.withLogContext
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.pdlhendelse.Doedshendelse
import no.nav.etterlatte.libs.common.pdlhendelse.ForelderBarnRelasjonHendelse
import no.nav.etterlatte.libs.common.pdlhendelse.UtflyttingsHendelse
import no.nav.etterlatte.libs.common.rapidsandrivers.correlationId
import no.nav.etterlatte.libs.common.rapidsandrivers.eventName
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import org.slf4j.LoggerFactory

val hendelseTyper = listOf("DOEDSFALL_V1", "UTFLYTTING_FRA_NORGE", "FORELDERBARNRELASJON_V1")

internal class PdlHendelser(
    rapidsConnection: RapidsConnection,
    private val behandlinger: Behandling

) : River.PacketListener {
    private val logger = LoggerFactory.getLogger(OppdaterBehandling::class.java)

    init {
        logger.info("initierer rapid for pdlHendelser")
        River(rapidsConnection).apply {
            eventName("PDL:PERSONHENDELSE")

            correlationId()
            validate { it.requireAny("hendelse", hendelseTyper) }
            validate { it.interestedIn("hendelse_data") }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) =
        withLogContext(packet.correlationId) {
            logger.info("Mottatt hendelse fra pdl: ${packet["hendelse"]}")
            try {
                when (packet["hendelse"].asText()) {
                    "DOEDSFALL_V1" -> {
                        logger.info("Doedshendelse mottatt")
                        val doedshendelse: Doedshendelse = objectMapper.treeToValue(packet["hendelse_data"])
                        behandlinger.sendDoedshendelse(doedshendelse)
                    }
                    "UTFLYTTING_FRA_NORGE" -> {
                        logger.info("Utflyttingshendelse mottatt")
                        val utflyttingsHendelse: UtflyttingsHendelse = objectMapper.treeToValue(packet["hendelse_data"])
                        behandlinger.sendUtflyttingshendelse(utflyttingsHendelse)
                    }
                    "FORELDERBARNRELASJON_V1" -> {
                        logger.info("Forelder-barn-relasjon mottatt")
                        val forelderBarnRelasjon: ForelderBarnRelasjonHendelse =
                            objectMapper.treeToValue(packet["hendelse_data"])
                        behandlinger.sendForelderBarnRelasjonHendelse(forelderBarnRelasjon)
                    }
                    else -> {
                        logger.info("Pdl-hendelsestypen mottatt håndteres ikke av applikasjonen")
                    }
                }
            } catch (e: Exception) {
                logger.error("Feil oppstod under lesing / sending av hendelse til behandling ", e)
            }
        }
}