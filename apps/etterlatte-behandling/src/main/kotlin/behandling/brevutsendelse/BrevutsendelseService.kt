package no.nav.etterlatte.behandling.brevutsendelse

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.behandling.brevutsendelse.SjekkGyldigBrevMottakerResultat.GYLDIG_MOTTAKER
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
import no.nav.etterlatte.libs.common.retryOgPakkUt
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
        brevutsendelse: Brevutsendelse,
        brukerTokenInfo: BrukerTokenInfo,
    ) {
        withLogContext(kv = mdcVerdier(brevutsendelse)) {
            logger.info("Starter å prosessering brevutsendelse av type ${brevutsendelse.type.name} for sak ${brevutsendelse.sakId}")

            val sak = hentSak(brevutsendelse)
            val gyldigBrevMottakerResultat = sjekkBrevMottakerService.sjekkOmPersonErGyldigBrevmottaker(sak, brukerTokenInfo)

            if (gyldigBrevMottakerResultat == GYLDIG_MOTTAKER) {
                // TODO: lagre ned status på brev?
                val brevStatusResponse = sendBrev(sak, brevutsendelse, brukerTokenInfo)

                // TODO hvis noe feiler her, må manuell oppgave opprettes
            } else {
                logger.info("Manuell oppgave opprettes i sak ${sak.id} fordi mottaker ikke var gyldig: ${gyldigBrevMottakerResultat.name}")
                opprettManuellOppgave(brevutsendelse)
            }

            logger.info("Prosessering av brevutsendelse i sak ${brevutsendelse.sakId} er fullført")

            // TODO burde returnere et objekt her som sier noe om hva som ble resultatet (brev sendt, manuell oppgave opprettet etc)
        }
    }

    private fun mdcVerdier(brevutsendelse: Brevutsendelse) =
        mapOf(
            "sakId" to brevutsendelse.sakId.toString(),
            "brevutsendelseType" to brevutsendelse.type.name,
        )

    private fun hentSak(brevutsendelse: Brevutsendelse) =
        (
            sakService.finnSak(brevutsendelse.sakId)
                ?: throw InternfeilException("Fant ikke sak med id ${brevutsendelse.sakId} for brevutsendelse")
        )

    private fun sendBrev(
        sak: Sak,
        brevutsendelse: Brevutsendelse,
        saksbehandler: BrukerTokenInfo,
    ): BrevStatusResponse {
        logger.info("Sender brev til ${sak.ident.maskerFnr()} i sak ${sak.id}")

        val tomtBrev = BrevParametre.TomtBrev(Spraak.NB) // TODO placeholder inntil vi har en brevmal

        val opprettetBrev =
            runBlocking {
                brevKlient.opprettSpesifiktBrev(brevutsendelse.sakId, tomtBrev, saksbehandler)
            }
        val brevResponse =
            runBlocking {
                retryOgPakkUt(3) {
                    ferdigStillJournalFoerOgDistribuerOpprettetBrev(opprettetBrev, brevutsendelse, sak, saksbehandler)
                }
            }

        return brevResponse
    }

    private fun ferdigStillJournalFoerOgDistribuerOpprettetBrev(
        opprettetBrev: Brev,
        brevutsendelse: Brevutsendelse,
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

    private fun opprettManuellOppgave(brevutsendelse: Brevutsendelse): OppgaveIntern =
        // TODO legg inn riktig parametere for oppgave
        oppgaveService.opprettOppgave(
            referanse = brevutsendelse.id.toString(),
            sakId = brevutsendelse.sakId,
            kilde = null,
            type = OppgaveType.GENERELL_OPPGAVE,
            merknad = "Her må det komme tekst om hva saksbehandler må gjøre",
        )
}
