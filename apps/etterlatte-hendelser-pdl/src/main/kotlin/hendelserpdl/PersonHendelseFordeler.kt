package no.nav.etterlatte.hendelserpdl

import no.nav.etterlatte.hendelserpdl.pdl.PdlTjenesterKlient
import no.nav.etterlatte.kafka.JsonMessage
import no.nav.etterlatte.kafka.KafkaProdusent
import no.nav.etterlatte.libs.common.logging.sikkerlogger
import no.nav.etterlatte.libs.common.pdl.FantIkkePersonException
import no.nav.etterlatte.libs.common.pdlhendelse.Adressebeskyttelse
import no.nav.etterlatte.libs.common.pdlhendelse.Bostedsadresse
import no.nav.etterlatte.libs.common.pdlhendelse.DoedshendelsePdl
import no.nav.etterlatte.libs.common.pdlhendelse.Endringstype
import no.nav.etterlatte.libs.common.pdlhendelse.Folkeregisteridentifikatorhendelse
import no.nav.etterlatte.libs.common.pdlhendelse.ForelderBarnRelasjonHendelse
import no.nav.etterlatte.libs.common.pdlhendelse.PdlHendelse
import no.nav.etterlatte.libs.common.pdlhendelse.PdlHendelserKeys
import no.nav.etterlatte.libs.common.pdlhendelse.SivilstandHendelse
import no.nav.etterlatte.libs.common.pdlhendelse.UtflyttingsHendelse
import no.nav.etterlatte.libs.common.pdlhendelse.VergeMaalEllerFremtidsfullmakt
import no.nav.etterlatte.libs.common.person.AdressebeskyttelseGradering
import no.nav.etterlatte.libs.common.person.PdlIdentifikator
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
import no.nav.person.pdl.leesah.Personhendelse
import no.nav.person.pdl.leesah.adressebeskyttelse.Gradering
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.UUID

