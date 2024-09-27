package no.nav.etterlatte.brev.oppgave

import no.nav.etterlatte.brev.behandlingklient.OppgaveKlient
import no.nav.etterlatte.brev.model.BrevID
import no.nav.etterlatte.libs.common.oppgave.NyOppgaveDto
import no.nav.etterlatte.libs.common.oppgave.OppgaveIntern
import no.nav.etterlatte.libs.common.oppgave.OppgaveKilde
import no.nav.etterlatte.libs.common.oppgave.OppgaveType
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import org.slf4j.LoggerFactory

class OppgaveService(
    private val oppgaveKlient: OppgaveKlient,
) {
    private val logger = LoggerFactory.getLogger(OppgaveService::class.java)

    suspend fun opprettOppgaveForFeiletBrev(
        sakId: SakId,
        brevID: BrevID,
        brukerTokenInfo: BrukerTokenInfo,
    ): OppgaveIntern {
        logger.info("Oppretter oppgave for brev som feilet (sakId=$sakId, brevID=$brevID)")

        val nyOppgave =
            NyOppgaveDto(
                oppgaveKilde = OppgaveKilde.DOEDSHENDELSE,
                oppgaveType = OppgaveType.MANUELL_UTSENDING_BREV,
                merknad = "Kunne ikke sende informasjonsbrev automatisk for dødshendelse.",
                referanse = brevID.toString(),
            )

        return oppgaveKlient.opprettOppgave(sakId, nyOppgave, brukerTokenInfo)
    }
}
