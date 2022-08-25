import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.etterlatte.libs.common.arbeidsforhold.AaregResponse
import no.nav.etterlatte.libs.common.arbeidsforhold.ArbeidsforholdOpplysning
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstyper
import no.nav.etterlatte.libs.common.inntekt.Ident
import no.nav.etterlatte.libs.common.inntekt.InntektsOpplysning
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.opplysninger.kilde.inntektskomponenten.InntektsKomponentenResponse
import no.nav.etterlatte.opplysninger.kilde.inntektskomponenten.OpplysningsByggerService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.FileNotFoundException

class OpplysningsbyggerTest {

    @Test
    fun `skal hente alle ufoeretrygd og alderspensjon inntekter`() {
        val service = OpplysningsByggerService()
        val inntektsKomponentenResponse = objectMapper.readValue<InntektsKomponentenResponse>(
            readFile("inntektskomponenten_eksempelrespons.json")
        )

        val opplysninger = service.byggOpplysninger(inntektsKomponentenResponse, emptyList())

        assertEquals(opplysninger.size, 1)
        assertEquals(opplysninger[0].opplysningType, Opplysningstyper.AVDOED_INNTEKT_V1)
        val inntektsOpplysning = (opplysninger[0].opplysning) as InntektsOpplysning
        assertEquals(inntektsOpplysning.pensjonEllerTrygd.size, 177)
        assertEquals(inntektsOpplysning.ytelseFraOffentlig.size, 27)
        assertEquals(inntektsOpplysning.loennsinntekt.size, 48)
        assertEquals(inntektsOpplysning.naeringsinntekt.size, 0)
    }

    @Test
    fun `skal hente alle arbeidsforhold og mappe til opplysning`() {
        val service = OpplysningsByggerService()
        val aaregResponse = objectMapper.readValue<List<AaregResponse>>(
            readFile("aareg_eksempelresponse.json")
        )

        val opplysninger = service.byggOpplysninger(InntektsKomponentenResponse(null, Ident("", "NA")), aaregResponse)

        assertEquals(opplysninger.size, 1)
        assertEquals(opplysninger[0].opplysningType, Opplysningstyper.ARBEIDSFORHOLD_V1)
        val arbeidsforholdOpplysning = (opplysninger[0].opplysning) as ArbeidsforholdOpplysning
        assertEquals(arbeidsforholdOpplysning.arbeidsforhold.size, 1)
        assertEquals(arbeidsforholdOpplysning.arbeidsforhold[0].type.beskrivelse, "Ordinært arbeidsforhold")
    }

    private fun readFile(file: String): String {
        return OpplysningsbyggerTest::class.java.getResource(file)?.readText() ?: throw FileNotFoundException(
            "Fant ikke filen $file"
        )
    }
}