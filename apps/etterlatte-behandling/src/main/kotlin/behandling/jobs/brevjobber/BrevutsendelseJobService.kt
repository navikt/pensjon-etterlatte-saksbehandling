package no.nav.etterlatte.behandling.jobs.brevjobber

import no.nav.etterlatte.Context
import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.behandling.jobs.brevjobber.ArbeidStatus.FEILET
import no.nav.etterlatte.behandling.jobs.brevjobber.ArbeidStatus.FERDIG
import no.nav.etterlatte.behandling.jobs.brevjobber.ArbeidStatus.PAAGAAENDE
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import no.nav.etterlatte.libs.ktor.token.HardkodaSystembruker
import org.slf4j.LoggerFactory

class BrevutsendelseJobService(
    private val brevutsendelseDao: BrevutsendelseDao,
    private val brevutsendelseService: BrevutsendelseService,
) {
    private val saksbehandler: BrukerTokenInfo = HardkodaSystembruker.oppgave

    private val logger = LoggerFactory.getLogger(this::class.java)

    fun setupKontekstAndRun(context: Context) {
        Kontekst.set(context)
        run()
    }

    private fun run() {
        logger.info("Starter jobb for masseutsendelse av brev")

        val brevutsendelser = inTransaction { brevutsendelseDao.hentNyeJobber(ANTALL_SAKER, SPESIFIKKE_SAKER, EKSKLUDERTE_SAKER) }
        logger.info("Hentet ${brevutsendelser.size} brevutsendelser som er klare for prosessering")

        brevutsendelser.forEach { brevutsendelse ->
            try {
                inTransaction {
                    val paagaaendeBrevutsendelse = oppdaterStatus(brevutsendelse, PAAGAAENDE)
                    brevutsendelseService.prosesserBrevutsendelse(paagaaendeBrevutsendelse, saksbehandler)
                    oppdaterStatus(paagaaendeBrevutsendelse, FERDIG)
                }
            } catch (e: Exception) {
                // TODO ønsker nok å lagre ned exception her
                oppdaterStatus(brevutsendelse, FEILET)
                logger.error("Feilet under brevutsendelse av type ${brevutsendelse.type.name} for sak ${brevutsendelse.sakId}", e)
            }
        }
    }

    private fun oppdaterStatus(
        jobb: Brevutsendelse,
        status: ArbeidStatus,
    ) = brevutsendelseDao.oppdaterJobb(jobb.oppdaterStatus(status))

    // TODO burde dette heller håndteres fra en tabell slik at det kan oppdateres uten å endre koden? (https://jira.adeo.no/browse/EY-4870)
    companion object {
        val ANTALL_SAKER: Int = 1
        val EKSKLUDERTE_SAKER: List<SakId> = emptyList()
        val SPESIFIKKE_SAKER: List<SakId> = emptyList()
    }
}
