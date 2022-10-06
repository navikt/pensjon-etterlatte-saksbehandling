package no.nav.etterlatte.opplysninger.kilde.inntektskomponenten

import no.nav.etterlatte.OpplysningsBygger
import no.nav.etterlatte.libs.common.arbeidsforhold.AaregResponse
import no.nav.etterlatte.libs.common.arbeidsforhold.ArbeidsforholdOpplysning
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstyper
import no.nav.etterlatte.libs.common.inntekt.ArbeidsInntektMaaned
import no.nav.etterlatte.libs.common.inntekt.InntektType
import no.nav.etterlatte.libs.common.inntekt.InntektsOpplysning
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.person.Foedselsnummer
import java.time.Instant
import java.util.*

class OpplysningsByggerService : OpplysningsBygger {

    override fun byggOpplysninger(
        inntektsKomponentenResponse: InntektsKomponentenResponse,
        arbeidsforholdListe: List<AaregResponse>,
        fnr: Foedselsnummer
    ): List<Grunnlagsopplysning<out Any>> {
        val inntekter = inntektsKomponentenResponse.arbeidsInntektMaaned?.let { inntekter ->
            val pensjonEllerTrygd = inntekter.filtrertPaaInntektsType(InntektType.PENSJON_ELLER_TRYGD)
            val ytelseFraOffentlig = inntekter.filtrertPaaInntektsType(InntektType.YTELSE_FRA_OFFENTLIGE)
            val loennsinntekt = inntekter.filtrertPaaInntektsType(InntektType.LOENNSINNTEKT)
            val naeringsinntekt = inntekter.filtrertPaaInntektsType(InntektType.NAERINGSINNTEKT)

            InntektsOpplysning(pensjonEllerTrygd, ytelseFraOffentlig, loennsinntekt, naeringsinntekt)
        } ?: InntektsOpplysning(emptyList(), emptyList(), emptyList(), emptyList())

        val inntektsOpplysning = lagOpplysning(
            opplysningsType = Opplysningstyper.INNTEKT,
            kilde = Grunnlagsopplysning.Aordningen(Instant.now()),
            opplysning = inntekter,
            fnr = fnr
        )

        val arbeidsforholdOpplysning = lagOpplysning(
            Opplysningstyper.ARBEIDSFORHOLD_V1,
            Grunnlagsopplysning.AAregisteret(Instant.now()),
            ArbeidsforholdOpplysning(arbeidsforholdListe),
            fnr
        )
        return listOf(inntektsOpplysning, arbeidsforholdOpplysning)
    }
}

fun List<ArbeidsInntektMaaned>.filtrertPaaInntektsType(inntektType: InntektType) = this.mapNotNull { arbeidsinntekt ->
    arbeidsinntekt.arbeidsInntektInformasjon.inntektListe?.filter { it.inntektType == inntektType }
}.flatten()

fun <T> lagOpplysning(
    opplysningsType: Opplysningstyper,
    kilde: Grunnlagsopplysning.Kilde,
    opplysning: T,
    fnr: Foedselsnummer?
): Grunnlagsopplysning<T> {
    return Grunnlagsopplysning(
        UUID.randomUUID(),
        kilde,
        opplysningsType,
        objectMapper.createObjectNode(),
        opplysning,
        null,
        fnr
    )
}