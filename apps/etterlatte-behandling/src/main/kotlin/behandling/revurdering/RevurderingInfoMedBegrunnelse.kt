package no.nav.etterlatte.behandling.revurdering

import no.nav.etterlatte.libs.common.behandling.RevurderingInfo

data class RevurderingInfoMedBegrunnelse(
    val revurderingInfo: RevurderingInfo?,
    val begrunnelse: String?,
)
