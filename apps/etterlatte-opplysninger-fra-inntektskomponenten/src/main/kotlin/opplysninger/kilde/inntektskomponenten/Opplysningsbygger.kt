package no.nav.etterlatte.opplysninger.kilde.inntektskomponenten

import no.nav.etterlatte.OpplysningsBygger
import no.nav.etterlatte.libs.common.behandling.Behandlingsopplysning
import no.nav.etterlatte.libs.common.behandling.opplysningstyper.Opplysningstyper
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.soeknad.dataklasser.Barnepensjon
import java.time.Instant
import java.util.*

class OpplysningsByggerService : OpplysningsBygger {

    override fun byggOpplysninger(
        barnepensjon: Barnepensjon,
        inntektsKomponentenResponse: InntektsKomponentenResponse
    ): List<Behandlingsopplysning<out Any>> {

        // TODO: sjekk om det finnes inntekt for uføretrygd eller alderspensjon i løpet av de siste fem år
        val uforetrygd = harFaattUforetrygd(inntektsKomponentenResponse.arbeidsInntektMaaned)
        val alderspensjon = harFaatAlderspensjon(inntektsKomponentenResponse.arbeidsInntektMaaned)

        return listOf(lagOpplysning(Opplysningstyper.PENSJON_UFORE_V1, PensjonUforeOpplysning(uforetrygd, alderspensjon)))
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

fun <T> lagOpplysning(opplysningsType: Opplysningstyper, opplysning: T): Behandlingsopplysning<T> {
    return Behandlingsopplysning(
        UUID.randomUUID(),
        Behandlingsopplysning.Pdl("pdl", Instant.now(), null),
        opplysningsType,
        objectMapper.createObjectNode(),
        opplysning
    )
}

data class PensjonUforeOpplysning(val mottattUforetrygd: Boolean, val mottattAlderspensjon: Boolean)