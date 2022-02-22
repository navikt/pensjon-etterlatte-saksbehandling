package person

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.features.json.JacksonSerializer
import io.ktor.client.features.json.JsonFeature
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.person.pdl.ParallelleSannheterKlient
import no.nav.etterlatte.person.pdl.PdlAdressebeskyttelse
import no.nav.etterlatte.person.pdl.PersonResponse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

internal class ParallelleSannheterTest {

    private val parallelleSannheterKlient = ParallelleSannheterKlient(
        httpClient = HttpClient(CIO) {
            install(JsonFeature) { serializer = JacksonSerializer(objectMapper) }
        },
        apiUrl = "https://pensjon-parallelle-sannheter.dev.intern.nav.no"
    )

    // TODO flere tester

    @Test
    fun `test navn`() {
        val json = javaClass.getResource("/pdl/personToNavn.json")!!.readText()
        val personResponse = objectMapper.readValue(json, PersonResponse::class.java)

        val avklartNavn = runBlocking {
            parallelleSannheterKlient.avklarNavn(personResponse.data?.hentPerson!!)
        }

        assertNotNull(avklartNavn)

    }

    @Test
    fun `test adressebeskyttelse`() {
        val json = javaClass.getResource("/pdl/personToNavn.json")!!.readText()
        val personResponse = objectMapper.readValue(json, PersonResponse::class.java)

        val avklartAdressebeskyttelse: PdlAdressebeskyttelse? = runBlocking {
            parallelleSannheterKlient.avklarAdressebeskyttelse(personResponse.data?.hentPerson!!)
        }

        assertNull(avklartAdressebeskyttelse)

    }

}
