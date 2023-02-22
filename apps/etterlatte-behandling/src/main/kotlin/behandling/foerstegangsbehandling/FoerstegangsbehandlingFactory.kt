package no.nav.etterlatte.behandling.foerstegangsbehandling

import no.nav.etterlatte.behandling.BehandlingDao
import no.nav.etterlatte.behandling.hendelse.HendelseDao
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.behandling.SakType
import java.util.*

class FoerstegangsbehandlingFactory(private val behandlinger: BehandlingDao, private val hendelser: HendelseDao) {
    fun hentFoerstegangsbehandling(id: UUID): FoerstegangsbehandlingAggregat =
        FoerstegangsbehandlingAggregat(id, behandlinger, hendelser)

    fun opprettFoerstegangsbehandling(
        sakId: Long,
        sakType: SakType,
        mottattDato: String,
        persongalleri: Persongalleri
    ): FoerstegangsbehandlingAggregat =
        FoerstegangsbehandlingAggregat.opprettFoerstegangsbehandling(
            sakId,
            sakType,
            mottattDato,
            persongalleri,
            behandlinger,
            hendelser
        )
}