package no.nav.etterlatte.vedtaksvurdering

import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.beregning.AvkortingDto
import no.nav.etterlatte.libs.common.beregning.BeregningDTO
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingDto

data class VedtakData(
    val detaljertBehandling: DetaljertBehandling,
    val vilkaarsvurderingDto: VilkaarsvurderingDto? = null,
    val beregningOgAvkorting: BeregningOgAvkorting? = null,
    val sak: Sak
)

data class BeregningOgAvkorting(
    val beregning: BeregningDTO,
    val avkorting: AvkortingDto? = null
)