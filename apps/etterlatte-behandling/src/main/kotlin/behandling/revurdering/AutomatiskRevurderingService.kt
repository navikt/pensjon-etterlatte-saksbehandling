package no.nav.etterlatte.behandling.revurdering

import no.nav.etterlatte.behandling.domain.Behandling
import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.behandling.Revurderingaarsak
import java.time.LocalDate

class AutomatiskRevurderingService(private val revurderingService: RevurderingService) {
    fun opprettAutomatiskRevurdering(
        sakId: Long,
        forrigeBehandling: Behandling,
        revurderingAarsak: Revurderingaarsak,
        virkningstidspunkt: LocalDate? = null,
        kilde: Vedtaksloesning,
        persongalleri: Persongalleri,
        mottattDato: String? = null,
        begrunnelse: String? = null,
    ) = revurderingService.opprettAutomatiskRevurdering(
        sakId,
        forrigeBehandling,
        revurderingAarsak,
        virkningstidspunkt,
        kilde,
        persongalleri,
        mottattDato,
        begrunnelse,
    )
}
