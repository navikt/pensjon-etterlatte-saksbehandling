package no.nav.etterlatte.behandling.omberegning

import no.nav.etterlatte.behandling.GenerellBehandlingService
import no.nav.etterlatte.behandling.revurdering.RevurderingFactory
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.common.behandling.RevurderingAarsak
import java.util.*

class OmberegningService(
    private val revurderingFactory: RevurderingFactory,
    private val behandlingService: GenerellBehandlingService
) {
    fun opprettOmberegning(
        sakId: Long,
        aarsak: RevurderingAarsak
    ): UUID {
        val forrigeBehandling = behandlingService.hentBehandlingerISak(sakId)
            .maxByOrNull { it.behandlingOpprettet }
            ?: throw IllegalArgumentException("Fant ikke forrige behandling i sak $sakId")
        return inTransaction {
            revurderingFactory.opprettRevurdering(
                sakId = sakId,
                persongalleri = forrigeBehandling.persongalleri,
                revurderingAarsak = aarsak
            )
        }.lagretBehandling.id
    }
}