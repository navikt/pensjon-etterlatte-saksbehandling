package no.nav.etterlatte.prosess

import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.etterlatte.EtterlatteFordeler
import no.nav.etterlatte.FordelerKriterierService
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
    private val personService = PersonService(klientMock)

    private val nyhendelseJson = javaClass.getResource("/NyBarnePensjon.json")!!.readText()
    private val hendelseIkkeBarnePensjonJson = javaClass.getResource("/ikkeBarnepensjon.json")!!.readText()
    private val hendelseIkkeGyldig = javaClass.getResource("/hendelseUgyldig.json")!!.readText()

    @AfterEach
    fun afterEach() {
        clearAllMocks()
    }

    @Test
    fun `gyldig soknad til vedtakslosning`() {
        val barnFnr = Foedselsnummer.of("07010776133")
        val avdoedFnr = Foedselsnummer.of("24014021406")
        val etterlattFnr = Foedselsnummer.of("11057523044")

        coEvery { klientMock.hentPerson(match { it.foedselsnummer == barnFnr } ) } returns mockPerson(
            adresse = mockNorskAdresse(),
            familieRelasjon = FamilieRelasjon(
                ansvarligeForeldre = listOf(ForeldreAnsvar(etterlattFnr)),
                foreldre = null,
                barn = null,
            )
        )

        coEvery { klientMock.hentPerson(match { it.foedselsnummer == avdoedFnr }) } returns mockPerson(
            doedsdato = "2022-01-01",
            adresse = mockNorskAdresse()
        )

        coEvery { klientMock.hentPerson(match { it.foedselsnummer == etterlattFnr }) } returns mockPerson(
            adresse = mockNorskAdresse(),
            familieRelasjon = FamilieRelasjon(
                ansvarligeForeldre = listOf(ForeldreAnsvar(Foedselsnummer.of("11057523044"))),
                foreldre = null,
                barn = listOf(Barn(Foedselsnummer.of("07010776133"))),
            )
        )

        val inspector = TestRapid()
            .apply { EtterlatteFordeler(this, personService, FordelerKriterierService()) }
            .apply { sendTestMessage(nyhendelseJson) }
            .inspektør

        assertEquals("ey_fordelt", inspector.message(0).get("@event_name").asText())
        assertEquals("true", inspector.message(0).get("@soeknad_fordelt").asText())
    }

    @Test
    fun `soknad med avdoed som ikke er registrert som doed skal ikke fordeles`() {
        val barnFnr = Foedselsnummer.of("07010776133")
        val avdoedFnr = Foedselsnummer.of("24014021406")
        val etterlattFnr = Foedselsnummer.of("11057523044")

        coEvery { klientMock.hentPerson(match { it.foedselsnummer == barnFnr }) } returns mockPerson(
            adresse = mockNorskAdresse(),
            familieRelasjon = FamilieRelasjon(
                ansvarligeForeldre = listOf(ForeldreAnsvar(etterlattFnr)),
                foreldre = null,
                barn = null,
            )
        )

        coEvery { klientMock.hentPerson(match { it.foedselsnummer == avdoedFnr }) } returns mockPerson(
            adresse = mockNorskAdresse()
        )

        coEvery { klientMock.hentPerson(match { it.foedselsnummer == etterlattFnr }) } returns mockPerson(
            adresse = mockNorskAdresse(),
            familieRelasjon = FamilieRelasjon(
                ansvarligeForeldre = listOf(ForeldreAnsvar(Foedselsnummer.of("11057523044"))),
                foreldre = null,
                barn = listOf(Barn(Foedselsnummer.of("07010776133"))),
            )
        )

        val inspector = TestRapid()
            .apply { EtterlatteFordeler(this, personService, FordelerKriterierService()) }
            .apply { sendTestMessage(nyhendelseJson) }
            .inspektør

        assertTrue(inspector.size == 0)
    }

    @Test
    fun hendelseIkkeGyldigLengre() {
        val inspector = TestRapid()
            .apply { EtterlatteFordeler(this, personService, FordelerKriterierService()) }
            .apply { sendTestMessage(hendelseIkkeGyldig) }
            .inspektør

        assertTrue(inspector.size == 0)
    }

    @Test
    fun ikkeBarnepensjonSoeknad() {
        val inspector = TestRapid()
            .apply { EtterlatteFordeler(this, personService, FordelerKriterierService()) }
            .apply { sendTestMessage(hendelseIkkeBarnePensjonJson) }
            .inspektør

        assertTrue(inspector.size == 0)
    }

    @Test
    fun `skal feile og logge dersom kall mot pdltjenester feiler`() {
        coEvery { klientMock.hentPerson(any()) } throws RuntimeException("Noe feilet")

        val inspector = TestRapid()
            .apply { EtterlatteFordeler(this, personService, FordelerKriterierService()) }
            .apply { sendTestMessage(nyhendelseJson) }
            .inspektør

        assertTrue(inspector.size == 0)
    }

}
