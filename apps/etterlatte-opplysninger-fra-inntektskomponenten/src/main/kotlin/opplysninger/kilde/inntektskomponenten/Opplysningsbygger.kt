package no.nav.etterlatte.opplysninger.kilde.inntektskomponenten

import no.nav.etterlatte.OpplysningsBygger
import no.nav.etterlatte.libs.common.arbeidsforhold.AaregResponse
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.periode.Periode
import no.nav.etterlatte.libs.common.person.Foedselsnummer
import java.time.Instant
import java.time.YearMonth
import java.util.*

class OpplysningsByggerService : OpplysningsBygger {

    override fun byggOpplysninger(
        inntektsKomponentenResponse: InntektsKomponentenResponse,
        arbeidsforholdListe: List<AaregResponse>,
        fnr: Foedselsnummer
    ): List<Grunnlagsopplysning<out Any?>> {
        val innhentetTidspunkt = Instant.now()
        val forrigeMåned = YearMonth.now().minusMonths(1)

        val periodisertInntekt = inntektsKomponentenResponse.arbeidsInntektMaaned?.map {
            lagOpplysning(
                opplysningsType = Opplysningstype.INNTEKT,
                kilde = Grunnlagsopplysning.Aordningen(innhentetTidspunkt),
                opplysning = it.arbeidsInntektInformasjon,
                periode = Periode(
                    fom = it.aarMaaned,
                    tom = it.aarMaaned
                ),
                fnr = fnr
            )
        } ?: listOf(
            Grunnlagsopplysning.empty(
                opplysningType = Opplysningstype.INNTEKT,
                kilde = Grunnlagsopplysning.Aordningen(innhentetTidspunkt),
                fnr = fnr,
                fom = YearMonth.from(fnr.getBirthDate()),
                tom = forrigeMåned
            )
        )

        val arbeidsforholdOpplysning = arbeidsforholdListe.takeIf { it.isNotEmpty() }?.map {
            lagOpplysning(
                opplysningsType = Opplysningstype.ARBEIDSFORHOLD,
                kilde = Grunnlagsopplysning.Aordningen(innhentetTidspunkt),
                opplysning = it,
                periode = Periode(
                    fom = YearMonth.from(it.ansettelsesperiode.startdato),
                    tom = it.ansettelsesperiode.sluttdato?.let { tom -> YearMonth.from(tom) }
                ),
                fnr = fnr
            )
        } ?: listOf(
            Grunnlagsopplysning.empty(
                opplysningType = Opplysningstype.ARBEIDSFORHOLD,
                kilde = Grunnlagsopplysning.Aordningen(innhentetTidspunkt),
                fnr = fnr,
                fom = YearMonth.from(fnr.getBirthDate())
            )
        )

        return periodisertInntekt + arbeidsforholdOpplysning
    }
}

fun <T : Any> lagOpplysning(
    opplysningsType: Opplysningstype,
    kilde: Grunnlagsopplysning.Kilde,
    opplysning: T,
    fnr: Foedselsnummer?,
    periode: Periode? = null
): Grunnlagsopplysning<T> {
    return Grunnlagsopplysning(
        id = UUID.randomUUID(),
        kilde = kilde,
        opplysningType = opplysningsType,
        meta = objectMapper.createObjectNode(),
        opplysning = opplysning,
        periode = periode,
        fnr = fnr
    )
}