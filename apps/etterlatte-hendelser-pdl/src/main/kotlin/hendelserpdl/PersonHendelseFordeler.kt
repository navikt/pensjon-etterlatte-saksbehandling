package no.nav.etterlatte.hendelserpdl

import no.nav.etterlatte.hendelserpdl.LeesahOpplysningstype.ADRESSEBESKYTTELSE_V1
import no.nav.etterlatte.hendelserpdl.LeesahOpplysningstype.DOEDSFALL_V1
import no.nav.etterlatte.hendelserpdl.LeesahOpplysningstype.FORELDERBARNRELASJON_V1
import no.nav.etterlatte.hendelserpdl.LeesahOpplysningstype.SIVILSTAND_V1
import no.nav.etterlatte.hendelserpdl.LeesahOpplysningstype.UTFLYTTING_FRA_NORGE
import no.nav.etterlatte.hendelserpdl.LeesahOpplysningstype.VERGEMAAL_ELLER_FREMTIDSFULLMAKT_V1
import no.nav.etterlatte.hendelserpdl.pdl.PdlKlient
import no.nav.etterlatte.kafka.JsonMessage
import no.nav.etterlatte.kafka.KafkaProdusent
import no.nav.etterlatte.libs.common.pdlhendelse.Adressebeskyttelse
import no.nav.etterlatte.libs.common.pdlhendelse.Doedshendelse
import no.nav.etterlatte.libs.common.pdlhendelse.Endringstype
import no.nav.etterlatte.libs.common.pdlhendelse.ForelderBarnRelasjonHendelse
import no.nav.etterlatte.libs.common.pdlhendelse.PdlHendelse
import no.nav.etterlatte.libs.common.pdlhendelse.SivilstandHendelse
import no.nav.etterlatte.libs.common.pdlhendelse.UtflyttingsHendelse
import no.nav.etterlatte.libs.common.pdlhendelse.VergeMaalEllerFremtidsfullmakt
import no.nav.etterlatte.libs.common.person.AdressebeskyttelseGradering
import no.nav.etterlatte.libs.common.person.PdlIdentifikator
import no.nav.etterlatte.libs.common.person.maskerFnr
import no.nav.person.pdl.leesah.Personhendelse
import no.nav.person.pdl.leesah.adressebeskyttelse.Gradering
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*

enum class LeesahOpplysningstype {
    ADRESSEBESKYTTELSE_V1,
    FORELDERBARNRELASJON_V1,
    UTFLYTTING_FRA_NORGE,
    DOEDSFALL_V1,
    VERGEMAAL_ELLER_FREMTIDSFULLMAKT_V1,
    SIVILSTAND_V1
}

