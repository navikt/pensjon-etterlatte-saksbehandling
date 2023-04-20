package no.nav.etterlatte.beregning

import no.nav.etterlatte.libs.common.periode.Periode
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import java.util.*

data class Avkorting(
    val behandlingId: UUID,
    val avkortingGrunnlag: List<AvkortingGrunnlag>,
    val beregningEtterAvkorting: List<BeregningEtterAvkorting>,
    val tidspunktForAvkorting: Tidspunkt
)

data class AvkortingGrunnlag(
    val periode: Periode,
    val aarsInntekt: Int,
    val gjeldendeAar: Int,
    val spesifikasjon: String
)

data class BeregningEtterAvkorting(
    val periode: Periode,
    val bruttoYtelse: Int,
    val avkorting: Int,
    val ytelseEtterAvkorting: Int
)