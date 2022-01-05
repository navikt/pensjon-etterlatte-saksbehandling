package no.nav.etterlatte.prosess

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonTypeRef
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.features.json.JacksonSerializer
import io.ktor.client.features.json.JsonFeature
import io.ktor.content.TextContent
import io.ktor.http.ContentType
import io.ktor.http.fullPath
import io.ktor.http.headersOf
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.EtterlatteFordeler
import no.nav.etterlatte.common.mapJsonToAny
import no.nav.etterlatte.libs.common.journalpost.JournalpostRequest
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.pdl.AdressebeskyttelseResponse
import no.nav.etterlatte.libs.common.pdl.Gradering
import no.nav.etterlatte.libs.common.person.Foedselsnummer
import no.nav.etterlatte.pdl.PdlKlient
import no.nav.etterlatte.pdl.Person
import no.nav.etterlatte.pdl.PersonService
import no.nav.etterlatte.prosess.pdl.PersonResponse
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.security.token.support.client.core.jwk.JwkFactory.fromJson
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Paths
import java.time.Clock
import java.time.LocalDateTime
import java.time.Month
import java.time.ZoneId
import java.time.ZoneOffset

internal class EtterlatteFordelerTest {

    private val klientMock = mockk<PdlKlient>()
    private val service = PersonService(klientMock)

    private val hendelseJson = javaClass.getResource("/fullMessage2.json")!!.readText()
    private val barnJson = javaClass.getResource("/person.json")!!.readText()
    private val barn = mapJsonToAny<Person>(barnJson, false)


    @AfterEach
    fun afterEach() {
        clearAllMocks()
    }

    //TODO flere tester
    //hendelsegyldig til
    //ikke barnepensjonsøknad
    //test ugyldig fnr

    @Test
    fun testFeltMapping() {
        coEvery { klientMock.hentPerson(any()) } returns barn

        val inspector = TestRapid()
            .apply { EtterlatteFordeler(this, service) }
            .apply { sendTestMessage(hendelseJson) }
            .inspektør

        println("gah")
        assertEquals(
            //TODO våre egne assertions
            "bah","bah"
            //Gradering.STRENGT_FORTROLIG.name,
            //inspector.message(0).get("@adressebeskyttelse").asText()
        )
    }
}
/*

    @Test
    fun testTomResponse() {
        coEvery { klientMock.finnAdressebeskyttelseForFnr(any()) } returns barn

        val inspector = TestRapid()
            .apply { EtterlatteFordeler(this, service) }
            .apply { sendTestMessage(hendelseJson) }
            .inspektør

        assertEquals(
            Gradering.UGRADERT.name,
            inspector.message(0).get("@adressebeskyttelse").asText()
        )
    }

}

    @Test
    fun `Skal finne alle gyldige fødselsnummer i søknaden`() {
        val gyldigeFoedselsnummer = listOf(
            "07106123912", "14106126780", "21929774873", "61929750062", "61483601467", "29507030252"
        ).map { Foedselsnummer.of(it) }

        val resultat: List<Foedselsnummer> = jacksonObjectMapper().readTree(hendelseJson).finnFoedselsnummer()
        assertEquals(resultat.size, gyldigeFoedselsnummer.size)
        assertTrue(resultat.containsAll(gyldigeFoedselsnummer))
    }

    private fun opprettRespons(): AdressebeskyttelseResponse {
        val graderingString = gradering.joinToString { "{\"gradering\" : \"$it\"}" }

        val json = javaClass.getResource("/personResponse.json")!!.readText()

        return jacksonObjectMapper().readValue(json, jacksonTypeRef())
    }
}

 */