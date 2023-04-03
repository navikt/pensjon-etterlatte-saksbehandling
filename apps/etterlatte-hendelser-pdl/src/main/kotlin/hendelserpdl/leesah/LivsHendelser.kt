package no.nav.etterlatte.hendelserpdl.leesah

import no.nav.etterlatte.kafka.JsonMessage
import no.nav.etterlatte.kafka.KafkaProdusent
import no.nav.etterlatte.libs.common.pdlhendelse.Adressebeskyttelse
import no.nav.etterlatte.libs.common.pdlhendelse.AdressebeskyttelseGradering
import no.nav.etterlatte.libs.common.pdlhendelse.Doedshendelse
import no.nav.etterlatte.libs.common.pdlhendelse.Endringstype
import no.nav.etterlatte.libs.common.pdlhendelse.ForelderBarnRelasjonHendelse
import no.nav.etterlatte.libs.common.pdlhendelse.UtflyttingsHendelse
import no.nav.etterlatte.libs.common.pdlhendelse.VergeMaalEllerFremtidsfullmakt
import no.nav.etterlatte.libs.common.person.maskerFnr
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.util.*

interface ILivsHendelserRapid {
    fun personErDod(
        fnr: String,
        doedsdato: String?,
        endringstype: Endringstype
    )

    fun personUtflyttingFraNorge(
        fnr: String,
        tilflyttingsLand: String?,
        tilflyttingsstedIUtlandet: String?,
        utflyttingsdato: String?,
        endringstype: Endringstype
    )

    fun forelderBarnRelasjon(
        fnr: String,
        relatertPersonsIdent: String?,
        relatertPersonsRolle: String?,
        minRolleForPerson: String?,
        relatertPersonUtenFolkeregisteridentifikator: String?,
        endringstype: Endringstype
    )

    fun haandterAdressebeskyttelse(
        fnr: String,
        adressebeskyttelseGradering: AdressebeskyttelseGradering,
        endringstype: Endringstype
    )
    fun haandterVergeoppnevnelse(
        fnr: String,
        vergeIdent: String,
        endringstype: Endringstype
    )
}

class LivsHendelserTilRapid(private val kafkaProduser: KafkaProdusent<String, JsonMessage>) : ILivsHendelserRapid {
    val logger = LoggerFactory.getLogger(this.javaClass)

    override fun haandterVergeoppnevnelse(fnr: String, vergeIdent: String, endringstype: Endringstype) {
        logger.info("Poster at en person med fnr=${fnr.maskerFnr()} har fått verge")
        val vergeMaalEllerFremtidsfullmakt = VergeMaalEllerFremtidsfullmakt(
            fnr = fnr,
            vergeIdent = vergeIdent,
            endringstype = endringstype
        )
        kafkaProduser.publiser(
            UUID.randomUUID().toString(),
            JsonMessage.newMessage(
                "PDL:PERSONHENDELSE",
                mapOf(
                    "hendelse" to LeesahOpplysningstyper.VERGEMAAL_ELLER_FREMTIDSFULLMAKT_V1.toString(),
                    "hendelse_data" to vergeMaalEllerFremtidsfullmakt
                )
            )
        )
    }

    override fun personErDod(fnr: String, doedsdato: String?, endringstype: Endringstype) {
        logger.info("Poster at en person med fnr=${fnr.maskerFnr()} er doed")
        val avdoedDoedsdato = doedsdato.parseDato(fnr, logger)
        val doedshendelse = Doedshendelse(avdoedFnr = fnr, doedsdato = avdoedDoedsdato, endringstype = endringstype)
        kafkaProduser.publiser(
            UUID.randomUUID().toString(),
            JsonMessage.newMessage(
                "PDL:PERSONHENDELSE",
                mapOf(
                    "hendelse" to LeesahOpplysningstyper.DOEDSFALL_V1.toString(),
                    "hendelse_data" to doedshendelse
                )
            )
        )
    }

    override fun personUtflyttingFraNorge(
        fnr: String,
        tilflyttingsLand: String?,
        tilflyttingsstedIUtlandet: String?,
        utflyttingsdato: String?,
        endringstype: Endringstype
    ) {
        logger.info("Poster at en person med fnr=${fnr.maskerFnr()} har flyttet til utlandet")
        val utflyttingsdatoParsed = utflyttingsdato.parseDato(fnr, logger)
        val utflyttingsHendelse = UtflyttingsHendelse(
            fnr = fnr,
            tilflyttingsLand = tilflyttingsLand,
            tilflyttingsstedIUtlandet = tilflyttingsstedIUtlandet,
            utflyttingsdato = utflyttingsdatoParsed,
            endringstype = endringstype

        )
        kafkaProduser.publiser(
            UUID.randomUUID().toString(),
            JsonMessage.newMessage(
                "PDL:PERSONHENDELSE",
                mapOf(
                    "hendelse" to LeesahOpplysningstyper.UTFLYTTING_FRA_NORGE.toString(),
                    "hendelse_data" to utflyttingsHendelse
                )
            )
        )
    }

    override fun forelderBarnRelasjon(
        fnr: String,
        relatertPersonsIdent: String?,
        relatertPersonsRolle: String?,
        minRolleForPerson: String?,
        relatertPersonUtenFolkeregisteridentifikator: String?,
        endringstype: Endringstype
    ) {
        logger.info("Poster at en person med fnr=${fnr.maskerFnr()} har endret forelder-barn-relasjon")
        val forelderBarnRelasjonHendelse = ForelderBarnRelasjonHendelse(
            fnr = fnr,
            relatertPersonsIdent = relatertPersonsIdent,
            relatertPersonsRolle = relatertPersonsRolle,
            minRolleForPerson = minRolleForPerson,
            relatertPersonUtenFolkeregisteridentifikator = relatertPersonUtenFolkeregisteridentifikator,
            endringstype = endringstype
        )
        kafkaProduser.publiser(
            UUID.randomUUID().toString(),
            JsonMessage.newMessage(
                "PDL:PERSONHENDELSE",
                mapOf(
                    "hendelse" to LeesahOpplysningstyper.FORELDERBARNRELASJON_V1.toString(),
                    "hendelse_data" to forelderBarnRelasjonHendelse
                )
            )
        )
    }

    override fun haandterAdressebeskyttelse(
        fnr: String,
        adressebeskyttelseGradering: AdressebeskyttelseGradering,
        endringstype: Endringstype
    ) {
        logger.info("Poster at en person med fnr=${fnr.maskerFnr()} har adressebeskyttelse")
        val adressebeskyttelse = Adressebeskyttelse(
            fnr = fnr,
            adressebeskyttelseGradering = adressebeskyttelseGradering,
            endringstype = endringstype
        )
        kafkaProduser.publiser(
            UUID.randomUUID().toString(),
            JsonMessage.newMessage(
                "PDL:PERSONHENDELSE",
                mapOf(
                    "hendelse" to LeesahOpplysningstyper.ADRESSEBESKYTTELSE_V1.toString(),
                    "hendelse_data" to adressebeskyttelse
                )
            )
        )
    }
}

fun String?.parseDato(fnr: String, logger: Logger): LocalDate? = try {
    this?.let { LocalDate.parse(it) }
} catch (e: Exception) {
    logger.warn(
        "Kunne ikke parse doedsdato for en person med fnr=${fnr.maskerFnr()} " +
            "Verdien for doedsdato er: $this. Vi bruker null som dødsdato.",
        e
    )
    null
}