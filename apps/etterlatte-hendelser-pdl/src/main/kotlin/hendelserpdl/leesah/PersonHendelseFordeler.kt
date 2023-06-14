package no.nav.etterlatte.hendelserpdl.leesah

import no.nav.etterlatte.hendelserpdl.leesah.LeesahOpplysningstyper.ADRESSEBESKYTTELSE_V1
import no.nav.etterlatte.hendelserpdl.leesah.LeesahOpplysningstyper.DOEDSFALL_V1
import no.nav.etterlatte.hendelserpdl.leesah.LeesahOpplysningstyper.FORELDERBARNRELASJON_V1
import no.nav.etterlatte.hendelserpdl.leesah.LeesahOpplysningstyper.SIVILSTAND_V1
import no.nav.etterlatte.hendelserpdl.leesah.LeesahOpplysningstyper.UTFLYTTING_FRA_NORGE
import no.nav.etterlatte.hendelserpdl.leesah.LeesahOpplysningstyper.VERGEMAAL_ELLER_FREMTIDSFULLMAKT_V1
import no.nav.etterlatte.hendelserpdl.pdl.Pdl
import no.nav.etterlatte.libs.common.pdlhendelse.Endringstype
import no.nav.etterlatte.libs.common.person.AdressebeskyttelseGradering
import no.nav.etterlatte.libs.common.person.PdlIdentifikator
import no.nav.person.pdl.leesah.Personhendelse
import no.nav.person.pdl.leesah.adressebeskyttelse.Gradering
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.format.DateTimeFormatter

val sikkerLogg: Logger = LoggerFactory.getLogger("sikkerLogg")

