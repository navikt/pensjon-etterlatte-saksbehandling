package no.nav.etterlatte.behandling.generellbehandling

import no.nav.etterlatte.libs.common.generellbehandling.GenerellBehandling
import no.nav.etterlatte.libs.common.generellbehandling.Innhold
import no.nav.etterlatte.libs.common.oppgave.OppgaveIntern
import no.nav.etterlatte.libs.common.oppgave.OppgaveKilde
import no.nav.etterlatte.libs.common.oppgave.OppgaveType
import no.nav.etterlatte.oppgave.OppgaveService
import no.nav.etterlatte.token.Saksbehandler
import org.slf4j.LoggerFactory
import java.util.UUID

class GenerellBehandlingService(
    private val generellBehandlingDao: GenerellBehandlingDao,
    private val oppgaveService: OppgaveService,
) {
    private val logger = LoggerFactory.getLogger(GenerellBehandlingService::class.java)

    fun opprettBehandling(generellBehandling: GenerellBehandling): GenerellBehandling {
        val opprettetbehandling = generellBehandlingDao.opprettGenerellbehandling(generellBehandling)
        val oppgaveForGenerellBehandling =
            oppgaveService.opprettNyOppgaveMedSakOgReferanse(
                opprettetbehandling.id.toString(),
                opprettetbehandling.sakId,
                OppgaveKilde.GENERELL_BEHANDLING,
                OppgaveType.UTLAND,
                null,
            )
        tildelSaksbehandlerTilNyOppgaveHvisFinnes(oppgaveForGenerellBehandling, opprettetbehandling)
        return opprettetbehandling
    }

    private fun tildelSaksbehandlerTilNyOppgaveHvisFinnes(
        oppgave: OppgaveIntern,
        generellBehandling: GenerellBehandling,
    ) {
        if (generellBehandling.tilknyttetBehandling !== null) {
            val kanskjeOppgaveMedSaksbehandler =
                oppgaveService.hentOppgaveForSaksbehandlerFraFoerstegangsbehandling(
                    behandlingId = generellBehandling.tilknyttetBehandling!!,
                )
            if (kanskjeOppgaveMedSaksbehandler != null) {
                val saksbehandler = kanskjeOppgaveMedSaksbehandler.saksbehandler
                if (saksbehandler != null) {
                    oppgaveService.tildelSaksbehandler(oppgave.id, saksbehandler)
                    logger.info(
                        "Opprettet generell behandling for utland for sak: ${generellBehandling.sakId} " +
                            "og behandling: ${generellBehandling.tilknyttetBehandling}. Gjelder oppgave: ${oppgave.id}",
                    )
                }
            }
        }
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

    fun oppdaterBehandling(generellBehandling: GenerellBehandling): GenerellBehandling {
        return generellBehandlingDao.oppdaterGenerellBehandling(generellBehandling)
    }

    fun hentBehandlingMedId(id: UUID): GenerellBehandling? {
        return generellBehandlingDao.hentGenerellBehandlingMedId(id)
    }

    fun hentBehandlingForSak(sakId: Long): List<GenerellBehandling> {
        return generellBehandlingDao.hentGenerellBehandlingForSak(sakId)
    }
}
