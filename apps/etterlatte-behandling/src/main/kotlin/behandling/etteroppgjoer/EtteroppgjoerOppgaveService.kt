package no.nav.etterlatte.behandling.etteroppgjoer

import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import no.nav.etterlatte.libs.common.oppgave.OppgaveKilde
import no.nav.etterlatte.libs.common.oppgave.OppgaveType
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.oppgave.OppgaveService

// TODO: overkill men plassere felles her
class EtteroppgjoerOppgaveService(
    private val oppgaveService: OppgaveService,
) {
    fun opprettOppgaveForOpprettForbehandling(
        sakId: SakId,
        merknad: String? = null,
    ) {
        val defaultMerknad = "Etteroppgjøret for ${2024} er klart til behandling"

        val eksisterendeOppgaver = oppgaveService.hentOppgaverForSakAvType(sakId, listOf(OppgaveType.ETTEROPPGJOER))
        if (eksisterendeOppgaver.isEmpty()) {
            oppgaveService.opprettOppgave(
                referanse = "",
                sakId = sakId,
                kilde = OppgaveKilde.HENDELSE,
                type = OppgaveType.ETTEROPPGJOER,
                merknad = merknad ?: defaultMerknad,
            )
        } else {
            throw InternfeilException(
                "Forsøker å opprette ny oppgave om opprette forbehandling, " +
                    "men det eksisterer allerede ${eksisterendeOppgaver.size} oppgave(r) for sakId=$sakId",
            )
        }
    }
}
