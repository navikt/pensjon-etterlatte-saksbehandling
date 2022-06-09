package no.nav.etterlatte.opplysninger.kilde.inntektskomponenten

import no.nav.etterlatte.OpplysningsBygger
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstyper
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.soeknad.dataklasser.Barnepensjon
import java.time.Instant
import java.util.*

class OpplysningsByggerService : OpplysningsBygger {

    override fun byggOpplysninger(
        inntektsKomponentenResponse: InntektsKomponentenResponse
    ): List<Grunnlagsopplysning<out Any>> {

        // TODO: sjekk om det finnes inntekt for uføretrygd eller alderspensjon i løpet av de siste fem år
        var uforetrygd = false
        var alderspensjon = false
        if(inntektsKomponentenResponse.arbeidsInntektMaaned != null) {
            uforetrygd = harFaattUforetrygd(inntektsKomponentenResponse.arbeidsInntektMaaned)
            alderspensjon = harFaatAlderspensjon(inntektsKomponentenResponse.arbeidsInntektMaaned)
            return listOf(lagOpplysning(Opplysningstyper.PENSJON_UFORE_V1,
                PensjonUforeOpplysning(uforetrygd, alderspensjon, inntektsKomponentenResponse.arbeidsInntektMaaned)))
        }

        throw Exception("Ingen grunnlagsopplysninger tilgjengelig for avdød")

    }

    // TODO - simpel sjekk. Vil vi ha ut noe mer?
    fun harFaatAlderspensjon(arbeidsInntektListe: List<ArbeidsInntektMaaned>): Boolean {
        arbeidsInntektListe.forEach { inntektMaaned ->
            inntektMaaned.arbeidsInntektInformasjon.inntektListe.forEach{ inntekt ->
                if(inntekt.inntektType === "ufoeretrygd") {
                    return true
                }
            }
        }
        return false
    }

    // TODO - simpel sjekk. Vil vi ha ut noe mer?
    fun harFaattUforetrygd(arbeidsInntektListe: List<ArbeidsInntektMaaned>): Boolean {
        arbeidsInntektListe.forEach { inntektMaaned ->
            inntektMaaned.arbeidsInntektInformasjon.inntektListe.forEach{ inntekt ->
                if(inntekt.inntektType === "alderspensjon") {
                    return true
                }
            }
        }
        return false
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

data class PensjonUforeOpplysning(val mottattUforetrygd: Boolean, val mottattAlderspensjon: Boolean, val grunnlag: List<ArbeidsInntektMaaned>)