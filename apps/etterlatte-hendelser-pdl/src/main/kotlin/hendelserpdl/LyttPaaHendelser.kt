package no.nav.etterlatte.hendelserpdl

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.hendelserpdl.leesah.ILivetErEnStroemAvHendelser
import no.nav.etterlatte.hendelserpdl.pdl.Pdl
import no.nav.etterlatte.libs.common.pdlhendelse.Endringstype
import no.nav.person.pdl.leesah.Personhendelse
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.format.DateTimeFormatter

class LyttPaaHendelser(
    private val livshendelser: ILivetErEnStroemAvHendelser,
    private val postHendelser: ILivsHendelser,
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

            when (it.opplysningstype) {
                "DOEDSFALL_V1" -> haandterDoedsendelse(it)
                "UTFLYTTING_FRA_NORGE" -> haandterUtflyttingFraNorge(it)
                "FORELDERBARNRELASJON_V1" -> haandterForelderBarnRelasjon(it)
                else -> {
                    log.info(
                        "Så opplysning om ${it.opplysningstype} opprettet ${it.opprettet} " +
                            " for ident ${it.personidenter}: $it"
                    )
                }
            }
        }

        runBlocking {
            if (antallMeldingerLest == 0) delay(500)
        }
    }

    private fun haandterForelderBarnRelasjon(personhendelse: Personhendelse) {
        val hendelseType = "Forelder-barn-relasjon-hendelse"
        personhendelse.loggHendelse(hendelseType)
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
            personhendelse.loggFeilVedHaandtering(hendelseType, e)
        }
    }

    private fun haandterDoedsendelse(personhendelse: Personhendelse) {
        val hendelseType = "Doedshendelse"
        personhendelse.loggHendelse(hendelseType)
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
                    log.info("Kunne ikke String-formatere dato")
                    null
                },
                endringstype = endringstype
            )
        } catch (e: Exception) {
            personhendelse.loggFeilVedHaandtering(hendelseType, e)
        }
        dodsmeldinger++
    }

    fun haandterUtflyttingFraNorge(personhendelse: Personhendelse) {
        val hendelseType = "Utflytting fra Norge-hendelse"
        personhendelse.loggHendelse(hendelseType)
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
                    log.info("Kunne ikke String-formatere dato")
                    null
                },
                endringstype = endringstype
            )
        } catch (e: Exception) {
            personhendelse.loggFeilVedHaandtering(hendelseType, e)
        }
    }

    private fun Personhendelse.loggHendelse(hendelseType: String) {
        log.info(
            "$hendelseType mottatt for : $personidenter med endringstype $endringstype. Hendelse: $this"
        )
    }

    private fun Personhendelse.loggFeilVedHaandtering(hendelseType: String, e: Exception) {
        log.error(
            "kunne ikke haandtere $hendelseType " + "for ${personidenter.firstOrNull()}. Dette skyldes sannsynligvis" +
                "at personhendelsen ser annerledes ut enn forventet, eller at det var problem med henting av " +
                "folkeregisteridentifikatoren fra PDL",
            e
        )
    }

    fun fraStart() {
        livshendelser.fraStart()
    }

    fun stop() {
        // livshendelser.stop()
        stopped = true
    }

    fun start() {
        stopped = false
    }
}