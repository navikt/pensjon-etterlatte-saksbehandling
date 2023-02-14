package no.nav.etterlatte.hendelserpdl

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.hendelserpdl.leesah.ILivetErEnStroemAvHendelser
import no.nav.etterlatte.hendelserpdl.pdl.Pdl
import no.nav.etterlatte.libs.common.logging.withLogContext
import no.nav.etterlatte.libs.common.pdlhendelse.Endringstype
import no.nav.etterlatte.libs.common.pdlhendelse.Gradering
import no.nav.person.pdl.leesah.Personhendelse
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.format.DateTimeFormatter

class LyttPaaHendelser(
    private val livshendelser: ILivetErEnStroemAvHendelser,
    private val postHendelser: ILivsHendelserRapid,
    private val pdlService: Pdl
) {
    val log: Logger = LoggerFactory.getLogger(LyttPaaHendelser::class.java)
    var iterasjoner = 0
    var dodsmeldinger = 0
    var meldinger = 0
    var stopped = false

    fun stream() {
        iterasjoner++

        val antallMeldingerLest = livshendelser.poll {
            meldinger++

            withLogContext {
                when (it.opplysningstype) {
                    LeesahOpplysningstyper.DOEDSFALL_V1.toString() -> haandterDoedsendelse(it)
                    LeesahOpplysningstyper.UTFLYTTING_FRA_NORGE.toString() -> haandterUtflyttingFraNorge(it)
                    LeesahOpplysningstyper.FORELDERBARNRELASJON_V1.toString() -> haandterForelderBarnRelasjon(it)
                    LeesahOpplysningstyper.ADRESSEBESKYTTELSE_V1.toString() -> haandterAdressebeskyttelse(it)
                    else -> log.info("Så en hendelse av type ${it.opplysningstype} som vi ikke håndterer")
                }
            }
        }

        runBlocking {
            if (antallMeldingerLest == 0) delay(500)
        }
    }

    private fun haandterAdressebeskyttelse(personhendelse: Personhendelse) {
        val hendelseType = "Adressebeskyttelse"
        val gradering = personhendelse.adressebeskyttelse.gradering
        if (gradering == null || gradering == no.nav.person.pdl.leesah.adressebeskyttelse.Gradering.UGRADERT) {
            log.info("Ignorerer person med tom eller ugradert gradering, krever ingen tiltak.")
            return
        }
        try {
            val personnummer = runBlocking {
                pdlService.hentFolkeregisterIdentifikator(personhendelse.personidenter.first())
            }
            val endringstype = Endringstype.valueOf(personhendelse.endringstype.name)
            personhendelse.adressebeskyttelse.let {
                postHendelser.haandterAdressebeskyttelse(
                    fnr = personnummer.folkeregisterident.value,
                    gradering = gradering.let { Gradering.valueOf(gradering.toString()) },
                    endringstype = endringstype
                )
            }
        } catch (e: Exception) {
            loggFeilVedHaandtering(personhendelse.hendelseId, hendelseType, e)
        }
    }

    private fun haandterForelderBarnRelasjon(personhendelse: Personhendelse) {
        val hendelseType = "Forelder-barn-relasjon-hendelse"
        try {
            val personnummer = runBlocking {
                pdlService.hentFolkeregisterIdentifikator(personhendelse.personidenter.first())
            }
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
        } catch (e: Exception) {
            loggFeilVedHaandtering(personhendelse.hendelseId, hendelseType, e)
        }
    }

    private fun haandterDoedsendelse(personhendelse: Personhendelse) {
        val hendelseType = "Doedshendelse"
        try {
            val personnummer = runBlocking {
                pdlService.hentFolkeregisterIdentifikator(personhendelse.personidenter.first())
            }
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
        } catch (e: Exception) {
            loggFeilVedHaandtering(personhendelse.hendelseId, hendelseType, e)
        }
        dodsmeldinger++
    }

    fun haandterUtflyttingFraNorge(personhendelse: Personhendelse) {
        val hendelseType = "Utflytting fra Norge-hendelse"
        try {
            val personnummer = runBlocking {
                pdlService.hentFolkeregisterIdentifikator(personhendelse.personidenter.first())
            }
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
        } catch (e: Exception) {
            loggFeilVedHaandtering(personhendelse.hendelseId, hendelseType, e)
        }
    }

    private fun loggFeilVedHaandtering(hendelsesid: String, hendelseType: String, e: Exception) {
        log.error(
            "kunne ikke haandtere $hendelseType for hendelsen med id=$hendelsesid. Dette skyldes sannsynligvis" +
                "at personhendelsen ser annerledes ut enn forventet, eller at det var problem med henting av " +
                "folkeregisteridentifikatoren fra PDL",
            e
        )
    }

    fun fraStart() {
        livshendelser.fraStart()
    }

    fun stop() {
        stopped = true
    }

    fun start() {
        stopped = false
    }
}