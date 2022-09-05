package barnepensjon.vilkaar.avdoedesmedlemskap.perioder

import kombinerPerioder
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.AvdoedesMedlemskapGrunnlag
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.PeriodeType
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.VurdertMedlemskapsPeriode
import tilVurderteMedlemskapsPerioder

fun finnOffentligeYtelserPerioder(grunnlag: AvdoedesMedlemskapGrunnlag): List<VurdertMedlemskapsPeriode> =
    grunnlag.inntektsOpplysning.opplysning.ytelseFraOffentlig
        .groupBy { it.beskrivelse }
        .map {
            it.value
                .kombinerPerioder()
                .tilVurderteMedlemskapsPerioder(PeriodeType.OFFENTLIG_YTELSE, grunnlag.inntektsOpplysning.kilde, it.key)
        }.flatten()