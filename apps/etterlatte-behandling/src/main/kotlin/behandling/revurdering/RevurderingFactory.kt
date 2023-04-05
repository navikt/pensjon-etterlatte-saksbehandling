package no.nav.etterlatte.behandling.revurdering

import no.nav.etterlatte.behandling.BehandlingDao
import no.nav.etterlatte.behandling.domain.Behandling
import no.nav.etterlatte.behandling.hendelse.HendelseDao
import no.nav.etterlatte.libs.common.behandling.RevurderingAarsak
import java.time.LocalDate
import java.util.*

class RevurderingFactory(private val behandlinger: BehandlingDao, private val hendelser: HendelseDao) {
    fun hentRevurdering(id: UUID): RevurderingAggregat =
        RevurderingAggregat(id, behandlinger, hendelser)

    fun opprettManuellRevurdering(
        sakId: Long,
        forrigeBehandling: Behandling,
        revurderingAarsak: RevurderingAarsak
    ): RevurderingAggregat =
        RevurderingAggregat.opprettManuellRevurdering(
            sak = sakId,
            forrigeBehandling = forrigeBehandling,
            revurderingAarsak = revurderingAarsak,
            behandlinger = behandlinger,
            hendelser = hendelser
        )

    fun opprettAutomatiskRevurdering(
        sakId: Long,
        forrigeBehandling: Behandling,
        fradato: LocalDate,
        revurderingAarsak: RevurderingAarsak
    ): RevurderingAggregat =
        RevurderingAggregat.opprettAutomatiskRevurdering(
            sak = sakId,
            forrigeBehandling = forrigeBehandling,
            fraDato = fradato,
            behandlinger = behandlinger,
            hendelser = hendelser,
            revurderingAarsak = revurderingAarsak
        )
}