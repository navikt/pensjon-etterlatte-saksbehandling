package no.nav.etterlatte.behandling

import no.nav.etterlatte.behandling.domain.Behandling
import no.nav.etterlatte.libs.common.sak.BehandlingOgSak
import no.nav.etterlatte.libs.common.sak.SakId

interface BehandlingStatusDao {
    fun lagreStatus(lagretBehandling: Behandling)

    fun migrerStatusPaaAlleBehandlingerSomTrengerNyBeregning(sakIder: List<SakId>): List<BehandlingOgSak>

    fun hentAapneBehandlinger(sakIder: List<SakId>): List<BehandlingOgSak>
}
