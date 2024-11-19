package no.nav.etterlatte.brev.behandling

import no.nav.etterlatte.beregning.grunnlag.InstitusjonsoppholdBeregningsgrunnlag
import no.nav.etterlatte.beregning.grunnlag.Reduksjon
import no.nav.etterlatte.brev.model.OmstillingsstoenadBeregningsperiode
import no.nav.etterlatte.libs.common.IntBroek
import no.nav.etterlatte.libs.common.beregning.BeregningsMetode
import no.nav.etterlatte.libs.common.beregning.SanksjonertYtelse
import no.nav.pensjon.brevbaker.api.model.Kroner
import java.time.LocalDate

data class Utbetalingsinfo(
    val antallBarn: Int,
    val beloep: Kroner,
    val virkningsdato: LocalDate,
    val soeskenjustering: Boolean,
    val beregningsperioder: List<Beregningsperiode>,
)

data class Avkortingsinfo(
    val virkningsdato: LocalDate,
    val beregningsperioder: List<AvkortetBeregningsperiode>,
    val endringIUtbetalingVedVirk: Boolean,
)

data class AvkortetBeregningsperiode(
    val datoFOM: LocalDate,
    val datoTOM: LocalDate?,
    val grunnbeloep: Kroner,
    val inntekt: Kroner,
    val oppgittInntekt: Kroner,
    val fratrekkInnAar: Kroner,
    val innvilgaMaaneder: Int,
    val ytelseFoerAvkorting: Kroner,
    val restanse: Kroner,
    val trygdetid: Int,
    val utbetaltBeloep: Kroner,
    val beregningsMetodeAnvendt: BeregningsMetode,
    val beregningsMetodeFraGrunnlag: BeregningsMetode,
    val sanksjon: SanksjonertYtelse?,
    val institusjon: InstitusjonsoppholdBeregningsgrunnlag?,
    val erOverstyrtInnvilgaMaaneder: Boolean,
) {
    fun tilOmstillingsstoenadBeregningsperiode(): OmstillingsstoenadBeregningsperiode =
        OmstillingsstoenadBeregningsperiode(
            datoFOM = this.datoFOM,
            datoTOM = this.datoTOM,
            inntekt = this.inntekt,
            oppgittInntekt = this.oppgittInntekt,
            fratrekkInnAar = this.fratrekkInnAar,
            innvilgaMaaneder = this.innvilgaMaaneder,
            grunnbeloep = this.grunnbeloep,
            ytelseFoerAvkorting = this.ytelseFoerAvkorting,
            restanse = this.restanse,
            utbetaltBeloep = this.utbetaltBeloep,
            trygdetid = this.trygdetid,
            beregningsMetodeAnvendt = this.beregningsMetodeAnvendt,
            beregningsMetodeFraGrunnlag = this.beregningsMetodeFraGrunnlag,
            sanksjon = this.sanksjon != null,
            institusjon = this.institusjon != null && this.institusjon.reduksjon != Reduksjon.NEI_KORT_OPPHOLD,
            erOverstyrtInnvilgaMaaneder = this.erOverstyrtInnvilgaMaaneder,
        )
}

data class Beregningsperiode(
    val datoFOM: LocalDate,
    val datoTOM: LocalDate?,
    val grunnbeloep: Kroner,
    val antallBarn: Int,
    val utbetaltBeloep: Kroner,
    val trygdetid: Int,
    val trygdetidForIdent: String? = null,
    val prorataBroek: IntBroek?,
    val institusjon: Boolean,
    val beregningsMetodeAnvendt: BeregningsMetode,
    val beregningsMetodeFraGrunnlag: BeregningsMetode,
    val avdoedeForeldre: List<String?>? = null,
)

fun List<Beregningsperiode>.hentUtbetaltBeloep(): Int {
    // TODO: Håndter grunnbeløpsendringer
    return this.last().utbetaltBeloep.value
}
