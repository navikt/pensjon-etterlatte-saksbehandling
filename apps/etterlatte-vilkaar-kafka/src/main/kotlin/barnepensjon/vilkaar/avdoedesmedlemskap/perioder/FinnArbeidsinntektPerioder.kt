package barnepensjon.vilkaar.avdoedesmedlemskap.perioder

import kombinerPerioder
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.AvdoedesMedlemskapGrunnlag
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.PeriodeType
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.VurdertMedlemskapsperiode
import tilVurderteMedlemskapsPerioder

fun finnArbeidsinntektPerioder(grunnlag: AvdoedesMedlemskapGrunnlag): List<VurdertMedlemskapsperiode> =
    grunnlag.inntektsOpplysning.opplysning.let { inntektsOpplysning ->
        val loennsPerioder = inntektsOpplysning.loennsinntekt
            .kombinerPerioder()
            .tilVurderteMedlemskapsPerioder(PeriodeType.LOENNSINNTEKT, grunnlag.inntektsOpplysning.kilde)

        val naeringsPerioder = inntektsOpplysning.naeringsinntekt
            .kombinerPerioder()
            .tilVurderteMedlemskapsPerioder(PeriodeType.NAERINGSINNTEKT, grunnlag.inntektsOpplysning.kilde)

        loennsPerioder + naeringsPerioder
    }