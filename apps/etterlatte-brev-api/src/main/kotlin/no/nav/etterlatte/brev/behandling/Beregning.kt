package no.nav.etterlatte.brev.behandling

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
)

data class AvkortetBeregningsperiode(
    val datoFOM: LocalDate,
    val datoTOM: LocalDate?,
    val grunnbeloep: Kroner,
    val inntekt: Kroner,
    val aarsinntekt: Kroner,
    val fratrekkInnAar: Kroner,
    val relevanteMaanederInnAar: Int,
    val ytelseFoerAvkorting: Kroner,
    val restanse: Kroner,
    val trygdetid: Int,
    val utbetaltBeloep: Kroner,
    val beregningsMetodeAnvendt: BeregningsMetode,
    val beregningsMetodeFraGrunnlag: BeregningsMetode,
    val sanksjon: SanksjonertYtelse?,
)

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
)

fun List<Beregningsperiode>.hentUtbetaltBeloep(): Int {
    // TODO: Håndter grunnbeløpsendringer
    return this.last().utbetaltBeloep.value
}
