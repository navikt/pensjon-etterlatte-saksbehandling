package no.nav.etterlatte.behandling.regulering

import no.nav.etterlatte.behandling.BehandlingDao
import no.nav.etterlatte.behandling.hendelse.HendelseDao
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.behandling.RevurderingAarsak
import java.util.*

class RevurderingFactory(private val behandlinger: BehandlingDao, private val hendelser: HendelseDao) {
    fun hentRevurdering(id: UUID): RevurderingAggregat =
        RevurderingAggregat(id, behandlinger, hendelser)

    fun opprettRevurdering(
        sakId: Long,
        persongalleri: Persongalleri,
        revurderingAarsak: RevurderingAarsak
    ): RevurderingAggregat =
        RevurderingAggregat.opprettRevurdering(
            sakId,
            persongalleri,
            revurderingAarsak,
            behandlinger,
            hendelser
        )
}