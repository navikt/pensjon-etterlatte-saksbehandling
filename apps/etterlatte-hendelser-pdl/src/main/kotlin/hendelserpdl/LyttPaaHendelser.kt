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

    private fun haandterDoedsendelse(personhendelse: Personhendelse) {
        log.info(
            "Doedshendelse mottatt for : ${personhendelse.personidenter} med endringstype " +
                "${personhendelse.endringstype}. Hendelse: $personhendelse"
        )
        try {
            val personnummer =
                runBlocking { pdlService.hentFolkeregisterIdentifikator(personhendelse.personidenter.first()) }
            val endringstype = Endringstype.valueOf(personhendelse.endringstype.name)
            postHendelser.personErDod(
                fnr = personnummer.folkeregisterident.value,
                doedsdato = personhendelse.doedsfall?.doedsdato?.format(DateTimeFormatter.ISO_DATE),
                endringstype = endringstype
            )
        } catch (e: Exception) {
            log.error(
                "kunne ikke haandtere doedshendelse for ${personhendelse.personidenter.first()}.",
                e
            )
        }
        dodsmeldinger++
    }

    fun haandterUtflyttingFraNorge(personhendelse: Personhendelse) {
        log.info(
            "Utflytting fra Norge-henddelse mottatt for : ${personhendelse.personidenter} med endringstype " +
                "${personhendelse.endringstype}. Hendelse: $personhendelse"
        )
        try {
            val personnummer =
                runBlocking { pdlService.hentFolkeregisterIdentifikator(personhendelse.personidenter.first()) }
            val endringstype = Endringstype.valueOf(personhendelse.endringstype.name)
            postHendelser.personUtflyttingFraNorge(
                fnr = personnummer.folkeregisterident.value,
                tilflyttingsLand = personhendelse.utflyttingFraNorge?.tilflyttingsland,
                tilflyttingsstedIUtlandet = personhendelse.utflyttingFraNorge?.tilflyttingsstedIUtlandet,
                utflyttingsdato = personhendelse.utflyttingFraNorge?.utflyttingsdato?.format(DateTimeFormatter.ISO_DATE), // ktlint-disable max-line-length
                endringstype = endringstype
            )
        } catch (e: Exception) {
            log.error(
                "kunne ikke haandtere utflytting fra Norge-hendelse " +
                    "for ${personhendelse.personidenter.first()}",
                e
            )
        }
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