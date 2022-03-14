package no.nav.etterlatte.person

import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.STOR_SNERK
import no.nav.etterlatte.TRIVIELL_MIDTPUNKT
import no.nav.etterlatte.libs.common.person.HentPersonRequest
import no.nav.etterlatte.libs.common.person.PersonRolle
import no.nav.etterlatte.mockResponse
import no.nav.etterlatte.pdl.ParallelleSannheterKlient
import no.nav.etterlatte.pdl.PdlHentPerson
import no.nav.etterlatte.pdl.PdlKlient
import no.nav.etterlatte.pdl.PdlPersonResponse
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class PersonServiceTest {

    private val pdlKlient = mockk<PdlKlient>()
    private val ppsKlient = mockk<ParallelleSannheterKlient>()
    private val personService = PersonService(pdlKlient, ppsKlient )

    @BeforeEach
    fun beforeEach() {
        val personResponse: PdlPersonResponse = mockResponse("/pdl/person.json")
        val hentPerson: PdlHentPerson = personResponse.data?.hentPerson!!

        coEvery { pdlKlient.hentPerson(any(), any()) } returns personResponse
        coEvery { ppsKlient.avklarNavn(any()) } returns hentPerson.navn.first()
        coEvery { ppsKlient.avklarAdressebeskyttelse(any()) } returns null
        coEvery { ppsKlient.avklarStatsborgerskap(any()) } returns hentPerson.statsborgerskap?.first()
        coEvery { ppsKlient.avklarSivilstand(any()) } returns null
        coEvery { ppsKlient.avklarFoedsel(any()) } returns hentPerson.foedsel.first()
        coEvery { ppsKlient.avklarDoedsfall(any()) } returns null
        coEvery { ppsKlient.avklarBostedsadresse(any()) } returns hentPerson.bostedsadresse?.first()
        coEvery { ppsKlient.avklarKontaktadresse(any()) } returns hentPerson.kontaktadresse?.first()
        coEvery { ppsKlient.avklarOppholdsadresse(any()) } returns hentPerson.oppholdsadresse?.first()
    }

    @AfterEach
    fun afterEach() {
        clearAllMocks()
    }

    @Test
    fun`skal mappe person som inkluderer familierelasjon (foreldre)`() {
        val person = runBlocking {
            personService.hentPerson(HentPersonRequest(TRIVIELL_MIDTPUNKT, rolle = PersonRolle.BARN))
        }

        val expectedForeldreFnr = listOf("26117512737", "14097030880")

        assertNotNull(person.familieRelasjon?.foreldre)
        assertEquals(2, person.familieRelasjon?.foreldre?.size)
        assertTrue(person.familieRelasjon?.foreldre?.get(0)?.value in expectedForeldreFnr)
        assertTrue(person.familieRelasjon?.foreldre?.get(1)?.value in expectedForeldreFnr)
    }

    @Test
    fun`skal mappe person som inkluderer familierelasjon (ansvarlige foreldre)`() {
        val person = runBlocking {
            personService.hentPerson(HentPersonRequest(TRIVIELL_MIDTPUNKT, rolle = PersonRolle.BARN))
        }

        val expectedForeldreFnr = listOf("26117512737", "14097030880")

        assertNotNull(person.familieRelasjon?.ansvarligeForeldre)
        assertEquals(2, person.familieRelasjon?.ansvarligeForeldre?.size)
        assertTrue(person.familieRelasjon?.foreldre?.get(0)?.value in expectedForeldreFnr)
        assertTrue(person.familieRelasjon?.foreldre?.get(1)?.value in expectedForeldreFnr)
    }

    @Test
    @Disabled("TODO - få inn datagrunnlag for denne")
    fun`skal mappe person som inkluderer familierelasjon (barn)`() {
        val person = runBlocking {
            personService.hentPerson(HentPersonRequest(TRIVIELL_MIDTPUNKT, rolle = PersonRolle.BARN))
        }

        val foreldreFnr = listOf("26117512737", "14097030880")

        assertNotNull(person.familieRelasjon?.barn)
        assertEquals(2, person.familieRelasjon?.barn?.size)
        assertTrue(person.familieRelasjon?.barn?.get(0)?.value in foreldreFnr)
        assertTrue(person.familieRelasjon?.barn?.get(1)?.value in foreldreFnr)
    }

    @Test
    fun `Hent utland med innflytting mappes korrekt`() {
        val person = runBlocking {
            personService.hentPerson(HentPersonRequest(TRIVIELL_MIDTPUNKT, rolle = PersonRolle.BARN))
        }

        assertEquals(2, person.utland?.innflyttingTilNorge?.size)
        assertEquals("NIC", person.utland?.innflyttingTilNorge?.get(0)?.fraflyttingsland)
        assertEquals("1970-09-14", person.utland?.innflyttingTilNorge?.get(0)?.dato.toString())
    }

    @Test
    fun `Hent utland med utflytting mappes korrekt`() {
        val person = runBlocking {
            personService.hentPerson(HentPersonRequest(TRIVIELL_MIDTPUNKT, rolle = PersonRolle.BARN))
        }

        assertEquals(1, person.utland?.utflyttingFraNorge?.size)
        assertEquals("FRA", person.utland?.utflyttingFraNorge?.get(0)?.tilflyttingsland)
        assertEquals("2021-07-01", person.utland?.utflyttingFraNorge?.get(0)?.dato.toString())
    }

    @Test
    fun `Person ikke finnes kaster exception`() {
        coEvery { pdlKlient.hentPerson(any(), any()) } returns PdlPersonResponse(data = null, errors = emptyList())

        assertThrows<PdlForesporselFeilet> {
            runBlocking {
                personService.hentPerson(HentPersonRequest(TRIVIELL_MIDTPUNKT, rolle = PersonRolle.BARN))
            }
        }
    }

    @Test
    fun `Finner ikke person i PDL`() {
        coEvery { pdlKlient.hentPerson(any(), any()) } returns mockResponse("/pdl/person_ikke_funnet.json")

        runBlocking {
            assertThrows<PdlForesporselFeilet> {
                personService.hentPerson(HentPersonRequest(STOR_SNERK, rolle = PersonRolle.BARN))
            }
        }
    }

}