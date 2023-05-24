package no.nav.etterlatte.avkorting

import com.fasterxml.jackson.databind.JsonNode
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.periode.Periode
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import java.util.*

data class Avkorting(
    val behandlingId: UUID,
    val avkortingGrunnlag: List<AvkortingGrunnlag>,
    val avkortetYtelse: List<AvkortetYtelse>
)

data class AvkortingGrunnlag(
    val periode: Periode,
    val aarsinntekt: Int,
    val spesifikasjon: String,
    val beregnetAvkorting: List<BeregnetAvkortingGrunnlag>,
    val kilde: Grunnlagsopplysning.Saksbehandler
)

data class BeregnetAvkortingGrunnlag(
    val periode: Periode,
    val avkorting: Int,
    val tidspunkt: Tidspunkt,
    val regelResultat: JsonNode,
    val kilde: Grunnlagsopplysning.RegelKilde
)

data class AvkortetYtelse(
    val periode: Periode,
    val ytelseEtterAvkorting: Int,
    val avkortingsbeloep: Int,
    val ytelseFoerAvkorting: Int,
    val tidspunkt: Tidspunkt,
    val regelResultat: JsonNode,
    val kilde: Grunnlagsopplysning.RegelKilde
)