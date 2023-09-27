package no.nav.etterlatte.behandling.generellbehandling

import no.nav.etterlatte.libs.common.generellbehandling.GenerellBehandling
import no.nav.etterlatte.libs.common.generellbehandling.Innhold
import no.nav.etterlatte.libs.common.oppgave.OppgaveKilde
import no.nav.etterlatte.libs.common.oppgave.OppgaveType
import no.nav.etterlatte.oppgave.OppgaveService
import no.nav.etterlatte.token.Saksbehandler
import java.util.UUID

class GenerellBehandlingService(
    private val generellBehandlingDao: GenerellBehandlingDao,
    private val oppgaveService: OppgaveService,
) {
    fun opprettBehandling(generellBehandling: GenerellBehandling) {
        generellBehandlingDao.opprettGenerellbehandling(generellBehandling)
    }

    fun attesterBehandling(
        generellBehandling: GenerellBehandling,
        saksbehandler: Saksbehandler,
    ) {
        oppdaterBehandling(generellBehandling)
        when (generellBehandling.innhold) {
            is Innhold.Utland -> validerUtland(generellBehandling.innhold as Innhold.Utland)
            is Innhold.Annen -> throw NotImplementedError("Ikke implementert")
            null -> throw NotImplementedError("Ikke implementert")
        }
        oppgaveService.ferdigStillOppgaveUnderBehandling(generellBehandling.id.toString(), saksbehandler)
        oppgaveService.opprettNyOppgaveMedSakOgReferanse(
            generellBehandling.id.toString(),
            generellBehandling.sakId,
            OppgaveKilde.GENERELL_BEHANDLING,
            OppgaveType.ATTESTERING,
            merknad = "Attestering av utlandsbehandling",
        )
    }

    private fun validerUtland(innhold: Innhold.Utland) {
        if (innhold.landIsoKode.isEmpty() || innhold.landIsoKode.all { it.length == 3 }) {
            // TODO: slå opp mot kodeverk og validere her?
            throw IllegalArgumentException("Mangler land eller feil lengde på landisokode")
        }
        if (innhold.rinanummer.isEmpty()) {
            throw IllegalArgumentException("Må ha rinanummer")
        }
    }

    fun oppdaterBehandling(generellBehandling: GenerellBehandling) {
        generellBehandlingDao.oppdaterGenerellBehandling(generellBehandling)
    }

    fun hentBehandlingMedId(id: UUID): GenerellBehandling? {
        return generellBehandlingDao.hentGenerellBehandlingMedId(id)
    }

    fun hentBehandlingForSak(sakId: Long): List<GenerellBehandling> {
        return generellBehandlingDao.hentGenerellBehandlingForSak(sakId)
    }
}
