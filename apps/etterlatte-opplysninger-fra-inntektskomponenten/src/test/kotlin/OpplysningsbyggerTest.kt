import com.fasterxml.jackson.module.kotlin.readValue
import grunnlag.AVDOED_FOEDSELSNUMMER
import no.nav.etterlatte.libs.common.arbeidsforhold.AaregResponse
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstyper
import no.nav.etterlatte.libs.common.inntekt.Ident
import no.nav.etterlatte.libs.common.inntekt.InntektsOpplysning
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.person.Foedselsnummer
import no.nav.etterlatte.opplysninger.kilde.inntektskomponenten.InntektsKomponentenResponse
import no.nav.etterlatte.opplysninger.kilde.inntektskomponenten.OpplysningsByggerService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.FileNotFoundException
import java.time.YearMonth

class OpplysningsbyggerTest {

    @Test
    fun `skal hente alle ufoeretrygd og alderspensjon inntekter`() {
        val service = OpplysningsByggerService()
        val inntektsKomponentenResponse = objectMapper.readValue<InntektsKomponentenResponse>(
            readFile("inntektskomponenten_eksempelrespons.json")
        )

        val opplysninger = service.byggOpplysninger(inntektsKomponentenResponse, emptyList(), AVDOED_FOEDSELSNUMMER)

        assertEquals(opplysninger.size, 2)
        assertEquals(opplysninger[0].opplysningType, Opplysningstyper.INNTEKT)
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

        val opplysninger = service.byggOpplysninger(
            InntektsKomponentenResponse(null, Ident("", "NA")),
            aaregResponse,
            AVDOED_FOEDSELSNUMMER
        )

        assertEquals(opplysninger.size, 2)
        assertEquals(opplysninger[1].opplysningType, Opplysningstyper.ARBEIDSFORHOLD_V1)
        val arbeidsforholdOpplysning = (opplysninger[1].opplysning) as AaregResponse
        assertEquals(arbeidsforholdOpplysning.type.beskrivelse, "Ordinært arbeidsforhold")
    }

    @Test
    fun `hvis bruker ikke har noe arbeidsforhold så lager vi en tom periodisert opplysning`() {
        val service = OpplysningsByggerService()
        val aaregResponse = emptyList<AaregResponse>()
        val avdødFnr = Foedselsnummer.of("06087748063")

        val opplysninger = service.byggOpplysninger(
            InntektsKomponentenResponse(null, Ident("", "NA")),
            aaregResponse,
            avdødFnr
        )

        val arbeidsforholdOpplysning = opplysninger[1]

        assertEquals(null, arbeidsforholdOpplysning.opplysning)
        assertEquals("06087748063", arbeidsforholdOpplysning.fnr!!.value)
        assertEquals(YearMonth.of(1977, 8), arbeidsforholdOpplysning.periode!!.fom)
        assertEquals(null, arbeidsforholdOpplysning.periode?.tom)
    }

    private fun readFile(file: String): String {
        return OpplysningsbyggerTest::class.java.getResource(file)?.readText() ?: throw FileNotFoundException(
            "Fant ikke filen $file"
        )
    }
}