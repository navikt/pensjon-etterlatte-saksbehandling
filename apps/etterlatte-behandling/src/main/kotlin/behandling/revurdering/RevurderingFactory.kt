package no.nav.etterlatte.behandling.revurdering

import no.nav.etterlatte.behandling.BehandlingDao
import no.nav.etterlatte.behandling.HendelseDao
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import java.util.*

class RevurderingFactory(private val behandlinger: BehandlingDao, private val hendelser: HendelseDao) {
    fun hentRevurdering(id: UUID): RevurderingAggregat =
        RevurderingAggregat(id, behandlinger, hendelser)

    fun opprettRevurdering(
        sakId: Long,
        persongalleri: Persongalleri,
        mottattDato: String
    ): RevurderingAggregat =
        RevurderingAggregat.opprettRevurdering(sakId, persongalleri, mottattDato, behandlinger, hendelser)
}