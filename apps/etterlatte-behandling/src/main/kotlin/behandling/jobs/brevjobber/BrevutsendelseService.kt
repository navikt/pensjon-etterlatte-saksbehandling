package no.nav.etterlatte.behandling.jobs.brevjobber

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.behandling.jobs.brevjobber.SjekkGyldigBrevMottakerResultat.GYLDIG_MOTTAKER
import no.nav.etterlatte.behandling.klienter.BrevApiKlient
import no.nav.etterlatte.brev.BrevParametre
import no.nav.etterlatte.brev.SaksbehandlerOgAttestant
import no.nav.etterlatte.brev.model.Brev
import no.nav.etterlatte.brev.model.BrevStatusResponse
import no.nav.etterlatte.brev.model.FerdigstillJournalFoerOgDistribuerOpprettetBrev
import no.nav.etterlatte.brev.model.Spraak
import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import no.nav.etterlatte.libs.common.logging.withLogContext
import no.nav.etterlatte.libs.common.oppgave.OppgaveIntern
import no.nav.etterlatte.libs.common.oppgave.OppgaveType
import no.nav.etterlatte.libs.common.person.maskerFnr
import no.nav.etterlatte.libs.common.retry
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import no.nav.etterlatte.oppgave.OppgaveService
import no.nav.etterlatte.sak.SakService
import org.slf4j.LoggerFactory

class BrevutsendelseService(
    private val sakService: SakService,
    private val oppgaveService: OppgaveService,
    private val sjekkBrevMottakerService: SjekkBrevMottakerService,
    private val brevKlient: BrevApiKlient,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    fun prosesserBrevutsendelse(
        brevutsendelse: Arbeidsjobb,
        brukerTokenInfo: BrukerTokenInfo,
    ) {
        withLogContext(kv = mdcVerdier(brevutsendelse)) {
            logger.info("Starter å prosessering brevutsendelse av type ${brevutsendelse.type.name} for sak ${brevutsendelse.sakId}")

            val sak = hentSak(brevutsendelse)
            val gyldigBrevMottakerResultat = sjekkBrevMottakerService.sjekkOmPersonErGyldigBrevmottaker(sak, brukerTokenInfo)

            if (gyldigBrevMottakerResultat == GYLDIG_MOTTAKER) {
                sendBrev(sak, brevutsendelse, brukerTokenInfo)
                // TODO hvis noe feiler her, må manuell oppgave opprettes
            } else {
                logger.info("Manuell oppgave opprettes i sak ${sak.id} fordi mottaker ikke var gyldig: ${gyldigBrevMottakerResultat.name}")
                opprettManuellOppgave(brevutsendelse)
            }

            logger.info("Prosessering av brevutsendelse i sak ${brevutsendelse.sakId} er fullført")

            // TODO burde returnere et objekt her som sier noe om hva som ble resultatet (brev sendt, manuell oppgave opprettet etc)
        }
    }

    private fun mdcVerdier(brevutsendelse: Arbeidsjobb) =
        mapOf(
            "sakId" to brevutsendelse.sakId.toString(),
            "jobbType" to brevutsendelse.type.name,
        )

    private fun hentSak(brevutsendelse: Arbeidsjobb) =
        (
            sakService.finnSak(brevutsendelse.sakId)
                ?: throw InternfeilException("Fant ikke sak med id ${brevutsendelse.sakId} for brevutsendelse")
        )

    private fun sendBrev(
        sak: Sak,
        brevutsendelse: Arbeidsjobb,
        saksbehandler: BrukerTokenInfo,
    ): Boolean {
        logger.info("Sender brev til ${sak.ident.maskerFnr()} i sak ${sak.id}")

        val tomtBrev = BrevParametre.TomtBrev(Spraak.NB) // TODO placeholder inntil vi har en brevmal

        val opprettetBrev =
            runBlocking {
                brevKlient.opprettSpesifiktBrev(brevutsendelse.sakId, tomtBrev, saksbehandler)
            }
        runBlocking {
            retry(3) {
                ferdigStillJournalFoerOgDistribuerOpprettetBrev(opprettetBrev, brevutsendelse, sak, saksbehandler)
            }
        }

        return true
    }

    private fun ferdigStillJournalFoerOgDistribuerOpprettetBrev(
        opprettetBrev: Brev,
        brevutsendelse: Arbeidsjobb,
        sak: Sak,
        saksbehandler: BrukerTokenInfo,
    ): BrevStatusResponse =
        runBlocking {
            brevKlient.ferdigstillJournalFoerOgDistribuerBrev(
                FerdigstillJournalFoerOgDistribuerOpprettetBrev(
                    opprettetBrev.id,
                    brevutsendelse.sakId,
                    sak.enhet,
                    SaksbehandlerOgAttestant(saksbehandler.ident(), saksbehandler.ident()),
                ),
                saksbehandler,
            )
        }

    private fun opprettManuellOppgave(brevutsendelse: Arbeidsjobb): OppgaveIntern =
        oppgaveService.opprettOppgave(
            referanse = brevutsendelse.id.toString(),
            sakId = brevutsendelse.sakId,
            kilde = null, // TODO legge inn kilde
            type = OppgaveType.GENERELL_OPPGAVE, // TODO er dette riktig
            merknad = "Her må det komme tekst om hva saksbehandler må gjøre", // TODO hva skal stå her?
        )
}
