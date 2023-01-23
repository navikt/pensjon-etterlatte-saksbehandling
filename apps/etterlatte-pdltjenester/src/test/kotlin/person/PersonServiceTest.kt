package no.nav.etterlatte.person

import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.STOR_SNERK
import no.nav.etterlatte.TRIVIELL_MIDTPUNKT
import no.nav.etterlatte.libs.common.person.HentFolkeregisterIdentRequest
import no.nav.etterlatte.libs.common.person.HentPersonRequest
import no.nav.etterlatte.libs.common.person.PersonRolle
import no.nav.etterlatte.libs.ktor.Saksbehandler
import no.nav.etterlatte.libs.sporingslogg.Sporingslogg
import no.nav.etterlatte.mockResponse
import no.nav.etterlatte.pdl.ParallelleSannheterKlient
import no.nav.etterlatte.pdl.PdlFolkeregisterIdentResponse
import no.nav.etterlatte.pdl.PdlHentPerson
import no.nav.etterlatte.pdl.PdlKlient
import no.nav.etterlatte.pdl.PdlPersonResponse
import no.nav.etterlatte.pdl.PdlPersonResponseBolk
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
    private val personService = PersonService(pdlKlient, ppsKlient, Sporingslogg())
    private val saksbehandler = Saksbehandler("A1234")

    @BeforeEach
    fun beforeEach() {
        val personResponse: PdlPersonResponse = mockResponse("/pdl/person.json")
        val personBolkResponse: PdlPersonResponseBolk = mockResponse("/pdl/personBolk.json")
        val personIdentResponse: PdlFolkeregisterIdentResponse = mockResponse("/pdl/folkeregisterident.json")
        val hentPerson: PdlHentPerson = personResponse.data?.hentPerson!!

        coEvery { pdlKlient.hentPerson(any(), any()) } returns personResponse
        coEvery { pdlKlient.hentPersonBolk(any()) } returns personBolkResponse
        coEvery { pdlKlient.hentFolkeregisterIdent(any()) } returns personIdentResponse
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
    fun `skal mappe avdoed med barnekull`() {
        val person = runBlocking {
            personService.hentPerson(HentPersonRequest(TRIVIELL_MIDTPUNKT, rolle = PersonRolle.AVDOED), saksbehandler)
        }

        val expectedBarnFnr = listOf("70078749472", "06067018735")

        assertNotNull(person.avdoedesBarn)
        assertEquals(2, person.avdoedesBarn?.size)
        assertTrue(person.avdoedesBarn?.get(0)?.foedselsnummer?.value in expectedBarnFnr)
        assertTrue(person.avdoedesBarn?.get(1)?.foedselsnummer?.value in expectedBarnFnr)
    }

    @Test
    fun `skal mappe person som inkluderer familierelasjon (foreldre)`() {
        val person = runBlocking {
            personService.hentOpplysningsperson(
                HentPersonRequest(TRIVIELL_MIDTPUNKT, rolle = PersonRolle.BARN),
                saksbehandler
            )
        }

        val expectedForeldreFnr = listOf("26117512737", "14097030880")

        assertNotNull(person.familieRelasjon?.verdi?.foreldre)
        assertEquals(2, person.familieRelasjon?.verdi?.foreldre?.size)
        assertTrue(person.familieRelasjon?.verdi?.foreldre?.get(0)?.value in expectedForeldreFnr)
        assertTrue(person.familieRelasjon?.verdi?.foreldre?.get(1)?.value in expectedForeldreFnr)
    }

    @Test
    fun `skal mappe person som inkluderer familierelasjon (ansvarlige foreldre)`() {
        val person = runBlocking {
            personService.hentOpplysningsperson(
                HentPersonRequest(TRIVIELL_MIDTPUNKT, rolle = PersonRolle.BARN),
                saksbehandler
            )
        }

        val expectedForeldreFnr = listOf("26117512737", "14097030880")

        assertNotNull(person.familieRelasjon?.verdi?.ansvarligeForeldre)
        assertEquals(2, person.familieRelasjon?.verdi?.ansvarligeForeldre?.size)
        assertTrue(person.familieRelasjon?.verdi?.foreldre?.get(0)?.value in expectedForeldreFnr)
        assertTrue(person.familieRelasjon?.verdi?.foreldre?.get(1)?.value in expectedForeldreFnr)
    }

    @Test
    @Disabled("TODO - få inn datagrunnlag for denne")
    fun `skal mappe person som inkluderer familierelasjon (barn)`() {
        val person = runBlocking {
            personService.hentOpplysningsperson(
                HentPersonRequest(TRIVIELL_MIDTPUNKT, rolle = PersonRolle.BARN),
                saksbehandler
            )
        }

        val foreldreFnr = listOf("26117512737", "14097030880")

        assertNotNull(person.familieRelasjon?.verdi?.barn)
        assertEquals(2, person.familieRelasjon?.verdi?.barn?.size)
        assertTrue(person.familieRelasjon?.verdi?.barn?.get(0)?.value in foreldreFnr)
        assertTrue(person.familieRelasjon?.verdi?.barn?.get(1)?.value in foreldreFnr)
    }

    @Test
    fun `Hent utland med innflytting mappes korrekt`() {
        val person = runBlocking {
            personService.hentOpplysningsperson(
                HentPersonRequest(TRIVIELL_MIDTPUNKT, rolle = PersonRolle.BARN),
                saksbehandler
            )
        }

        assertEquals(2, person.utland?.verdi?.innflyttingTilNorge?.size)
        assertEquals("NIC", person.utland?.verdi?.innflyttingTilNorge?.get(0)?.fraflyttingsland)
        assertEquals("1970-09-14", person.utland?.verdi?.innflyttingTilNorge?.get(0)?.dato.toString())
    }

    @Test
    fun `Hent utland med utflytting mappes korrekt`() {
        val person = runBlocking {
            personService.hentOpplysningsperson(
                HentPersonRequest(TRIVIELL_MIDTPUNKT, rolle = PersonRolle.BARN),
                saksbehandler
            )
        }

        assertEquals(1, person.utland?.verdi?.utflyttingFraNorge?.size)
        assertEquals("FRA", person.utland?.verdi?.utflyttingFraNorge?.get(0)?.tilflyttingsland)
        assertEquals("2021-07-01", person.utland?.verdi?.utflyttingFraNorge?.get(0)?.dato.toString())
    }

    @Test
    fun `Person ikke finnes kaster exception`() {
        coEvery { pdlKlient.hentPerson(any(), any()) } returns PdlPersonResponse(data = null, errors = emptyList())

        assertThrows<PdlForesporselFeilet> {
            runBlocking {
                personService.hentPerson(HentPersonRequest(TRIVIELL_MIDTPUNKT, rolle = PersonRolle.BARN), saksbehandler)
            }
        }
    }

    @Test
    fun `Finner ikke person i PDL`() {
        coEvery { pdlKlient.hentPerson(any(), any()) } returns mockResponse("/pdl/person_ikke_funnet.json")

        runBlocking {
            assertThrows<PdlFantIkkePerson> {
                personService.hentPerson(HentPersonRequest(STOR_SNERK, rolle = PersonRolle.BARN), saksbehandler)
            }
        }
    }

    @Test
    fun `Skal hente folkeregisterident for aktoerid`() {
        val personIdentResponse =
            runBlocking {
                personService.hentFolkeregisterIdent(
                    HentFolkeregisterIdentRequest("2305469522806"),
                    saksbehandler
                )
            }
        val expectedFolkeregisterIdent = "70078749472"
        assertEquals(expectedFolkeregisterIdent, personIdentResponse.folkeregisterident.value)
    }

    @Test
    fun `finner ikke folkeregisterident`() {
        coEvery { pdlKlient.hentFolkeregisterIdent(any()) } returns mockResponse("/pdl/ident_ikke_funnet.json")
        runBlocking {
            assertThrows<PdlFantIkkePerson> {
                personService.hentFolkeregisterIdent(HentFolkeregisterIdentRequest("1234"), saksbehandler)
            }
        }
    }
}