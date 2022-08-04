package no.nav.etterlatte.hendelserpdl

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.hendelserpdl.leesah.ILivetErEnStroemAvHendelser
import no.nav.etterlatte.hendelserpdl.pdl.Pdl
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.format.DateTimeFormatter


class FinnDodsmeldinger(
    private val livshendelser: ILivetErEnStroemAvHendelser,
    private val dodshendelser: IDodsmeldinger,
    private val pdlService: Pdl
) {
    val log: Logger = LoggerFactory.getLogger(FinnDodsmeldinger::class.java)
    var iterasjoner = 0
    var dodsmeldinger = 0
    var meldinger = 0
    var stopped = false

    fun stream() {
        iterasjoner++

        val antallMeldingerLest = livshendelser.poll {
            meldinger++

            if (it.getOpplysningstype() == "DOEDSFALL_V1") {
                log.info("Doedshendelse mottatt for : ${it.personidenter}")
                try {
                    val personnummer =
                        runBlocking { pdlService.hentFolkeregisterIdentifikator(it.personidenter.first()) }

                    dodshendelser.personErDod(
                        personnummer.folkeregisterident.value,
                        (it.getDoedsfall()?.getDoedsdato()?.format(DateTimeFormatter.ISO_DATE))
                    )
                } catch (e: Exception) {
                    log.error("kunne ikke hente folkeregisterident for ${it.personidenter.first()}. Går til neste melding")
                }
                dodsmeldinger++
            } else {
                //log.info("Så opplysning om ${it.opplysningstype} opprettet ${it.opprettet}")
            }

        }


        runBlocking {
            if (antallMeldingerLest == 0) delay(500)
        }
    }


    fun fraStart() {
        livshendelser.fraStart()
    }


    fun stop() {
        //livshendelser.stop()
        stopped = true
    }

    fun start() {
        stopped = false
    }

}