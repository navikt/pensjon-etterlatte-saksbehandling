package no.nav.etterlatte.prosess

import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.etterlatte.EtterlatteFordeler
import no.nav.etterlatte.common.mapJsonToAny
import no.nav.etterlatte.libs.common.pdl.Gradering
import no.nav.etterlatte.libs.common.person.Foedselsnummer
import no.nav.etterlatte.libs.common.person.Person
import no.nav.etterlatte.libs.common.person.eyUtland
import no.nav.etterlatte.pdl.PdlKlient
import no.nav.etterlatte.pdl.PersonService
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class EtterlatteFordelerTest {

    private val klientMock = mockk<PdlKlient>()
    private val service = PersonService(klientMock)

    //TODO flytte ned til relevant test?
    private val hendelseJson = javaClass.getResource("/barnePensjon.json")!!.readText()
    private val hendelseIkkeBarnePensjonJson = javaClass.getResource("/ikkeBarnepensjon.json")!!.readText()
    private val hendelseIkkeGyldig = javaClass.getResource("/hendelseUgyldig.json")!!.readText()
    private val ugyldigFnr = javaClass.getResource("/ugyldigFnr.json")!!.readText()
    private val yrkesskade = javaClass.getResource("/yrkesskade.json")!!.readText()
    private val InnsenderIkkeGjenlevende = javaClass.getResource("/gjenlevende.json")!!.readText()
    private val barnGammel = mapJsonToAny<Person>(javaClass.getResource("/personGammel.json")!!.readText(), false)
    private val barn = mapJsonToAny<Person>(javaClass.getResource("/person.json")!!.readText(), false)
    private val avdoed = mapJsonToAny<Person>(javaClass.getResource("/persondoed.json")!!.readText(), false)
    private val utland = mapJsonToAny<eyUtland>(javaClass.getResource("/utland.json")!!.readText(), false)
    private val ikkeUtland = mapJsonToAny<eyUtland>(javaClass.getResource("/ikkeUtland.json")!!.readText(), false)


    @AfterEach
    fun afterEach() {
        clearAllMocks()
    }

    //TODO flere tester

    @Test
    fun testFeltMapping() {
        coEvery { klientMock.hentPerson(any()) } returns barn
        coEvery { klientMock.hentPerson(Foedselsnummer.of("13087307551")) } returns avdoed
        coEvery { klientMock.hentUtland(any()) } returns ikkeUtland

        val inspector = TestRapid()
            .apply { EtterlatteFordeler(this, service) }
            .apply { sendTestMessage(hendelseJson) }
            .inspektør

        assertEquals(Gradering.STRENGT_FORTROLIG_UTLAND.name, inspector.message(0).get("@adressebeskyttelse").asText())
        assertEquals("ey_fordelt", inspector.message(0).get("@event_name").asText())
    }
    @Test
    fun ikkeBarnepensjonSoeknad() {
        coEvery { klientMock.hentPerson(any()) } returns barn
        coEvery { klientMock.hentUtland(any()) } returns ikkeUtland
        val inspector = TestRapid()
            .apply { EtterlatteFordeler(this, service) }
            .apply { sendTestMessage(hendelseIkkeBarnePensjonJson) }
            .inspektør

        assertTrue(inspector.size == 0)

    }
    //TODO gjøre om på hvordan testen forholder seg til tid.
    @Test
    fun barnForGammel() {
        coEvery { klientMock.hentPerson(any()) } returns barnGammel
        coEvery { klientMock.hentUtland(any()) } returns ikkeUtland
        val inspector = TestRapid()
            .apply { EtterlatteFordeler(this, service) }
            .apply { sendTestMessage(hendelseJson) }
            .inspektør

        assertTrue(inspector.size == 0)

    }
    @Test
    fun hendelseIkkeGyldigLengre() {
        coEvery { klientMock.hentPerson(any()) } returns barn
        coEvery { klientMock.hentUtland(any()) } returns ikkeUtland
        val inspector = TestRapid()
            .apply { EtterlatteFordeler(this, service) }
            .apply { sendTestMessage(hendelseIkkeGyldig) }
            .inspektør

        assertTrue(inspector.size == 0)

    }
    @Test
    fun ugyldigFnrISoeknad() {
        coEvery { klientMock.hentPerson(any()) } returns barn
        coEvery { klientMock.hentUtland(any()) } returns ikkeUtland
        val inspector = TestRapid()
            .apply { EtterlatteFordeler(this, service) }
            .apply { sendTestMessage(ugyldigFnr) }
            .inspektør

        assertEquals("ugyldigFnr", inspector.message(0).get("@event_name").asText())

    }
    @Test
    fun avdoedHarYrkesskade() {
        coEvery { klientMock.hentPerson(any()) } returns barn
        coEvery { klientMock.hentUtland(any()) } returns ikkeUtland
        val inspector = TestRapid()
            .apply { EtterlatteFordeler(this, service) }
            .apply { sendTestMessage(yrkesskade) }
            .inspektør

        assertTrue(inspector.size == 0)

    }
    @Test
    fun innsenderErIkkeGjennlevende() {
        coEvery { klientMock.hentPerson(any()) } returns barn
        coEvery { klientMock.hentUtland(any()) } returns ikkeUtland
        val inspector = TestRapid()
            .apply { EtterlatteFordeler(this, service) }
            .apply { sendTestMessage(InnsenderIkkeGjenlevende) }
            .inspektør

        assertTrue(inspector.size == 0)

    }
    @Test
    fun HarUtlandsopphold() {
        coEvery { klientMock.hentPerson(any()) } returns barn
        coEvery { klientMock.hentUtland(any()) } returns utland
        val inspector = TestRapid()
            .apply { EtterlatteFordeler(this, service) }
            .apply { sendTestMessage(hendelseJson) }
            .inspektør

        assertTrue(inspector.size == 0)

    }
    @Test
    fun HarIkkeUtlandsopphold() {
        coEvery { klientMock.hentPerson(any()) } returns barn
        coEvery { klientMock.hentPerson(Foedselsnummer.of("13087307551"))} returns avdoed
        coEvery { klientMock.hentUtland(any()) } returns ikkeUtland
        val inspector = TestRapid()
            .apply { EtterlatteFordeler(this, service) }
            .apply { sendTestMessage(hendelseJson) }
            .inspektør

        assertEquals("ey_fordelt", inspector.message(0).get("@event_name").asText())
        assertEquals("true", inspector.message(0).get("@soeknad_fordelt").asText())
    }

    @Test
    fun AvdoedErIkkeDoed() {
        coEvery { klientMock.hentPerson(any())} returns barn
        coEvery { klientMock.hentUtland(any()) } returns ikkeUtland

        val inspector = TestRapid()
            .apply { EtterlatteFordeler(this, service) }
            .apply { sendTestMessage(hendelseJson) }
            .inspektør

        assertTrue(inspector.size == 0)
    }

    @Test
    fun AvdoedErDoed() {
        coEvery { klientMock.hentPerson(any())} returns barn
        coEvery { klientMock.hentPerson(Foedselsnummer.of("13087307551"))} returns avdoed
        coEvery { klientMock.hentUtland(any()) } returns ikkeUtland

        val inspector = TestRapid()
            .apply { EtterlatteFordeler(this, service) }
            .apply { sendTestMessage(hendelseJson) }
            .inspektør
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