package no.nav.etterlatte.brev.behandling

import no.nav.etterlatte.libs.common.IntBroek
import no.nav.etterlatte.libs.common.beregning.BeregningsMetode
import no.nav.pensjon.brevbaker.api.model.Kroner
import java.time.LocalDate

data class Utbetalingsinfo(
    val overstyrt: Boolean,
    val antallBarn: Int,
    val beloep: Kroner,
    val virkningsdato: LocalDate,
    val soeskenjustering: Boolean,
    val beregningsperioder: List<Beregningsperiode>,
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
    val avdoedeForeldre: List<String?>?,
    val harForeldreloessats: Boolean?,
)

fun List<Beregningsperiode>.hentUtbetaltBeloep(): Int {
    // TODO: Håndter grunnbeløpsendringer
    return this.last().utbetaltBeloep.value
}
