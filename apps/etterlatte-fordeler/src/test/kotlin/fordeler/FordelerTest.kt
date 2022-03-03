package no.nav.etterlatte.fordeler

import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.etterlatte.libs.common.person.FamilieRelasjon
import no.nav.etterlatte.libs.common.person.Foedselsnummer
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

    private val soeknadBarnepensjon = javaClass.getResource("/fordeler/soknad_barnepensjon.json")!!.readText()
    private val soeknadIkkeBarnepensjon = javaClass.getResource("/fordeler/soknad_ikke_barnepensjon.json")!!.readText()
    private val hendelseIkkeGyldig = javaClass.getResource("/fordeler/soknad_utgaatt_hendelse.json")!!.readText()

    @AfterEach
    fun afterEach() {
        clearAllMocks()
    }

    @Test
    fun `skal fordele gyldig soknad til vedtakslosning`() {
        val barnFnr = Foedselsnummer.of("07010776133")
        val avdoedFnr = Foedselsnummer.of("24014021406")
        val etterlattFnr = Foedselsnummer.of("11057523044")

        coEvery { pdlTjenesterKlient.hentPerson(match { it.foedselsnummer == barnFnr } ) } returns mockPerson(
            bostedsadresse = mockNorskAdresse(),
            familieRelasjon = FamilieRelasjon(
                ansvarligeForeldre = listOf(etterlattFnr),
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
                ansvarligeForeldre = listOf(Foedselsnummer.of("11057523044")),
                foreldre = null,
                barn = listOf(Foedselsnummer.of("07010776133")),
            )
        )

        val inspector = TestRapid()
            .apply { Fordeler(this, pdlTjenesterKlient, FordelerKriterierService()) }
            .apply { sendTestMessage(soeknadBarnepensjon) }
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
                ansvarligeForeldre = listOf(etterlattFnr),
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
                ansvarligeForeldre = listOf(Foedselsnummer.of("11057523044")),
                foreldre = null,
                barn = listOf(Foedselsnummer.of("07010776133")),
            )
        )

        val inspector = TestRapid()
            .apply { Fordeler(this, pdlTjenesterKlient, FordelerKriterierService()) }
            .apply { sendTestMessage(soeknadBarnepensjon) }
            .inspektør

        assertTrue(inspector.size == 0)
    }

    @Test
    fun `skal ikke fordele hendelse som ikke lenger er gyldig`() {
        val inspector = TestRapid()
            .apply { Fordeler(this, pdlTjenesterKlient, FordelerKriterierService()) }
            .apply { sendTestMessage(hendelseIkkeGyldig) }
            .inspektør

        assertTrue(inspector.size == 0)
    }

    @Test
    fun `skal ikke fordele soknad som ikke er av typen barnepensjon`() {
        val inspector = TestRapid()
            .apply { Fordeler(this, pdlTjenesterKlient, FordelerKriterierService()) }
            .apply { sendTestMessage(soeknadIkkeBarnepensjon) }
            .inspektør

        assertTrue(inspector.size == 0)
    }

    @Test
    fun `skal feile og logge dersom kall mot pdltjenester feiler`() {
        coEvery { pdlTjenesterKlient.hentPerson(any()) } throws RuntimeException("Noe feilet")

        val inspector = TestRapid()
            .apply { Fordeler(this, pdlTjenesterKlient, FordelerKriterierService()) }
            .apply { sendTestMessage(soeknadBarnepensjon) }
            .inspektør

        assertTrue(inspector.size == 0)
    }

}
