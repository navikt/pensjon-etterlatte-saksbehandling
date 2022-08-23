import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstyper
import no.nav.etterlatte.libs.common.inntekt.PensjonUforeOpplysning
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
        assertEquals(opplysninger[0].opplysningType, Opplysningstyper.PENSJON_UFORE_V1)
        val pensjonUforeOpplysning = (opplysninger[0].opplysning) as PensjonUforeOpplysning
        assertEquals(pensjonUforeOpplysning.mottattUforetrygd.size, 25)
        assertEquals(pensjonUforeOpplysning.mottattAlderspensjon.size, 35)
    }

    private fun readFile(file: String): String {
        return OpplysningsbyggerTest::class.java.getResource(file)?.readText() ?: throw FileNotFoundException(
            "Fant ikke filen $file"
        )
    }
}