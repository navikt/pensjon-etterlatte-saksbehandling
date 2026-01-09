package no.nav.etterlatte.brev.model

import no.nav.etterlatte.libs.common.beregning.BeregningsMetode
import no.nav.pensjon.brevbaker.api.model.Kroner
import java.time.LocalDate

data class OmstillingsstoenadBeregningsperiode(
    val datoFOM: LocalDate,
    val datoTOM: LocalDate?,
    val inntekt: Kroner,
    val oppgittInntekt: Kroner,
    val fratrekkInnAar: Kroner,
    val innvilgaMaaneder: Int,
    val grunnbeloep: Kroner,
    val ytelseFoerAvkorting: Kroner,
    val restanse: Kroner,
    val utbetaltBeloep: Kroner,
    val trygdetid: Int,
    val beregningsMetodeAnvendt: BeregningsMetode,
    val beregningsMetodeFraGrunnlag: BeregningsMetode,
    val sanksjon: Boolean,
    val institusjon: Boolean,
    val erOverstyrtInnvilgaMaaneder: Boolean,
)
