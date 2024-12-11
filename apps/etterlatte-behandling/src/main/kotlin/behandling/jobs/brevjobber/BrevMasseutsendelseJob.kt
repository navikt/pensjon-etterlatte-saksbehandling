package no.nav.etterlatte.behandling.jobs.brevjobber

import no.nav.etterlatte.Context
import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.behandling.jobs.brevjobber.ArbeidStatus.FEILET
import no.nav.etterlatte.behandling.jobs.brevjobber.ArbeidStatus.FERDIG
import no.nav.etterlatte.behandling.jobs.brevjobber.ArbeidStatus.PAAGAAENDE
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import no.nav.etterlatte.libs.ktor.token.HardkodaSystembruker
import org.slf4j.LoggerFactory

class BrevMasseutsendelseJob(
    private val arbeidstabellDao: ArbeidstabellDao,
    private val brevMasseutsendelseService: BrevMasseutsendelseService,
) {
    private val saksbehandler: BrukerTokenInfo = HardkodaSystembruker.oppgave

    private val logger = LoggerFactory.getLogger(this::class.java)

    fun setupKontekstAndRun(context: Context) {
        Kontekst.set(context)
        run()
    }

    // TODO er jobber egentlig riktig begrep her - blir kanskje litt for generelt i denne sammenheng? Burde tabellen heller hete masseutsendelse_brev?
    // TODO vi ønsker kun å hente ut et visst antall for kjøring - hva blir riktig måte å gjøre dette på?
    // TODO mer logging må på plass
    private fun run() {
        logger.info("Starter jobb for masseutsendelse av brev")

        val brevutsendelser = inTransaction { arbeidstabellDao.hentKlareJobber() }
        logger.info("Hentet ${brevutsendelser.size} brevutsendelser som er klare for prosessering")

        brevutsendelser.forEach { brevutsendelse ->
            try {
                inTransaction {
                    val paagaaendeBrevutsendelse = oppdaterStatus(brevutsendelse, PAAGAAENDE)
                    brevMasseutsendelseService.prosesserBrevutsendelse(paagaaendeBrevutsendelse, saksbehandler)
                    oppdaterStatus(paagaaendeBrevutsendelse, FERDIG)
                }
            } catch (e: Exception) {
                oppdaterStatus(brevutsendelse, FEILET)
                logger.error("Feilet under brevutsendelse av type ${brevutsendelse.type.name} for sak ${brevutsendelse.sakId}", e)
            }
        }
    }

    private fun oppdaterStatus(
        jobb: Arbeidsjobb,
        status: ArbeidStatus,
    ) = arbeidstabellDao.oppdaterJobb(jobb.oppdaterStatus(status))
}
