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
import no.nav.etterlatte.libs.common.objectMapper
import java.time.Instant
import java.util.*
import kotlin.collections.ArrayList


class OpplysningsByggerService : OpplysningsBygger {

    override fun byggOpplysninger(
        inntektsKomponentenResponse: InntektsKomponentenResponse,
        arbeidsforholdListe: List<AaregResponse>
    ): List<Grunnlagsopplysning<out Any>> {

        val opplysninger = ArrayList<Grunnlagsopplysning<out Any>>()

        print(inntektsKomponentenResponse)
        // TODO: sjekk om det finnes inntekt for uføretrygd eller alderspensjon i løpet av de siste fem år
        val uforetrygd: List<Inntekt>
        val alderspensjon: List<Inntekt>
        if(inntektsKomponentenResponse.arbeidsInntektMaaned != null) {
            uforetrygd = harFaattUforetrygd(inntektsKomponentenResponse.arbeidsInntektMaaned)
            alderspensjon = harFaattAlderspensjon(inntektsKomponentenResponse.arbeidsInntektMaaned)
            opplysninger.add(lagOpplysning(Opplysningstyper.PENSJON_UFORE_V1,
                PensjonUforeOpplysning(uforetrygd, alderspensjon
            )))
        }

        if(arbeidsforholdListe.isNotEmpty()){
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
            val arbeidsForholdOpplysning = ArbeidsforholdOpplysning(arbeidsforhold)
            opplysninger.add(lagOpplysning(Opplysningstyper.ARBEIDSFORHOLD_V1, arbeidsForholdOpplysning))
        }

        return opplysninger
    }

    fun harFaattAlderspensjon(arbeidsInntektListe: List<ArbeidsInntektMaaned>): List<Inntekt> {
        val inntektListe = arrayListOf<Inntekt>()

        arbeidsInntektListe.forEach { inntektMaaned ->
            inntektMaaned.arbeidsInntektInformasjon.inntektListe?.forEach{ inntekt ->
                print(inntekt.beskrivelse)
                if(inntekt.beskrivelse == "alderspensjon") {
                    inntektListe.add(inntekt)
                }
            }
        }
        return inntektListe
    }

    fun harFaattUforetrygd(arbeidsInntektListe: List<ArbeidsInntektMaaned>): List<Inntekt> {
        val inntektListe = arrayListOf<Inntekt>()
        arbeidsInntektListe.forEach { inntektMaaned ->
            inntektMaaned.arbeidsInntektInformasjon.inntektListe?.forEach{ inntekt ->
                if(inntekt.beskrivelse == "ufoeretrygd") {
                    inntektListe.add(inntekt)
                }
            }
        }
        return inntektListe
    }

}

fun <T> lagOpplysning(opplysningsType: Opplysningstyper, opplysning: T): Grunnlagsopplysning<T> {
    return Grunnlagsopplysning(
        UUID.randomUUID(),
        Grunnlagsopplysning.Pdl("pdl", Instant.now(), null),
        opplysningsType,
        objectMapper.createObjectNode(),
        opplysning
    )
}

