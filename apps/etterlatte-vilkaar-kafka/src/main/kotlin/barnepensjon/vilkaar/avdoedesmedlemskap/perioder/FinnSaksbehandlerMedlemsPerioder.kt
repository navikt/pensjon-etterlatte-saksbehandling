package barnepensjon.vilkaar.avdoedesmedlemskap.perioder

import no.nav.etterlatte.libs.common.grunnlag.Opplysning
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.AvdoedesMedlemskapsperiode
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.PeriodeType
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.SaksbehandlerMedlemskapsperiode
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.VurdertMedlemskapsperiode

fun finnSaksbehandlerMedlemsPerioder(
    medlemskap: Opplysning.Periodisert<SaksbehandlerMedlemskapsperiode?>
): List<VurdertMedlemskapsperiode> = medlemskap.perioder.mapNotNull {
    val verdi = it.verdi
    if (verdi == null) {
        null
    } else {
        VurdertMedlemskapsperiode(
            periodeType = verdi.periodeType,
            id = verdi.id,
            arbeidsgiver = verdi.arbeidsgiver,
            stillingsprosent = verdi.stillingsprosent,
            begrunnelse = verdi.begrunnelse,
            kilde = verdi.kilde,
            oppgittKilde = verdi.oppgittKilde,
            fraDato = verdi.fraDato,
            tilDato = verdi.tilDato,
            godkjentPeriode = verdi.erGodkjent()
        )
    }
}

fun AvdoedesMedlemskapsperiode.erGodkjent(): Boolean = when (periodeType) {
    PeriodeType.ARBEIDSPERIODE -> this.stillingsprosent?.let { it.toDouble() >= 80 } ?: true
    else -> true
}