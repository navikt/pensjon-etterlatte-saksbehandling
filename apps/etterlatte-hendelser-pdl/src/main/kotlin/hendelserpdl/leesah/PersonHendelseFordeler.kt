package no.nav.etterlatte.hendelserpdl.leesah

import no.nav.etterlatte.hendelserpdl.pdl.Pdl
import no.nav.etterlatte.libs.common.pdlhendelse.Endringstype
import no.nav.etterlatte.libs.common.person.AdressebeskyttelseGradering
import no.nav.etterlatte.libs.common.person.PdlIdentifikator
import no.nav.person.pdl.leesah.Personhendelse
import no.nav.person.pdl.leesah.verge.VergemaalEllerFremtidsfullmakt
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.format.DateTimeFormatter

class PersonHendelseFordeler(
    private val postHendelser: ILivsHendelserRapid,
    private val pdlService: Pdl
) {
    private val log: Logger = LoggerFactory.getLogger(PersonHendelseFordeler::class.java)

    suspend fun haandterHendelse(personhendelse: Personhendelse) {
        when (personhendelse.opplysningstype) {
            LeesahOpplysningstyper.DOEDSFALL_V1.toString() -> haandterDoedsendelse(personhendelse)
            LeesahOpplysningstyper.UTFLYTTING_FRA_NORGE.toString() -> haandterUtflyttingFraNorge(personhendelse)
            LeesahOpplysningstyper.FORELDERBARNRELASJON_V1.toString() -> haandterForelderBarnRelasjon(personhendelse)
            LeesahOpplysningstyper.ADRESSEBESKYTTELSE_V1.toString() -> haandterAdressebeskyttelse(personhendelse)
            LeesahOpplysningstyper.VERGEMAAL_ELLER_FREMTIDSFULLMAKT_V1.toString() -> haandterVergemaal(personhendelse)
            else -> log.info("Så en hendelse av type ${personhendelse.opplysningstype} som vi ikke håndterer")
        }
    }

    private suspend fun haandterVergemaal(personhendelse: Personhendelse) {
        val hendelseType = "Vergemål"
        val vergemaalEllerFremtidsfullmakt: VergemaalEllerFremtidsfullmakt? =
            personhendelse.vergemaalEllerFremtidsfullmakt
        if (vergemaalEllerFremtidsfullmakt?.type in
            listOf(
                    "ensligMindreaarigAsylsoeker",
                    "ensligMindreaarigFlyktning",
                    "mindreaarig",
                    "midlertidigForMindreaarig",
                    "forvaltningUtenforVergemaal"
                )
        ) {
            try {
                when (val personnummer = pdlService.hentPdlIdentifikator(personhendelse.personidenter.first())) {
                    is PdlIdentifikator.Npid -> {
                        log.info(
                            "Ignorerer en hendelse med id=${personhendelse.hendelseId} om en person som kun har NPID " +
                                "som identifikator"
                        )
                        return
                    }

                    is PdlIdentifikator.FolkeregisterIdent -> {
                        val endringstype = Endringstype.valueOf(personhendelse.endringstype.name)
                        personhendelse.vergemaalEllerFremtidsfullmakt.vergeEllerFullmektig.let {
                            postHendelser.haandterVergeoppnevnelse(
                                fnr = personnummer.folkeregisterident.value,
                                vergeIdent = personhendelse.vergemaalEllerFremtidsfullmakt
                                    .vergeEllerFullmektig.motpartsPersonident,
                                endringstype = endringstype
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                loggFeilVedHaandtering(personhendelse.hendelseId, hendelseType, e)
            }
        } else {
            log.info("Ignorerer vergemaalEllerFremtidsfullmakt av typen ${vergemaalEllerFremtidsfullmakt?.type}")
        }
    }

    private suspend fun haandterAdressebeskyttelse(personhendelse: Personhendelse) {
        val hendelseType = "Adressebeskyttelse"
        val gradering = personhendelse.adressebeskyttelse?.gradering
        if (gradering == null || gradering == no.nav.person.pdl.leesah.adressebeskyttelse.Gradering.UGRADERT) {
            log.info("Ignorerer person med tom eller ugradert gradering, krever ingen tiltak.")
            return
        }
        try {
            when (val personnummer = pdlService.hentPdlIdentifikator(personhendelse.personidenter.first())) {
                is PdlIdentifikator.Npid -> {
                    log.info(
                        "Ignorerer en hendelse med id=${personhendelse.hendelseId} om en person som kun har NPID " +
                            "som identifikator"
                    )
                    return
                }

                is PdlIdentifikator.FolkeregisterIdent -> {
                    val endringstype = Endringstype.valueOf(personhendelse.endringstype.name)
                    personhendelse.adressebeskyttelse.let {
                        postHendelser.haandterAdressebeskyttelse(
                            fnr = personnummer.folkeregisterident.value,
                            adressebeskyttelseGradering = gradering.let {
                                AdressebeskyttelseGradering.valueOf(gradering.toString())
                            },
                            endringstype = endringstype
                        )
                    }
                }
            }
        } catch (e: Exception) {
            loggFeilVedHaandtering(personhendelse.hendelseId, hendelseType, e)
            throw e
        }
    }

    private suspend fun haandterForelderBarnRelasjon(personhendelse: Personhendelse) {
        val hendelseType = "Forelder-barn-relasjon-hendelse"
        try {
            when (val personnummer = pdlService.hentPdlIdentifikator(personhendelse.personidenter.first())) {
                is PdlIdentifikator.Npid -> {
                    log.info(
                        "Ignorerer en hendelse med id=${personhendelse.hendelseId} om en person som kun har NPID " +
                            "som identifikator"
                    )
                    return
                }

                is PdlIdentifikator.FolkeregisterIdent -> {
                    val endringstype = Endringstype.valueOf(personhendelse.endringstype.name)
                    personhendelse.forelderBarnRelasjon.let {
                        postHendelser.forelderBarnRelasjon(
                            fnr = personnummer.folkeregisterident.value,
                            relatertPersonsIdent = it?.relatertPersonsIdent,
                            relatertPersonsRolle = it?.relatertPersonsRolle,
                            minRolleForPerson = it?.minRolleForPerson,
                            relatertPersonUtenFolkeregisteridentifikator =
                            it?.relatertPersonUtenFolkeregisteridentifikator?.toString(),
                            endringstype = endringstype
                        )
                    }
                }
            }
        } catch (e: Exception) {
            loggFeilVedHaandtering(personhendelse.hendelseId, hendelseType, e)
            throw e
        }
    }

    private suspend fun haandterDoedsendelse(personhendelse: Personhendelse) {
        val hendelseType = "Doedshendelse"
        try {
            when (val personnummer = pdlService.hentPdlIdentifikator(personhendelse.personidenter.first())) {
                is PdlIdentifikator.Npid -> {
                    log.info(
                        "Ignorerer en hendelse med id=${personhendelse.hendelseId} om en person som kun har NPID " +
                            "som identifikator"
                    )
                    return
                }

                is PdlIdentifikator.FolkeregisterIdent -> {
                    val endringstype = Endringstype.valueOf(personhendelse.endringstype.name)
                    postHendelser.personErDod(
                        fnr = personnummer.folkeregisterident.value,
                        doedsdato = try {
                            personhendelse.doedsfall?.doedsdato?.format(DateTimeFormatter.ISO_DATE)
                        } catch (e: Exception) {
                            log.warn("Kunne ikke String-formatere dato i en dødshendelse")
                            null
                        },
                        endringstype = endringstype
                    )
                }
            }
        } catch (e: Exception) {
            loggFeilVedHaandtering(personhendelse.hendelseId, hendelseType, e)
            throw e
        }
    }

    private suspend fun haandterUtflyttingFraNorge(personhendelse: Personhendelse) {
        val hendelseType = "Utflytting fra Norge-hendelse"
        try {
            when (val personnummer = pdlService.hentPdlIdentifikator(personhendelse.personidenter.first())) {
                is PdlIdentifikator.Npid -> {
                    log.info(
                        "Ignorerer en hendelse med id=${personhendelse.hendelseId} om en person som kun har NPID " +
                            "som identifikator"
                    )
                    return
                }

                is PdlIdentifikator.FolkeregisterIdent -> {
                    val endringstype = Endringstype.valueOf(personhendelse.endringstype.name)
                    postHendelser.personUtflyttingFraNorge(
                        fnr = personnummer.folkeregisterident.value,
                        tilflyttingsLand = personhendelse.utflyttingFraNorge?.tilflyttingsland,
                        tilflyttingsstedIUtlandet = personhendelse.utflyttingFraNorge?.tilflyttingsstedIUtlandet,
                        utflyttingsdato = try {
                            personhendelse.utflyttingFraNorge?.utflyttingsdato?.format(DateTimeFormatter.ISO_DATE)
                        } catch (e: Exception) {
                            log.warn("Kunne ikke String-formatere dato i en utflyttingshendelse")
                            null
                        },
                        endringstype = endringstype
                    )
                }
            }
        } catch (e: Exception) {
            loggFeilVedHaandtering(personhendelse.hendelseId, hendelseType, e)
            throw e
        }
    }

    private fun loggFeilVedHaandtering(hendelsesid: String, hendelseType: String, e: Exception) {
        log.error(
            "kunne ikke haandtere $hendelseType for hendelsen med id=$hendelsesid. Dette skyldes sannsynligvis" +
                "at personhendelsen ser annerledes ut enn forventet, eller at det var problem med henting av " +
                "personidentifikatoren fra PDL",
            e
        )
    }
}