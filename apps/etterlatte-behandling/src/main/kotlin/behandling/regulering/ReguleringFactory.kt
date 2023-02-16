package no.nav.etterlatte.behandling.revurdering

import no.nav.etterlatte.behandling.BehandlingDao
import no.nav.etterlatte.behandling.hendelse.HendelseDao
import no.nav.etterlatte.behandling.regulering.ReguleringAggregat
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import java.util.*

class ReguleringFactory(private val behandlinger: BehandlingDao, private val hendelser: HendelseDao) {
    fun hentRegulering(id: UUID): ReguleringAggregat =
        ReguleringAggregat(id, behandlinger, hendelser)

    fun opprettRegulering(
        sakId: Long,
        persongalleri: Persongalleri
    ): ReguleringAggregat =
        ReguleringAggregat.opprettRegulering(
            sakId,
            persongalleri,
            behandlinger,
            hendelser
        )
}