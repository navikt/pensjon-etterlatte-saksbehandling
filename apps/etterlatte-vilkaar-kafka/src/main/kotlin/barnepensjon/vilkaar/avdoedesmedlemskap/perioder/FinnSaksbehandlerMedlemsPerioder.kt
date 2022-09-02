package barnepensjon.vilkaar.avdoedesmedlemskap.perioder

import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.AvdoedesMedlemskapGrunnlag
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.VurdertMedlemskapsPeriode

fun finnSaksbehandlerMedlemsPerioder(grunnlag: AvdoedesMedlemskapGrunnlag): List<VurdertMedlemskapsPeriode> =
    grunnlag.saksbehandlerMedlemsPerioder
        .map { opplysning ->
            val periode = opplysning.opplysning
            VurdertMedlemskapsPeriode(
                periodeType = periode.periodeType,
                beskrivelse = null,
                kilde = opplysning.kilde,
                fraDato = periode.fraDato,
                tilDato = periode.tilDato,
                godkjentPeriode = true
            )
        }