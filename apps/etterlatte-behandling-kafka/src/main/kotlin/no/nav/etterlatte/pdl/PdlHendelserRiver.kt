package no.nav.etterlatte.pdl

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import no.nav.etterlatte.behandling.BehandlingService
import no.nav.etterlatte.libs.common.isProd
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.pdlhendelse.Adressebeskyttelse
import no.nav.etterlatte.libs.common.pdlhendelse.Bostedsadresse
import no.nav.etterlatte.libs.common.pdlhendelse.DoedshendelsePdl
import no.nav.etterlatte.libs.common.pdlhendelse.ForelderBarnRelasjonHendelse
import no.nav.etterlatte.libs.common.pdlhendelse.PdlHendelserKeys
import no.nav.etterlatte.libs.common.pdlhendelse.SivilstandHendelse
import no.nav.etterlatte.libs.common.pdlhendelse.UtflyttingsHendelse
import no.nav.etterlatte.libs.common.pdlhendelse.VergeMaalEllerFremtidsfullmakt
import no.nav.etterlatte.libs.common.person.maskerFnr
import no.nav.etterlatte.pdl.hendelse.LeesahOpplysningstype
import no.nav.etterlatte.pdl.hendelse.LeesahOpplysningstype.ADRESSEBESKYTTELSE_V1
import no.nav.etterlatte.pdl.hendelse.LeesahOpplysningstype.BOSTEDSADRESSE_V1
import no.nav.etterlatte.pdl.hendelse.LeesahOpplysningstype.DOEDSFALL_V1
import no.nav.etterlatte.pdl.hendelse.LeesahOpplysningstype.FOLKEREGISTERIDENTIFIKATOR_V1
import no.nav.etterlatte.pdl.hendelse.LeesahOpplysningstype.FORELDERBARNRELASJON_V1
import no.nav.etterlatte.pdl.hendelse.LeesahOpplysningstype.SIVILSTAND_V1
import no.nav.etterlatte.pdl.hendelse.LeesahOpplysningstype.UTFLYTTING_FRA_NORGE
import no.nav.etterlatte.pdl.hendelse.LeesahOpplysningstype.VERGEMAAL_ELLER_FREMTIDSFULLMAKT_V1
import no.nav.etterlatte.rapidsandrivers.HENDELSE_DATA_KEY
import no.nav.etterlatte.rapidsandrivers.ListenerMedLogging
import org.slf4j.LoggerFactory
import tools.jackson.module.kotlin.readValue

internal class PdlHendelserRiver(
    rapidsConnection: RapidsConnection,
    private val behandlinger: BehandlingService,
) : ListenerMedLogging() {
    private val logger = LoggerFactory.getLogger(this::class.java)

    init {
        initialiserRiver(rapidsConnection, PdlHendelserKeys.PERSONHENDELSE) {
            validate { it.requireKey("hendelse") }
            validate { it.requireKey(HENDELSE_DATA_KEY) }
        }
    }

    override fun haandterPakke(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        logger.info("Mottatt hendelse fra pdl: ${packet["hendelse"]}")
        try {
            val hendelse =
                packet["hendelse"]
                    .asText()
                    .takeIf { LeesahOpplysningstype.entries.map { it.name }.contains(it) }
                    ?.let { LeesahOpplysningstype.valueOf(it) }
            when (hendelse) {
                DOEDSFALL_V1 -> {
                    val doedshendelse: DoedshendelsePdl = objectMapper.readValue(packet[HENDELSE_DATA_KEY].toString())
                    logger.info("Doedshendelse mottatt for ${doedshendelse.fnr.maskerFnr()}")
                    behandlinger.sendDoedshendelse(doedshendelse)
                }

                UTFLYTTING_FRA_NORGE -> {
                    val utflyttingsHendelse: UtflyttingsHendelse = objectMapper.readValue(packet[HENDELSE_DATA_KEY].toString())
                    logger.info("Utflyttingshendelse mottatt for ${utflyttingsHendelse.fnr.maskerFnr()}")
                    behandlinger.sendUtflyttingshendelse(utflyttingsHendelse)
                }

                FORELDERBARNRELASJON_V1 -> {
                    val forelderBarnRelasjon: ForelderBarnRelasjonHendelse =
                        objectMapper.readValue(packet[HENDELSE_DATA_KEY].toString())
                    logger.info("Forelder-barn-relasjon mottatt for ${forelderBarnRelasjon.fnr.maskerFnr()}")
                    behandlinger.sendForelderBarnRelasjonHendelse(forelderBarnRelasjon)
                }

                ADRESSEBESKYTTELSE_V1 -> {
                    val adressebeskyttelse: Adressebeskyttelse =
                        objectMapper.readValue(packet[HENDELSE_DATA_KEY].toString())
                    logger.info("Adressebeskyttelse mottatt")
                    behandlinger.sendAdressebeskyttelseHendelse(adressebeskyttelse)
                }

                BOSTEDSADRESSE_V1 -> {
                    val bostedsadresse: Bostedsadresse =
                        objectMapper.readValue(packet[HENDELSE_DATA_KEY].toString())
                    logger.info("Bostedsadresse mottatt for ${bostedsadresse.fnr.maskerFnr()}")
                    behandlinger.sendAdresseHendelse(bostedsadresse)
                }

                VERGEMAAL_ELLER_FREMTIDSFULLMAKT_V1 -> {
                    logger.info("Veregmaal eller fremtidsfullmakt mottatt")
                    val vergeMaalEllerFremtidsfullmakt: VergeMaalEllerFremtidsfullmakt =
                        objectMapper.readValue(packet[HENDELSE_DATA_KEY].toString())
                    behandlinger.sendVergeMaalEllerFremtidsfullmakt(vergeMaalEllerFremtidsfullmakt)
                }

                SIVILSTAND_V1 -> {
                    logger.info("Sivilstand mottatt")
                    val sivilstandHendelse: SivilstandHendelse =
                        objectMapper.readValue(packet[HENDELSE_DATA_KEY].toString())
                    behandlinger.sendSivilstandHendelse(sivilstandHendelse)
                }

                FOLKEREGISTERIDENTIFIKATOR_V1 -> {
                    logger.info("Folkeregisteridentifikator mottatt")
                    behandlinger.sendFolkeregisteridentifikatorhendelse(objectMapper.readValue(packet[HENDELSE_DATA_KEY].toString()))
                }

                else -> {
                    logger.info("Pdl-hendelsestypen mottatt håndteres ikke av applikasjonen")
                }
            }
        } catch (e: Exception) {
            logger.error("Feil oppstod under lesing / sending av hendelse til behandling ", e)
            if (isProd()) {
                throw e
            } else {
                logger.info("Hopper over melding i DEV for å komme ajour med hendelser.")
            }
        }
    }
}
