package no.nav.etterlatte.hendelserpdl

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.hendelserpdl.leesah.ILivetErEnStroemAvHendelser
import no.nav.etterlatte.hendelserpdl.pdl.Pdl
import no.nav.etterlatte.libs.common.pdlhendelse.Endringstype
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
                log.info(
                    "Doedshendelse mottatt for : ${it.personidenter} med hendelsesId: ${it.hendelseId} og " +
                        "endringstype ${it.endringstype}. Evt. tidligere henselsesid: ${it.tidligereHendelseId}"
                )
                log.info("Fullstendig doedshendelse: $it")
                try {
                    val personnummer =
                        runBlocking { pdlService.hentFolkeregisterIdentifikator(it.personidenter.first()) }
                    val endringstype = Endringstype.valueOf(it.endringstype.name)

                    dodshendelser.personErDod(
                        personnummer.folkeregisterident.value,
                        (it.doedsfall?.doedsdato?.format(DateTimeFormatter.ISO_DATE)),
                        endringstype
                    )
                } catch (e: Exception) {
                    log.error(
                        "kunne ikke hente folkeregisterident for ${it.personidenter.first()}. Går til neste melding"
                    )
                }
                dodsmeldinger++
            } else {
                // log.info("Så opplysning om ${it.opplysningstype} opprettet ${it.opprettet} for ident ${it.personidenter} med endringstype ${it.endringstype} og hendelsesid: ${it.hendelseId}")
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
        // livshendelser.stop()
        stopped = true
    }

    fun start() {
        stopped = false
    }
}