package no.nav.etterlatte.beregning.grunnlag

import no.nav.etterlatte.libs.common.beregning.BeregningsMetodeBeregningsgrunnlag
import no.nav.etterlatte.libs.common.beregning.BeregningsmetodeForAvdoed
import no.nav.etterlatte.libs.common.feilhaandtering.sjekk
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.SoeskenMedIBeregning
import java.time.YearMonth
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
    val vedtaksperioder: List<Vedtaksperiode>? = null,
)

data class Vedtaksperiode(
    val fraOgMed: YearMonth,
    val tilOgMed: YearMonth? = null,
)

fun List<Vedtaksperiode>.validerVedtaksperioder() {
    sjekk(isNotEmpty()) {
        "Må ha perioder"
    }
    val perioderFoerSistePeriode = dropLast(1)
    sjekk(perioderFoerSistePeriode.none { it.tilOgMed == null }) {
        "Kun siste periode kan være åpen"
    }

    val perioderErIRiktigRekkefoelge =
        zipWithNext()
            .all { (first, second) -> first.tilOgMed != null && second.fraOgMed > first.tilOgMed }

    sjekk(perioderErIRiktigRekkefoelge) {
        "Perioder ikke i riktig rekkefølge / overlapper"
    }
}
