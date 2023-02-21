package no.nav.etterlatte.behandling.revurdering

import no.nav.etterlatte.behandling.BehandlingDao
import no.nav.etterlatte.behandling.domain.Behandling
import no.nav.etterlatte.behandling.hendelse.HendelseDao
import no.nav.etterlatte.behandling.regulering.ReguleringAggregat
import java.time.LocalDate
import java.util.*

class ReguleringFactory(private val behandlinger: BehandlingDao, private val hendelser: HendelseDao) {
    fun hentRegulering(id: UUID): ReguleringAggregat =
        ReguleringAggregat(id, behandlinger, hendelser)

    fun opprettRegulering(
        sakId: Long,
        forrigeBehandling: Behandling,
        fradato: LocalDate
    ): ReguleringAggregat =
        ReguleringAggregat.opprettRegulering(
            sakId,
            forrigeBehandling,
            fradato,
            behandlinger,
            hendelser
        )
}