package no.nav.etterlatte.beregning

import com.fasterxml.jackson.databind.JsonNode
import no.nav.etterlatte.libs.common.periode.Periode
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import java.util.*

data class Avkorting(
    val behandlingId: UUID,
    val avkortingGrunnlag: List<AvkortingGrunnlag>,
    val beregningEtterAvkorting: List<BeregningEtterAvkorting>
)

data class AvkortingGrunnlag(
    val periode: Periode,
    val aarsinntekt: Int,
    val gjeldendeAar: Int,
    val spesifikasjon: String,
    val beregnetAvkorting: List<BeregnetAvkortingGrunnlag>
)

data class BeregnetAvkortingGrunnlag(
    val periode: Periode,
    val avkorting: Int,
    val tidspunkt: Tidspunkt,
    val regelResultat: JsonNode
)

data class BeregningEtterAvkorting(
    val periode: Periode,
    val bruttoYtelse: Int,
    val avkorting: Int,
    val ytelseEtterAvkorting: Int
)