class PersonHendelseFordeler(
    private val kafkaProduser: KafkaProdusent<String, JsonMessage>,
    private val pdlKlient: PdlKlient
) {
    private val logger: Logger = LoggerFactory.getLogger(PersonHendelseFordeler::class.java)
    private val sikkerLogg: Logger = LoggerFactory.getLogger("sikkerLogg")

    suspend fun haandterHendelse(hendelse: Personhendelse) {
        if (hendelse.opplysningstype !in opplysningstyperSomHaandteres()) {
            logger.info("Hendelse ${hendelse.opplysningstype} med hendelseId=${hendelse.hendelseId} håndteres ikke")
            return
        }

        val ident = hendelse.personidenter.firstOrNull()?.let {
            pdlKlient.hentPdlIdentifikator(it)
        }

        try {
            when (ident) {
                null -> "Mottok en hendelse uten personident (hendelseId=${hendelse.hendelseId})".let {
                    sikkerLogg.info(it, hendelse)
                    logger.info(it)
                }

                is PdlIdentifikator.Npid -> loggIgnorererNpid(hendelse.hendelseId)
                is PdlIdentifikator.FolkeregisterIdent -> {
                    when (LeesahOpplysningstype.valueOf(hendelse.opplysningstype)) {
                        VERGEMAAL_ELLER_FREMTIDSFULLMAKT_V1 -> haandterVergemaal(hendelse, ident)
                        ADRESSEBESKYTTELSE_V1 -> haandterAdressebeskyttelse(hendelse, ident)
                        FORELDERBARNRELASJON_V1 -> haandterForelderBarnRelasjon(hendelse, ident)
                        DOEDSFALL_V1 -> haandterDoedsHendelse(hendelse, ident)
                        UTFLYTTING_FRA_NORGE -> haandterUtflyttingFraNorge(hendelse, ident)
                        SIVILSTAND_V1 -> haandterSivilstand(hendelse, ident)
                    }
                }
            }
        } catch (e: Exception) {
            loggFeilVedHaandteringAvHendelse(hendelse.hendelseId, hendelse.opplysningstype, e)
            throw e
        }
    }

    private fun haandterVergemaal(hendelse: Personhendelse, personnummer: PdlIdentifikator.FolkeregisterIdent) {
        val type = hendelse.vergemaalEllerFremtidsfullmakt?.type
        if (type !in listOf(
                "ensligMindreaarigAsylsoeker",
                "ensligMindreaarigFlyktning",
                "mindreaarig",
                "midlertidigForMindreaarig",
                "forvaltningUtenforVergemaal"
            )
        ) {
            logger.info("Ignorerer vergemaalEllerFremtidsfullmakt med type=$type")
            return
        }

        publiserPaaRapid(
            opplysningstype = VERGEMAAL_ELLER_FREMTIDSFULLMAKT_V1,
            hendelse = VergeMaalEllerFremtidsfullmakt(
                hendelseId = hendelse.hendelseId,
                endringstype = hendelse.endringstype(),
                fnr = personnummer.folkeregisterident.value,
                vergeIdent = hendelse.vergemaalEllerFremtidsfullmakt?.vergeEllerFullmektig?.motpartsPersonident
            )
        )
    }

    private fun haandterAdressebeskyttelse(
        hendelse: Personhendelse,
        personnummer: PdlIdentifikator.FolkeregisterIdent
    ) {
        val gradering = hendelse.adressebeskyttelse?.gradering
        if (gradering == null || gradering == Gradering.UGRADERT) {
            logger.info("Ignorerer person med tom eller ugradert gradering, krever ingen tiltak.")
            return
        }

        publiserPaaRapid(
            opplysningstype = ADRESSEBESKYTTELSE_V1,
            hendelse = Adressebeskyttelse(
                hendelseId = hendelse.hendelseId,
                endringstype = hendelse.endringstype(),
                fnr = personnummer.folkeregisterident.value,
                adressebeskyttelseGradering = gradering.let {
                    AdressebeskyttelseGradering.valueOf(gradering.toString())
                }
            )
        )
    }

    private fun haandterForelderBarnRelasjon(
        hendelse: Personhendelse,
        personnummer: PdlIdentifikator.FolkeregisterIdent
    ) {
        publiserPaaRapid(
            opplysningstype = FORELDERBARNRELASJON_V1,
            hendelse = ForelderBarnRelasjonHendelse(
                hendelseId = hendelse.hendelseId,
                endringstype = hendelse.endringstype(),
                fnr = personnummer.folkeregisterident.value,
                relatertPersonsIdent = hendelse.forelderBarnRelasjon?.relatertPersonsIdent,
                relatertPersonsRolle = hendelse.forelderBarnRelasjon?.relatertPersonsRolle,
                minRolleForPerson = hendelse.forelderBarnRelasjon?.minRolleForPerson,
                relatertPersonUtenFolkeregisteridentifikator =
                hendelse.forelderBarnRelasjon?.relatertPersonUtenFolkeregisteridentifikator?.toString()
            )
        )
    }

    private fun haandterDoedsHendelse(
        hendelse: Personhendelse,
        personnummer: PdlIdentifikator.FolkeregisterIdent
    ) {
        publiserPaaRapid(
            opplysningstype = DOEDSFALL_V1,
            hendelse = Doedshendelse(
                hendelseId = hendelse.hendelseId,
                endringstype = hendelse.endringstype(),
                fnr = personnummer.folkeregisterident.value,
                doedsdato = hendelse.doedsfall?.doedsdato
            )
        )
    }

    private fun haandterUtflyttingFraNorge(
        hendelse: Personhendelse,
        personnummer: PdlIdentifikator.FolkeregisterIdent
    ) {
        publiserPaaRapid(
            opplysningstype = UTFLYTTING_FRA_NORGE,
            hendelse = UtflyttingsHendelse(
                hendelseId = hendelse.hendelseId,
                endringstype = hendelse.endringstype(),
                fnr = personnummer.folkeregisterident.value,
                tilflyttingsLand = hendelse.utflyttingFraNorge?.tilflyttingsland,
                tilflyttingsstedIUtlandet = hendelse.utflyttingFraNorge?.tilflyttingsstedIUtlandet,
                utflyttingsdato = hendelse.utflyttingFraNorge?.utflyttingsdato
            )
        )
    }

    private fun haandterSivilstand(
        hendelse: Personhendelse,
        personnummer: PdlIdentifikator.FolkeregisterIdent
    ) {
        publiserPaaRapid(
            opplysningstype = SIVILSTAND_V1,
            hendelse = SivilstandHendelse(
                hendelseId = hendelse.hendelseId,
                endringstype = hendelse.endringstype(),
                fnr = personnummer.folkeregisterident.value,
                type = hendelse.sivilstand?.type,
                relatertVedSivilstand = hendelse.sivilstand?.relatertVedSivilstand,
                gyldigFraOgMed = hendelse.sivilstand?.gyldigFraOgMed,
                bekreftelsesdato = hendelse.sivilstand?.bekreftelsesdato
            )
        )
    }

    private fun opplysningstyperSomHaandteres() = LeesahOpplysningstype.values().map { it.toString() }

    private fun publiserPaaRapid(opplysningstype: LeesahOpplysningstype, hendelse: PdlHendelse) {
        logger.info("Publiserer at en person med fnr=${hendelse.fnr.maskerFnr()} har mottatt hendelse $opplysningstype")

        kafkaProduser.publiser(
            noekkel = UUID.randomUUID().toString(),
            verdi = JsonMessage.newMessage(
                eventName = "PDL:PERSONHENDELSE",
                map = mapOf(
                    "hendelse" to opplysningstype.toString(),
                    "hendelse_data" to hendelse
                )
            )
        )
    }

    private fun Personhendelse.endringstype() = Endringstype.valueOf(this.endringstype.name)

    private fun loggIgnorererNpid(hendelseId: String) =
        logger.info("Ignorerer en hendelse med id=$hendelseId om en person som kun har NPID som identifikator")

    private fun loggFeilVedHaandteringAvHendelse(hendelseId: String, opplysningstype: String, e: Exception) {
        logger.error(
            "Kunne ikke haandtere $opplysningstype med id=$hendelseId. Dette skyldes sannsynligvis at " +
                "personhendelsen ser annerledes ut enn forventet.",
            e
        )
    }
}