class PersonHendelseFordeler(
    private val postHendelser: ILivsHendelserRapid,
    private val pdlService: Pdl
) {
    private val logger: Logger = LoggerFactory.getLogger(PersonHendelseFordeler::class.java)

    suspend fun haandterHendelse(hendelse: Personhendelse) =
        try {
            when (val personnummer = hentPersonnummer(hendelse)) {
                null -> "Mottok en hendelse uten personident (hendelseId=${hendelse.hendelseId})".let {
                    sikkerLogg.info(it, hendelse)
                    logger.info(it)
                }
                is PdlIdentifikator.Npid -> loggIgnorererNpid(hendelse.hendelseId)
                is PdlIdentifikator.FolkeregisterIdent -> {
                    when (hendelse.opplysningstype) {
                        VERGEMAAL_ELLER_FREMTIDSFULLMAKT_V1.toString() -> haandterVergemaal(hendelse, personnummer)
                        ADRESSEBESKYTTELSE_V1.toString() -> haandterAdressebeskyttelse(hendelse, personnummer)
                        FORELDERBARNRELASJON_V1.toString() -> haandterForelderBarnRelasjon(hendelse, personnummer)
                        DOEDSFALL_V1.toString() -> haandterDoedsHendelse(hendelse, personnummer)
                        UTFLYTTING_FRA_NORGE.toString() -> haandterUtflyttingFraNorge(hendelse, personnummer)
                        SIVILSTAND_V1.toString() -> haandterSivilstand(hendelse, personnummer)
                        else -> logger.info("Så en hendelse av type ${hendelse.opplysningstype} som vi ikke håndterer")
                    }
                }
            }
        } catch (e: Exception) {
            loggFeilVedHaandteringAvHendelse(hendelse.hendelseId, hendelse.opplysningstype, e)
            throw e
        }

    private fun haandterVergemaal(hendelse: Personhendelse, personnummer: PdlIdentifikator.FolkeregisterIdent) {
        with(hendelse.vergemaalEllerFremtidsfullmakt) {
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

            postHendelser.haandterVergeoppnevnelse(
                hendelseId = hendelse.hendelseId,
                endringstype = hendelse.endringstype(),
                fnr = personnummer.folkeregisterident.value,
                vergeIdent = vergeEllerFullmektig.motpartsPersonident
            )
        }
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

        hendelse.adressebeskyttelse.let {
            postHendelser.haandterAdressebeskyttelse(
                hendelseId = hendelse.hendelseId,
                endringstype = hendelse.endringstype(),
                fnr = personnummer.folkeregisterident.value,
                adressebeskyttelseGradering = gradering.let {
                    AdressebeskyttelseGradering.valueOf(gradering.toString())
                }
            )
        }
    }

    private fun haandterForelderBarnRelasjon(
        hendelse: Personhendelse,
        personnummer: PdlIdentifikator.FolkeregisterIdent
    ) {
        with(hendelse.forelderBarnRelasjon) {
            postHendelser.forelderBarnRelasjon(
                hendelseId = hendelse.hendelseId,
                endringstype = hendelse.endringstype(),
                fnr = personnummer.folkeregisterident.value,
                relatertPersonsIdent = relatertPersonsIdent,
                relatertPersonsRolle = relatertPersonsRolle,
                minRolleForPerson = minRolleForPerson,
                relatertPersonUtenFolkeregisteridentifikator = relatertPersonUtenFolkeregisteridentifikator?.toString()
            )
        }
    }

    private fun haandterDoedsHendelse(
        hendelse: Personhendelse,
        personnummer: PdlIdentifikator.FolkeregisterIdent
    ) {
        postHendelser.personErDod(
            hendelseId = hendelse.hendelseId,
            endringstype = hendelse.endringstype(),
            fnr = personnummer.folkeregisterident.value,
            doedsdato = hendelse.doedsfall?.doedsdato?.asStringOrNull()
        )
    }

    private fun haandterUtflyttingFraNorge(
        hendelse: Personhendelse,
        personnummer: PdlIdentifikator.FolkeregisterIdent
    ) {
        postHendelser.personUtflyttingFraNorge(
            hendelseId = hendelse.hendelseId,
            endringstype = hendelse.endringstype(),
            fnr = personnummer.folkeregisterident.value,
            tilflyttingsLand = hendelse.utflyttingFraNorge?.tilflyttingsland,
            tilflyttingsstedIUtlandet = hendelse.utflyttingFraNorge?.tilflyttingsstedIUtlandet,
            utflyttingsdato = hendelse.utflyttingFraNorge?.utflyttingsdato?.asStringOrNull()
        )
    }

    private fun haandterSivilstand(
        hendelse: Personhendelse,
        personnummer: PdlIdentifikator.FolkeregisterIdent
    ) {
        with(hendelse.sivilstand) {
            postHendelser.endringSivilstand(
                hendelseId = hendelse.hendelseId,
                endringstype = hendelse.endringstype(),
                fnr = personnummer.folkeregisterident.value,
                type = type,
                relatertVedSivilstand = relatertVedSivilstand,
                gyldigFraOgMed = gyldigFraOgMed?.asStringOrNull(),
                bekreftelsesdato = bekreftelsesdato?.asStringOrNull()
            )
        }
    }

    private suspend fun hentPersonnummer(hendelse: Personhendelse) =
        hendelse.personidenter.firstOrNull()?.let { pdlService.hentPdlIdentifikator(it) }

    private fun LocalDate?.asStringOrNull() = try {
        this?.format(DateTimeFormatter.ISO_DATE)
    } catch (e: Exception) {
        logger.warn("Kunne ikke String-formatere dato")
        null
    }

    private fun Personhendelse.endringstype() = Endringstype.valueOf(this.endringstype.name)

    private fun loggIgnorererNpid(hendelseId: String) =
        logger.info("Ignorerer en hendelse med id=$hendelseId om en person som kun har NPID som identifikator")

    private fun loggFeilVedHaandteringAvHendelse(hendelseId: String, opplysningstype: String, e: Exception) {
        logger.error(
            """
            Kunne ikke haandtere $opplysningstype med id=$hendelseId. Dette skyldes sannsynligvis at 
            personhendelsen ser annerledes ut enn forventet, eller at det var problem med henting av 
            personidentifikatoren fra PDL
            """.trimIndent(),
            e
        )
    }
}