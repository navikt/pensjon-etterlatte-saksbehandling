package no.nav.etterlatte.person

import com.fasterxml.jackson.module.kotlin.jacksonTypeRef
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.person.Foedselsnummer
import no.nav.etterlatte.libs.common.person.HentPersonRequest
import no.nav.etterlatte.libs.common.person.PersonRolle
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

    private companion object {
        private const val TRIVIELL_MIDTPUNKT = "19040550081"
        private const val STOR_SNERK = "11057523044"
    }

    private val pdlKlient = mockk<PdlKlient>()
    private val ppsKlient = mockk<ParallelleSannheterKlient>()
    private val personService = PersonService(pdlKlient, ppsKlient )

    @BeforeEach
    fun beforeEach() {
        val personResponse: PdlPersonResponse = opprettResponse("/pdl/personUtvidetResponse.json")
        val hentPerson: PdlHentPerson = personResponse.data?.hentPerson!!

        coEvery { pdlKlient.hentPerson(any()) } returns personResponse
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
            personService.hentPerson(HentPersonRequest(Foedselsnummer.of(TRIVIELL_MIDTPUNKT), rolle = PersonRolle.BARN))
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
            personService.hentPerson(HentPersonRequest(Foedselsnummer.of(TRIVIELL_MIDTPUNKT), rolle = PersonRolle.BARN))
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
            personService.hentPerson(HentPersonRequest(Foedselsnummer.of(TRIVIELL_MIDTPUNKT), rolle = PersonRolle.BARN))
        }

        val foreldreFnr = listOf("26117512737", "14097030880")

        assertNotNull(person.familieRelasjon?.barn)
        assertEquals(2, person.familieRelasjon?.barn?.size)
        assertTrue(person.familieRelasjon?.barn?.get(0)?.value in foreldreFnr)
        assertTrue(person.familieRelasjon?.barn?.get(1)?.value in foreldreFnr)
    }

    @Test
    fun `Person ikke finnes kaster exception`() {
        coEvery { pdlKlient.hentPerson(any()) } returns PdlPersonResponse(data = null, errors = emptyList())

        assertThrows<PdlForesporselFeilet> {
            runBlocking {
                personService.hentPerson(HentPersonRequest(Foedselsnummer.of(TRIVIELL_MIDTPUNKT), rolle = PersonRolle.BARN))
            }
        }
    }

    @Test
    fun `Finner ikke person i PDL`() {
        coEvery { pdlKlient.hentPerson(any()) } returns opprettResponse("/pdl/personResponseIkkeFunnet.json")

        runBlocking {
            assertThrows<PdlForesporselFeilet> {
                personService.hentPerson(HentPersonRequest(Foedselsnummer.of(STOR_SNERK), rolle = PersonRolle.BARN))
            }
        }
    }

    private inline fun <reified T> opprettResponse(fil: String): T {
        val json = javaClass.getResource(fil)!!.readText()

        return objectMapper.readValue(json, jacksonTypeRef())
    }

    /*@Test
    fun `Hent utland med innflytting mappes korrekt`() {
        coEvery { personKlient.hentUtland(any()) } returns opprettResponse("/pdl/utlandResponseInnflytting.json")

        val person = runBlocking {
            service.hentUtland(Foedselsnummer.of(TRIVIELL_MIDTPUNKT))
        }


        assertEquals(2, person.innflyttingTilNorge?.size)
        assertEquals("ESP", person.innflyttingTilNorge?.get(0)?.fraflyttingsland)
        assertEquals("1970-06-06T00:00", person.innflyttingTilNorge?.get(0)?.dato)
        assertTrue(person.utflyttingFraNorge?.isEmpty()!!)


    }

     */
    /*
    @Test
    fun `Hent utland med utflytting mappes korrekt`() {
        coEvery { personKlient.hentUtland(any()) } returns opprettResponse("/pdl/utlandResponseFraflytting.json")

        val person = runBlocking {
            service.hentUtland(Foedselsnummer.of(TRIVIELL_MIDTPUNKT))
        }


        assertEquals(1, person.utflyttingFraNorge?.size)
        assertEquals("FRA", person.utflyttingFraNorge?.get(0)?.tilflyttingsland)
        assertEquals("2021-07-01", person.utflyttingFraNorge?.get(0)?.dato)
        assertTrue(person.innflyttingTilNorge?.isEmpty()!!)


    }

     */

}