package no.nav.etterlatte.behandling.generellbehandling

import no.nav.etterlatte.libs.common.generellbehandling.GenerellBehandling
import java.util.UUID

class GenerellBehandlingService(private val generellBehandlingDao: GenerellBehandlingDao) {
    fun opprettBehandling(generellBehandling: GenerellBehandling) {
        generellBehandlingDao.opprettGenerellbehandling(generellBehandling)
    }

    fun hentBehandlingMedId(id: UUID): GenerellBehandling? {
        return generellBehandlingDao.hentGenerellBehandlingMedId(id)
    }

    fun hentBehandlingForSak(sakId: Long): List<GenerellBehandling> {
        return generellBehandlingDao.hentGenerellBehandlingForSak(sakId)
    }
}