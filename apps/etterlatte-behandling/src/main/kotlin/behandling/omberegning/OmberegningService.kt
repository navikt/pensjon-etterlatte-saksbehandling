package no.nav.etterlatte.behandling.omberegning

import no.nav.etterlatte.behandling.revurdering.RevurderingFactory
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.behandling.RevurderingAarsak

class OmberegningService(private val revurderingFactory: RevurderingFactory) {
    fun opprettOmberegning(
        persongalleri: Persongalleri,
        sakId: Long,
        aarsak: RevurderingAarsak
    ) =
        inTransaction {
            revurderingFactory.opprettRevurdering(
                sakId = sakId,
                persongalleri = persongalleri,
                revurderingAarsak = aarsak
            )
        }
}