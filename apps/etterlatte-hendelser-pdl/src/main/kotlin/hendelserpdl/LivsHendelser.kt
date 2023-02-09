package no.nav.etterlatte.hendelserpdl

import no.nav.etterlatte.hendelserpdl.utils.maskerFnr
import no.nav.etterlatte.libs.common.pdlhendelse.Doedshendelse
import no.nav.etterlatte.libs.common.pdlhendelse.Endringstype
import no.nav.etterlatte.libs.common.pdlhendelse.ForelderBarnRelasjonHendelse
import no.nav.etterlatte.libs.common.pdlhendelse.UtflyttingsHendelse
import no.nav.etterlatte.libs.common.rapidsandrivers.eventNameKey
import no.nav.etterlatte.libs.common.toJson
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.util.*

interface IPostLivsHendelserPaaRapid {
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
}

class LivsHendelserRapid(private val context: RapidsConnection) : IPostLivsHendelserPaaRapid {
    val logger = LoggerFactory.getLogger(this.javaClass)

    override fun personErDod(fnr: String, doedsdato: String?, endringstype: Endringstype) {
        logger.info("Poster at en person med fnr=${fnr.maskerFnr()} er doed")
        val avdoedDoedsdato = doedsdato.parseDato(fnr, logger)
        val doedshendelse = Doedshendelse(avdoedFnr = fnr, doedsdato = avdoedDoedsdato, endringstype = endringstype)
        context.publish(
            UUID.randomUUID().toString(),
            JsonMessage.newMessage(
                mapOf(
                    eventNameKey to "PDL:PERSONHENDELSE",
                    "hendelse" to "DOEDSFALL_V1",
                    "hendelse_data" to doedshendelse
                )
            ).toJson()
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
        context.publish(
            UUID.randomUUID().toString(),
            JsonMessage.newMessage(
                mapOf(
                    eventNameKey to "PDL:PERSONHENDELSE",
                    "hendelse" to "UTFLYTTING_FRA_NORGE",
                    "hendelse_data" to utflyttingsHendelse
                )
            )
                .toJson()
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
        context.publish(
            UUID.randomUUID().toString(),
            JsonMessage.newMessage(
                mapOf(
                    eventNameKey to "PDL:PERSONHENDELSE",
                    "hendelse" to "FORELDERBARNRELASJON_V1",
                    "hendelse_data" to forelderBarnRelasjonHendelse
                )
            )
                .toJson()
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