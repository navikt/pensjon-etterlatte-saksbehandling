package no.nav.etterlatte

import com.fasterxml.jackson.module.kotlin.treeToValue
import no.nav.etterlatte.libs.common.logging.withLogContext
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.pdlhendelse.Adressebeskyttelse
import no.nav.etterlatte.libs.common.pdlhendelse.Doedshendelse
import no.nav.etterlatte.libs.common.pdlhendelse.ForelderBarnRelasjonHendelse
import no.nav.etterlatte.libs.common.pdlhendelse.SivilstandHendelse
import no.nav.etterlatte.libs.common.pdlhendelse.UtflyttingsHendelse
import no.nav.etterlatte.libs.common.pdlhendelse.VergeMaalEllerFremtidsfullmakt
import no.nav.etterlatte.libs.common.rapidsandrivers.correlationId
import no.nav.etterlatte.libs.common.rapidsandrivers.eventName
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import org.slf4j.LoggerFactory

internal class PdlHendelser(
    rapidsConnection: RapidsConnection,
    private val behandlinger: BehandlingService
) : River.PacketListener {
    private val logger = LoggerFactory.getLogger(PdlHendelser::class.java)

    init {
        logger.info("initierer rapid for pdlHendelser")
        River(rapidsConnection).apply {
            eventName("PDL:PERSONHENDELSE")

            correlationId()
            validate { it.requireKey("hendelse") }
            validate { it.requireKey("hendelse_data") }
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
                    "ADRESSEBESKYTTELSE_V1" -> {
                        logger.info("Adressebeskyttelse mottatt")
                        val adressebeskyttelse: Adressebeskyttelse =
                            objectMapper.treeToValue(packet["hendelse_data"])
                        behandlinger.sendAdressebeskyttelseHendelse(adressebeskyttelse)
                    }
                    "VERGEMAAL_ELLER_FREMTIDSFULLMAKT_V1" -> {
                        logger.info("Veregmaal eller fremtidsfullmakt mottatt")
                        val vergeMaalEllerFremtidsfullmakt: VergeMaalEllerFremtidsfullmakt =
                            objectMapper.treeToValue(packet["hendelse_data"])
                        behandlinger.sendVergeMaalEllerFremtidsfullmakt(vergeMaalEllerFremtidsfullmakt)
                    }
                    "SIVILSTAND_V1" -> {
                        logger.info("Sivilstand mottatt")
                        val sivilstandHendelse: SivilstandHendelse =
                            objectMapper.treeToValue(packet["hendelse_data"])
                        behandlinger.sendSivilstandHendelse(sivilstandHendelse)
                    }
                    else -> {
                        logger.info("Pdl-hendelsestypen mottatt håndteres ikke av applikasjonen")
                    }
                }
            } catch (e: Exception) {
                logger.error("Feil oppstod under lesing / sending av hendelse til behandling ", e)
                throw e
            }
        }
}