package no.nav.etterlatte.vedtaksvurdering

import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.beregning.AvkortingDto
import no.nav.etterlatte.libs.common.beregning.BeregnetEtteroppgjoerResultatDto
import no.nav.etterlatte.libs.common.beregning.BeregningDTO
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.trygdetid.TrygdetidDto
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingDto

data class VedtakData(
    val detaljertBehandling: DetaljertBehandling,
    val vilkaarsvurderingDto: VilkaarsvurderingDto? = null,
    val beregningOgAvkorting: BeregningOgAvkorting? = null,
    val sak: Sak,
    val trygdetid: List<TrygdetidDto>,
    val etteroppgjoerResultat: BeregnetEtteroppgjoerResultatDto? = null,
)

data class BeregningOgAvkorting(
    val beregning: BeregningDTO,
    val avkorting: AvkortingDto? = null,
)
