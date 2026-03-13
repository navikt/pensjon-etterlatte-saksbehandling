package no.nav.etterlatte.beregning.grunnlag

import no.nav.etterlatte.libs.common.beregning.BeregningsMetodeBeregningsgrunnlag
import no.nav.etterlatte.libs.common.beregning.BeregningsmetodeForAvdoed
import no.nav.etterlatte.libs.common.feilhaandtering.sjekk
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.SoeskenMedIBeregning
import no.nav.etterlatte.libs.common.periode.Periode
import java.util.UUID

data class BeregningsGrunnlag(
    val behandlingId: UUID,
    val kilde: Grunnlagsopplysning.Saksbehandler,
    val institusjonsopphold: List<GrunnlagMedPeriode<InstitusjonsoppholdBeregningsgrunnlag>> =
        emptyList(),
    val beregningsMetode: BeregningsMetodeBeregningsgrunnlag,
    val beregningsMetodeFlereAvdoede: List<GrunnlagMedPeriode<BeregningsmetodeForAvdoed>> = emptyList(),
    val soeskenMedIBeregning: List<GrunnlagMedPeriode<List<SoeskenMedIBeregning>>> = emptyList(),
    val kunEnJuridiskForelder: GrunnlagMedPeriode<TomVerdi>? = null,
    val vedtaksperioder: Vedtaksperioder?,
)

data class Vedtaksperioder(
    val perioder: List<Periode>,
) {
    init {
        sjekk(perioder.isNotEmpty()) {
            "Må ha perioder"
        }
        val perioderFoerSistePeriode = perioder.dropLast(1)
        sjekk(perioderFoerSistePeriode.none { it.tom == null }) {
            "Kun siste periode kan være åpen"
        }

        val perioderErIRiktigRekkefoelge =
            perioder
                .zipWithNext()
                .all { (first, second) -> first.tom != null && second.fom > first.tom }

        sjekk(perioderErIRiktigRekkefoelge) {
            "Perioder ikke i riktig rekkefølge / overlapper"
        }
    }
}
