package barnepensjon.vilkaar.avdoedesmedlemskap.perioder

import kombinerPerioder
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.AvdoedesMedlemskapGrunnlag
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.PeriodeType
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.VurdertMedlemskapsPeriode
import tilVurderteMedlemskapsPerioder

fun finnPensjonEllerTrygdePerioder(
    grunnlag: AvdoedesMedlemskapGrunnlag,
    type: PeriodeType,
    beskrivelse: String
): List<VurdertMedlemskapsPeriode> = grunnlag.inntektsOpplysning.opplysning.pensjonEllerTrygd
    .filter { it.beskrivelse == beskrivelse }
    .kombinerPerioder()
    .tilVurderteMedlemskapsPerioder(type, grunnlag.inntektsOpplysning.kilde)