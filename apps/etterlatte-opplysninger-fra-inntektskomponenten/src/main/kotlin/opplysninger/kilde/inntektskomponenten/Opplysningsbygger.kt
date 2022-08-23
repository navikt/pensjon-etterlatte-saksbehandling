package no.nav.etterlatte.opplysninger.kilde.inntektskomponenten

import no.nav.etterlatte.OpplysningsBygger
import no.nav.etterlatte.libs.common.arbeidsforhold.AaregResponse
import no.nav.etterlatte.libs.common.arbeidsforhold.ArbeidsForhold
import no.nav.etterlatte.libs.common.arbeidsforhold.ArbeidsforholdOpplysning
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstyper
import no.nav.etterlatte.libs.common.inntekt.ArbeidsInntektMaaned
import no.nav.etterlatte.libs.common.inntekt.Inntekt
import no.nav.etterlatte.libs.common.inntekt.PensjonUforeOpplysning
import no.nav.etterlatte.libs.common.inntekt.UtbetaltPeriode
import no.nav.etterlatte.libs.common.objectMapper
import java.time.YearMonth
import java.util.*

class OpplysningsByggerService : OpplysningsBygger {

    override fun byggOpplysninger(
        inntektsKomponentenResponse: InntektsKomponentenResponse,
        arbeidsforholdListe: List<AaregResponse>
    ): List<Grunnlagsopplysning<out Any>> {
        val pensjonUforeOpplysning = inntektsKomponentenResponse.arbeidsInntektMaaned?.let { inntekter ->
            val uforetrygd = mapToPeriode(inntekter.filtrertPaaType("ufoeretrygd"))
            val alderspensjon = mapToPeriode(inntekter.filtrertPaaType("alderspensjon"))

            lagOpplysning(
                opplysningsType = Opplysningstyper.PENSJON_UFORE_V1,
                kilde = Grunnlagsopplysning.Inntektskomponenten("Inntektskomponenten"),
                opplysning = PensjonUforeOpplysning(uforetrygd, alderspensjon)
            )
        }

        val arbeidsforholdOpplysning = if (arbeidsforholdListe.isNotEmpty()) {
            val arbeidsforhold = arbeidsforholdListe.map { t ->
                ArbeidsForhold(
                    t.type,
                    t.arbeidstaker,
                    t.arbeidssted,
                    t.ansettelsesdetaljer,
                    t.bruksperiode,
                    t.ansettelsesperiode
                )
            }
            lagOpplysning(
                Opplysningstyper.ARBEIDSFORHOLD_V1,
                Grunnlagsopplysning.Aregisteret("Aareg"),
                ArbeidsforholdOpplysning(arbeidsforhold)
            )
        } else {
            null
        }

        return listOfNotNull(pensjonUforeOpplysning, arbeidsforholdOpplysning)
    }
}

fun List<ArbeidsInntektMaaned>.filtrertPaaType(beskrivelse: String) = this.mapNotNull { arbeidsinntekt ->
    arbeidsinntekt.arbeidsInntektInformasjon.inntektListe?.filter { it.beskrivelse == beskrivelse }
}.flatten()

fun <T> lagOpplysning(
    opplysningsType: Opplysningstyper,
    kilde: Grunnlagsopplysning.Kilde,
    opplysning: T
): Grunnlagsopplysning<T> {
    return Grunnlagsopplysning(
        UUID.randomUUID(),
        kilde,
        opplysningsType,
        objectMapper.createObjectNode(),
        opplysning
    )
}

fun mapToPeriode(liste: List<Inntekt>): List<UtbetaltPeriode> = liste.map {
    UtbetaltPeriode(YearMonth.parse(it.utbetaltIMaaned), it.beloep)
}