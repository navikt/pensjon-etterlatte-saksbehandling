package no.nav.etterlatte.hendelserpdl

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.hendelserpdl.leesah.ILivetErEnStroemAvHendelser
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.format.DateTimeFormatter


class FinnDodsmeldinger(
    private val livshendelser: ILivetErEnStroemAvHendelser,
    private val dodshendelser: IDodsmeldinger
) {
    val log: Logger = LoggerFactory.getLogger(FinnDodsmeldinger::class.java)
    var iterasjoner = 0
    var dodsmeldinger = 0
    var meldinger = 0
    var stopped = true

    fun stream() {
        iterasjoner++

        val antallMeldingerLest = livshendelser.poll {
            meldinger++

            if (it.getOpplysningstype() == "DOEDSFALL_V1") {
                dodshendelser.personErDod(
                    it.getPersonidenter()[0],
                    (it.getDoedsfall()?.getDoedsdato()?.format(DateTimeFormatter.ISO_DATE))
                )
                dodsmeldinger++
            } else {
                log.info("Så opplysning om ${it.opplysningstype} opprettet ${it.opprettet}")
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