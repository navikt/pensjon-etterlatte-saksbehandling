package no.nav.etterlatte.prosess

import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.etterlatte.EtterlatteFordeler
import no.nav.etterlatte.common.mapJsonToAny
import no.nav.etterlatte.libs.common.pdl.Gradering
import no.nav.etterlatte.libs.common.person.*
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
    private val verge = javaClass.getResource("/verge.json")!!.readText()
    private val huketAvForUtlandJson = javaClass.getResource("/huketAvForUtland.json")!!.readText()
    private val gyldigadresse = mapJsonToAny<eyAdresse>(javaClass.getResource("/gyldigAdresseResponse.json")!!.readText(), false)
    private val familieRelasjon = mapJsonToAny<EyFamilieRelasjon>(javaClass.getResource("/familieRelasjon.json")!!.readText(), false)
    private val familieRelasjonIkkeAnsvarlig = mapJsonToAny<EyFamilieRelasjon>(javaClass.getResource("/familieRelasjonIkkeAnsvarlig.json")!!.readText(), false)


    @AfterEach
    fun afterEach() {
        clearAllMocks()
    }

    //TODO flere tester
    //TODO skrive tester for Adresse

    @Test
    fun testFeltMapping() {
        coEvery { klientMock.hentPerson(any()) } returns barn
        coEvery { klientMock.hentPerson(Foedselsnummer.of("13087307551")) } returns avdoed
        coEvery { klientMock.hentUtland(any()) } returns ikkeUtland
        coEvery { klientMock.hentAdresse(any(), false) } returns gyldigadresse
        coEvery { klientMock.hentFamilieRelasjon(any()) } returns familieRelasjon


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
        coEvery { klientMock.hentAdresse(any(), false) } returns gyldigadresse
        coEvery { klientMock.hentFamilieRelasjon(any()) } returns familieRelasjon


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
        coEvery { klientMock.hentAdresse(any(), false) } returns gyldigadresse
        coEvery { klientMock.hentFamilieRelasjon(any()) } returns familieRelasjon



        val inspector = TestRapid()
            .apply { EtterlatteFordeler(this, service) }
            .apply { sendTestMessage(hendelseJson) }
            .inspektør
        assertEquals("ey_fordelt", inspector.message(0).get("@event_name").asText())

    }
    @Test
    fun harVerge() {
        coEvery { klientMock.hentPerson(any()) } returns barn
        coEvery { klientMock.hentUtland(any()) } returns ikkeUtland
        coEvery { klientMock.hentPerson(Foedselsnummer.of("13087307551"))} returns avdoed
        val inspector = TestRapid()
            .apply { EtterlatteFordeler(this, service) }
            .apply { sendTestMessage(verge) }
            .inspektør
        assertTrue(inspector.size == 0)

    }
    @Test
    fun harHuketAvForUtenlandsoppholdForAvdoed() {
        coEvery { klientMock.hentPerson(any()) } returns barn
        coEvery { klientMock.hentUtland(any()) } returns ikkeUtland
        coEvery { klientMock.hentPerson(Foedselsnummer.of("13087307551"))} returns avdoed
        val inspector = TestRapid()
            .apply { EtterlatteFordeler(this, service) }
            .apply { sendTestMessage(huketAvForUtlandJson) }
            .inspektør
        assertTrue(inspector.size == 0)

    }
    @Test
    fun `gjenlevende har ikke foreldreansvar`() {
        coEvery { klientMock.hentPerson(any())} returns barn
        coEvery { klientMock.hentPerson(Foedselsnummer.of("13087307551"))} returns avdoed
        coEvery { klientMock.hentUtland(any()) } returns ikkeUtland
        coEvery { klientMock.hentAdresse(any(), false) } returns gyldigadresse
        coEvery { klientMock.hentFamilieRelasjon(any()) } returns familieRelasjonIkkeAnsvarlig

        val inspector = TestRapid()
            .apply { EtterlatteFordeler(this, service) }
            .apply { sendTestMessage(hendelseJson) }
            .inspektør
        assertTrue(inspector.size == 0)

    }


}
