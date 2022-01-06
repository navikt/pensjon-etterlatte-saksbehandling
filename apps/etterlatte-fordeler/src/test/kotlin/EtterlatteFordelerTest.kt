package no.nav.etterlatte.prosess

import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.etterlatte.EtterlatteFordeler
import no.nav.etterlatte.common.mapJsonToAny
import no.nav.etterlatte.libs.common.pdl.Gradering
import no.nav.etterlatte.libs.common.person.Person
import no.nav.etterlatte.pdl.PdlKlient
import no.nav.etterlatte.pdl.PersonService
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

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

        assertEquals(Gradering.STRENGT_FORTROLIG_UTLAND.name, inspector.message(0).get("@adressebeskyttelse").asText())
        assertEquals("ey_fordelt", inspector.message(0).get("@event_name").asText())
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