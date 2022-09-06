package barnepensjon.vilkaar.avdoedesmedlemskap.perioder

import kombinerPerioder
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.AvdoedesMedlemskapGrunnlag
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.PeriodeType
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.VurdertMedlemskapsperiode
import tilVurderteMedlemskapsPerioder

fun finnPensjonEllerTrygdePerioder(
    grunnlag: AvdoedesMedlemskapGrunnlag,
    type: PeriodeType,
    beskrivelse: String
): List<VurdertMedlemskapsperiode> = grunnlag.inntektsOpplysning.opplysning.pensjonEllerTrygd
    .filter { it.beskrivelse == beskrivelse }
    .kombinerPerioder()
    .tilVurderteMedlemskapsPerioder(type, grunnlag.inntektsOpplysning.kilde)