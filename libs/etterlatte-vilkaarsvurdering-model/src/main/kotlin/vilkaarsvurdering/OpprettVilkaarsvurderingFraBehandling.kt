package no.nav.etterlatte.vilkaarsvurdering

import vilkaarsvurdering.Vilkaarsvurdering
import java.util.UUID

data class OpprettVilkaarsvurderingFraBehandling(
    val forrigeBehandling: UUID,
    val vilkaarsvurdering: Vilkaarsvurdering,
)

data class OpprettVilkaarsvurderingMotBehandling(
    val forrigeBehandling: UUID,
)