class PersonHendelseFordeler(
    private val kafkaProduser: KafkaProdusent<String, JsonMessage>,
    private val pdlTjenesterKlient: PdlTjenesterKlient,
) {
    private val logger: Logger = LoggerFactory.getLogger(PersonHendelseFordeler::class.java)
    private val sikkerLogg: Logger = sikkerlogger()

    suspend fun haandterHendelse(hendelse: Personhendelse) {
        if (hendelse.opplysningstype !in opplysningstyperSomHaandteres()) {
            logger.info("Hendelse ${hendelse.opplysningstype} med hendelseId=${hendelse.hendelseId} håndteres ikke")
            return
        }

        val ident =
            hendelse.personidenter.firstOrNull()?.let {
                try {
                    pdlTjenesterKlient.hentPdlIdentifikator(it)
                } catch (e: FantIkkePersonException) {
                    logger.warn(
                        "Mottok en hendelse med en personident vi ikke fant ved oppslag tilbake i PDL. " +
                            "HendelseId=${hendelse.hendelseId}, ved oppslag mot ${it.maskerFnr()} " +
                            "(se sikkerlogg for ident(er))",
                    )
                    sikkerLogg.warn(
                        "Mottok en hendelse med en personident vi ikke fant ved oppslag tilbake i PDL. " +
                            "HendelseId=${hendelse.hendelseId}, identifikator(er): ${hendelse.personidenter}",
                    )
                    null
                }
            }

        try {
            when (ident) {
                null ->
                    "Mottok en hendelse uten personident (hendelseId=${hendelse.hendelseId})".let {
                        sikkerLogg.info(it, hendelse)
                        logger.info(it)
                    }

                is PdlIdentifikator.Npid -> loggIgnorererNpid(hendelse.hendelseId)
                is PdlIdentifikator.FolkeregisterIdent -> {
                    logger.info("Mottok en PDL hendelse (hendelseId=${hendelse.hendelseId}, type=${hendelse.opplysningstype})")

                    when (LeesahOpplysningstype.valueOf(hendelse.opplysningstype)) {
                        VERGEMAAL_ELLER_FREMTIDSFULLMAKT_V1 -> haandterVergemaal(hendelse, ident)
                        ADRESSEBESKYTTELSE_V1 -> haandterAdressebeskyttelse(hendelse, ident)
                        FORELDERBARNRELASJON_V1 -> haandterForelderBarnRelasjon(hendelse, ident)
                        DOEDSFALL_V1 -> haandterDoedsHendelse(hendelse, ident)
                        UTFLYTTING_FRA_NORGE -> haandterUtflyttingFraNorge(hendelse, ident)
                        SIVILSTAND_V1 -> haandterSivilstand(hendelse, ident)
                        BOSTEDSADRESSE_V1 -> haandterBostedsadresse(hendelse, ident)
                        FOLKEREGISTERIDENTIFIKATOR_V1 -> haandterFolkeregisteridentifikator(hendelse, ident)
                    }
                }
            }
        } catch (e: Exception) {
            loggFeilVedHaandteringAvHendelse(hendelse.hendelseId, hendelse.opplysningstype, e)
            throw e
        }
    }

    private fun haandterVergemaal(
        hendelse: Personhendelse,
        personnummer: PdlIdentifikator.FolkeregisterIdent,
    ) {
        val type = hendelse.vergemaalEllerFremtidsfullmakt?.type
        if (type !in
            listOf(
                "ensligMindreaarigAsylsoeker",
                "ensligMindreaarigFlyktning",
                "mindreaarig",
                "midlertidigForMindreaarig",
                "forvaltningUtenforVergemaal",
            )
        ) {
            logger.info("Ignorerer vergemaalEllerFremtidsfullmakt med type=$type")
            return
        }

        publiserPaaRapid(
            opplysningstype = VERGEMAAL_ELLER_FREMTIDSFULLMAKT_V1,
            hendelse =
                VergeMaalEllerFremtidsfullmakt(
                    hendelseId = hendelse.hendelseId,
                    endringstype = hendelse.endringstype(),
                    fnr = personnummer.folkeregisterident.value,
                    vergeIdent = hendelse.vergemaalEllerFremtidsfullmakt?.vergeEllerFullmektig?.motpartsPersonident,
                ),
        )
    }

    private fun haandterBostedsadresse(
        hendelse: Personhendelse,
        personnummer: PdlIdentifikator.FolkeregisterIdent,
    ) {
        publiserPaaRapid(
            opplysningstype = BOSTEDSADRESSE_V1,
            hendelse =
                Bostedsadresse(
                    hendelseId = hendelse.hendelseId,
                    endringstype = hendelse.endringstype(),
                    fnr = personnummer.folkeregisterident.value,
                ),
        )
    }

    private fun haandterAdressebeskyttelse(
        hendelse: Personhendelse,
        personnummer: PdlIdentifikator.FolkeregisterIdent,
    ) {
        val gradering = hendelse.adressebeskyttelse?.gradering
        if (gradering == null || gradering == Gradering.UGRADERT) {
            logger.info("Ignorerer person med tom eller ugradert gradering, krever ingen tiltak.")
            return
        }

        publiserPaaRapid(
            opplysningstype = ADRESSEBESKYTTELSE_V1,
            hendelse =
                Adressebeskyttelse(
                    hendelseId = hendelse.hendelseId,
                    endringstype = hendelse.endringstype(),
                    fnr = personnummer.folkeregisterident.value,
                    adressebeskyttelseGradering =
                        gradering.let {
                            AdressebeskyttelseGradering.valueOf(gradering.toString())
                        },
                ),
        )
    }

    private fun haandterForelderBarnRelasjon(
        hendelse: Personhendelse,
        personnummer: PdlIdentifikator.FolkeregisterIdent,
    ) {
        publiserPaaRapid(
            opplysningstype = FORELDERBARNRELASJON_V1,
            hendelse =
                ForelderBarnRelasjonHendelse(
                    hendelseId = hendelse.hendelseId,
                    endringstype = hendelse.endringstype(),
                    fnr = personnummer.folkeregisterident.value,
                    relatertPersonsIdent = hendelse.forelderBarnRelasjon?.relatertPersonsIdent,
                    relatertPersonsRolle = hendelse.forelderBarnRelasjon?.relatertPersonsRolle,
                    minRolleForPerson = hendelse.forelderBarnRelasjon?.minRolleForPerson,
                    relatertPersonUtenFolkeregisteridentifikator =
                        hendelse.forelderBarnRelasjon?.relatertPersonUtenFolkeregisteridentifikator?.toString(),
                ),
        )
    }

    private fun haandterDoedsHendelse(
        hendelse: Personhendelse,
        personnummer: PdlIdentifikator.FolkeregisterIdent,
    ) {
        publiserPaaRapid(
            opplysningstype = DOEDSFALL_V1,
            hendelse =
                DoedshendelsePdl(
                    hendelseId = hendelse.hendelseId,
                    endringstype = hendelse.endringstype(),
                    fnr = personnummer.folkeregisterident.value,
                    doedsdato = hendelse.doedsfall?.doedsdato,
                ),
        )
    }

    private fun haandterUtflyttingFraNorge(
        hendelse: Personhendelse,
        personnummer: PdlIdentifikator.FolkeregisterIdent,
    ) {
        publiserPaaRapid(
            opplysningstype = UTFLYTTING_FRA_NORGE,
            hendelse =
                UtflyttingsHendelse(
                    hendelseId = hendelse.hendelseId,
                    endringstype = hendelse.endringstype(),
                    fnr = personnummer.folkeregisterident.value,
                    tilflyttingsLand = hendelse.utflyttingFraNorge?.tilflyttingsland,
                    tilflyttingsstedIUtlandet = hendelse.utflyttingFraNorge?.tilflyttingsstedIUtlandet,
                    utflyttingsdato = hendelse.utflyttingFraNorge?.utflyttingsdato,
                ),
        )
    }

    private fun haandterSivilstand(
        hendelse: Personhendelse,
        personnummer: PdlIdentifikator.FolkeregisterIdent,
    ) {
        publiserPaaRapid(
            opplysningstype = SIVILSTAND_V1,
            hendelse =
                SivilstandHendelse(
                    hendelseId = hendelse.hendelseId,
                    endringstype = hendelse.endringstype(),
                    fnr = personnummer.folkeregisterident.value,
                    type = hendelse.sivilstand?.type,
                    relatertVedSivilstand = hendelse.sivilstand?.relatertVedSivilstand,
                    gyldigFraOgMed = hendelse.sivilstand?.gyldigFraOgMed,
                    bekreftelsesdato = hendelse.sivilstand?.bekreftelsesdato,
                ),
        )
    }

    private fun haandterFolkeregisteridentifikator(
        hendelse: Personhendelse,
        personnummer: PdlIdentifikator.FolkeregisterIdent,
    ) = when (hendelse.endringstype()) {
        Endringstype.OPPRETTET -> {}
        Endringstype.OPPHOERT,
        Endringstype.KORRIGERT,
        Endringstype.ANNULLERT,
        ->
            publiserPaaRapid(
                opplysningstype = FOLKEREGISTERIDENTIFIKATOR_V1,
                hendelse =
                    Folkeregisteridentifikatorhendelse(
                        hendelseId = hendelse.hendelseId,
                        endringstype = hendelse.endringstype(),
                        fnr = personnummer.folkeregisterident.value,
                        gammeltFnr = hendelse.personidenter.firstOrNull(),
                    ),
            )
    }

    private fun opplysningstyperSomHaandteres() = LeesahOpplysningstype.entries.map { it.toString() }

    private fun publiserPaaRapid(
        opplysningstype: LeesahOpplysningstype,
        hendelse: PdlHendelse,
    ) {
        logger.info(
            "Publiserer at en person med fnr=${hendelse.fnr.maskerFnr()} har mottatt hendelse " +
                "med id=${hendelse.hendelseId}, endringstype=${hendelse.endringstype} og type=$opplysningstype",
        )

        kafkaProduser.publiser(
            noekkel = UUID.randomUUID().toString(),
            verdi =
                JsonMessage.newMessage(
                    eventName = PdlHendelserKeys.PERSONHENDELSE.lagEventnameForType(),
                    map =
                        mapOf(
                            "hendelse" to opplysningstype.toString(),
                            "hendelse_data" to hendelse,
                        ),
                ),
        )
    }

    private fun Personhendelse.endringstype() = Endringstype.valueOf(this.endringstype.name)

    private fun loggIgnorererNpid(hendelseId: String) =
        logger.warn("Ignorerer en hendelse med id=$hendelseId om en person som kun har NPID som identifikator")

    private fun loggFeilVedHaandteringAvHendelse(
        hendelseId: String,
        opplysningstype: String,
        e: Exception,
    ) {
        logger.error(
            "Kunne ikke haandtere $opplysningstype med id=$hendelseId. Dette skyldes sannsynligvis at " +
                "personhendelsen ser annerledes ut enn forventet.",
            e,
        )
    }
}
