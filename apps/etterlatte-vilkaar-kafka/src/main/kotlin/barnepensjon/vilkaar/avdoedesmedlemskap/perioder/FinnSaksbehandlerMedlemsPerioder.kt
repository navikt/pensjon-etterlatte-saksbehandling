package barnepensjon.vilkaar.avdoedesmedlemskap.perioder

import no.nav.etterlatte.libs.common.grunnlag.Opplysning
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.AvdoedesMedlemskapsperiode
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.PeriodeType
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.SaksbehandlerMedlemskapsperiode
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.VurdertMedlemskapsperiode

fun finnSaksbehandlerMedlemsPerioder(
    medlemskap: Opplysning.Periodisert<SaksbehandlerMedlemskapsperiode?>
): List<VurdertMedlemskapsperiode> = medlemskap.perioder.mapNotNull { medlemskapsperiode ->
    medlemskapsperiode.verdi?.let {
        VurdertMedlemskapsperiode(
            periodeType = it.periodeType,
            id = it.id,
            arbeidsgiver = it.arbeidsgiver,
            stillingsprosent = it.stillingsprosent,
            begrunnelse = it.begrunnelse,
            kilde = it.kilde,
            oppgittKilde = it.oppgittKilde,
            fraDato = it.fraDato,
            tilDato = it.tilDato,
            godkjentPeriode = it.erGodkjent()
        )
    }
}

fun AvdoedesMedlemskapsperiode.erGodkjent(): Boolean = when (periodeType) {
    PeriodeType.ARBEIDSPERIODE -> this.stillingsprosent?.let { it.toDouble() >= 80 } ?: true
    else -> true
}