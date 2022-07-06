import com.fasterxml.jackson.databind.SerializationFeature
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.opplysninger.kilde.inntektskomponenten.InntektsKomponentenResponse
import no.nav.etterlatte.opplysninger.kilde.inntektskomponenten.OpplysningsByggerService
import org.junit.jupiter.api.Test
import java.io.FileNotFoundException

class OpplysningsbyggerTest {

    @Test
    fun `skal teste opplysninger`(){
        val service = OpplysningsByggerService()

        val inntektsKomponentenResponse = objectMapper.readValue(readFile("eksempelrespons.json"), InntektsKomponentenResponse::class.java)
        val opplysninger = service.byggOpplysninger(inntektsKomponentenResponse, emptyList())

        val result = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(opplysninger)
        print(result)
        assert(true)
    }

    fun readFile(file: String): String {
        return OpplysningsbyggerTest::class.java.getResource(file)?.readText()?: throw FileNotFoundException("Fant ikke filen $file")
    }

}