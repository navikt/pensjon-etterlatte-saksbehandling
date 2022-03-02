package no.nav.etterlatte.fordeler

import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.etterlatte.libs.common.person.Barn
import no.nav.etterlatte.libs.common.person.FamilieRelasjon
import no.nav.etterlatte.libs.common.person.Foedselsnummer
import no.nav.etterlatte.libs.common.person.ForeldreAnsvar
import no.nav.etterlatte.mockNorskAdresse
import no.nav.etterlatte.mockPerson
import no.nav.etterlatte.pdltjenester.PdlTjenesterKlient
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class FordelerTest {

    private val pdlTjenesterKlient = mockk<PdlTjenesterKlient>()

    private val nyhendelseJson = javaClass.getResource("/fordeler/soknad_barnepensjon.json")!!.readText()
    private val hendelseIkkeBarnePensjonJson = javaClass.getResource("/fordeler/soknad_ikke_barnepensjon.json")!!.readText()
    private val hendelseIkkeGyldig = javaClass.getResource("/fordeler/soknad_utgaatt_hendelse.json")!!.readText()

    @AfterEach
    fun afterEach() {
        clearAllMocks()
    }

    @Test
    fun `gyldig soknad til vedtakslosning`() {
        val barnFnr = Foedselsnummer.of("07010776133")
        val avdoedFnr = Foedselsnummer.of("24014021406")
        val etterlattFnr = Foedselsnummer.of("11057523044")

        coEvery { pdlTjenesterKlient.hentPerson(match { it.foedselsnummer == barnFnr } ) } returns mockPerson(
            bostedsadresse = mockNorskAdresse(),
            familieRelasjon = FamilieRelasjon(
                ansvarligeForeldre = listOf(ForeldreAnsvar(etterlattFnr)),
                foreldre = null,
                barn = null,
            )
        )

        coEvery { pdlTjenesterKlient.hentPerson(match { it.foedselsnummer == avdoedFnr }) } returns mockPerson(
            doedsdato = LocalDate.parse("2022-01-01"),
            bostedsadresse = mockNorskAdresse()
        )

        coEvery { pdlTjenesterKlient.hentPerson(match { it.foedselsnummer == etterlattFnr }) } returns mockPerson(
            bostedsadresse = mockNorskAdresse(),
            familieRelasjon = FamilieRelasjon(
                ansvarligeForeldre = listOf(ForeldreAnsvar(Foedselsnummer.of("11057523044"))),
                foreldre = null,
                barn = listOf(Barn(Foedselsnummer.of("07010776133"))),
            )
        )

        val inspector = TestRapid()
            .apply { Fordeler(this, pdlTjenesterKlient, FordelerKriterierService()) }
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

        coEvery { pdlTjenesterKlient.hentPerson(match { it.foedselsnummer == barnFnr }) } returns mockPerson(
            bostedsadresse = mockNorskAdresse(),
            familieRelasjon = FamilieRelasjon(
                ansvarligeForeldre = listOf(ForeldreAnsvar(etterlattFnr)),
                foreldre = null,
                barn = null,
            )
        )

        coEvery { pdlTjenesterKlient.hentPerson(match { it.foedselsnummer == avdoedFnr }) } returns mockPerson(
            bostedsadresse = mockNorskAdresse()
        )

        coEvery { pdlTjenesterKlient.hentPerson(match { it.foedselsnummer == etterlattFnr }) } returns mockPerson(
            bostedsadresse = mockNorskAdresse(),
            familieRelasjon = FamilieRelasjon(
                ansvarligeForeldre = listOf(ForeldreAnsvar(Foedselsnummer.of("11057523044"))),
                foreldre = null,
                barn = listOf(Barn(Foedselsnummer.of("07010776133"))),
            )
        )

        val inspector = TestRapid()
            .apply { Fordeler(this, pdlTjenesterKlient, FordelerKriterierService()) }
            .apply { sendTestMessage(nyhendelseJson) }
            .inspektør

        assertTrue(inspector.size == 0)
    }

    @Test
    fun hendelseIkkeGyldigLengre() {
        val inspector = TestRapid()
            .apply { Fordeler(this, pdlTjenesterKlient, FordelerKriterierService()) }
            .apply { sendTestMessage(hendelseIkkeGyldig) }
            .inspektør

        assertTrue(inspector.size == 0)
    }

    @Test
    fun ikkeBarnepensjonSoeknad() {
        val inspector = TestRapid()
            .apply { Fordeler(this, pdlTjenesterKlient, FordelerKriterierService()) }
            .apply { sendTestMessage(hendelseIkkeBarnePensjonJson) }
            .inspektør

        assertTrue(inspector.size == 0)
    }

    @Test
    fun `skal feile og logge dersom kall mot pdltjenester feiler`() {
        coEvery { pdlTjenesterKlient.hentPerson(any()) } throws RuntimeException("Noe feilet")

        val inspector = TestRapid()
            .apply { Fordeler(this, pdlTjenesterKlient, FordelerKriterierService()) }
            .apply { sendTestMessage(nyhendelseJson) }
            .inspektør

        assertTrue(inspector.size == 0)
    }

}
