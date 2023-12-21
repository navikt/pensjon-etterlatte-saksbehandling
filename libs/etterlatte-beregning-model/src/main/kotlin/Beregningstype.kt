package no.nav.etterlatte.libs.common.beregning

import no.nav.etterlatte.beregning.grunnlag.GrunnlagMedPeriode
import no.nav.etterlatte.beregning.grunnlag.InstitusjonsoppholdBeregningsgrunnlag
import no.nav.etterlatte.libs.common.IntBroek
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.SoeskenMedIBeregning
import no.nav.etterlatte.regler.Beregningstall
import java.util.UUID

// TODO kunne man like gjerne brukt sakType?
enum class Beregningstype {
    BP,
    OMS,
}

data class BeregningsGrunnlag(
    val behandlingId: UUID,
    val kilde: Grunnlagsopplysning.Saksbehandler,
    val soeskenMedIBeregning: List<GrunnlagMedPeriode<List<SoeskenMedIBeregning>>>,
    val institusjonsoppholdBeregningsgrunnlag: List<GrunnlagMedPeriode<InstitusjonsoppholdBeregningsgrunnlag>> =
        emptyList(),
    val beregningsMetode: BeregningsMetodeBeregningsgrunnlag,
)

enum class BeregningsMetode {
    BEST,
    NASJONAL,
    PRORATA,
}

data class BeregningsMetodeBeregningsgrunnlag(
    val beregningsMetode: BeregningsMetode,
    val begrunnelse: String? = null,
)

data class SamletTrygdetidMedBeregningsMetode(
    val beregningsMetode: BeregningsMetode,
    val samletTrygdetidNorge: Beregningstall?,
    val samletTrygdetidTeoretisk: Beregningstall?,
    val prorataBroek: IntBroek?,
    val ident: String?,
) {
    fun broek() = Beregningstall(this.prorataBroek?.let { it.teller.toDouble() / it.nevner.toDouble() } ?: 1.0)
}
