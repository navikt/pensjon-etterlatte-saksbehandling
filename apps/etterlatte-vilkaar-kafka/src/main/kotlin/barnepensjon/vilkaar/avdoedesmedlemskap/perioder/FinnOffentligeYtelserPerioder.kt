package barnepensjon.vilkaar.avdoedesmedlemskap.perioder

import kombinerPerioder
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.AvdoedesMedlemskapGrunnlag
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.PeriodeType
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.VurdertMedlemskapsperiode
import tilVurderteMedlemskapsPerioder

fun finnOffentligeYtelserPerioder(grunnlag: AvdoedesMedlemskapGrunnlag): List<VurdertMedlemskapsperiode> =
    grunnlag.inntektsOpplysning.opplysning.ytelseFraOffentlig
        .groupBy { it.beskrivelse }
        .map {
            it.value
                .kombinerPerioder()
                .tilVurderteMedlemskapsPerioder(PeriodeType.OFFENTLIG_YTELSE, grunnlag.inntektsOpplysning.kilde, it.key)
        }.flatten()