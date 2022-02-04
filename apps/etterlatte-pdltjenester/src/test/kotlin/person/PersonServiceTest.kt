package person

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonTypeRef
import io.ktor.features.NotFoundException
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.libs.common.pdl.EyHentAdresseRequest
import no.nav.etterlatte.libs.common.person.Foedselsnummer
import no.nav.etterlatte.person.PersonKlient
import no.nav.etterlatte.person.PersonService
import no.nav.etterlatte.person.pdl.PersonResponse
import no.nav.etterlatte.person.pdl.Sivilstandstype
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class PersonServiceTest {

    private companion object {
        private const val TREIG_FLOSKEL = "04096222195"
        private const val TRIVIELL_MIDTPUNKT = "19040550081"
        private const val STOR_SNERK = "11057523044"
        private const val DOLL_KOPP = "31017123133"
    }

    private val mapper = jacksonObjectMapper()
        .configure(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL, true)
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .registerModule(JavaTimeModule())

    private val personKlient = mockk<PersonKlient>()

    //TODO legge tilbake kodeverkService
    private val service = PersonService(personKlient)

    @AfterEach
    fun afterEach() {
        //coVerify(exactly = 1) { personKlient.hentPerson(any()) }
        clearAllMocks()
    }

    @Test
    fun `Komplett person mappes korrekt`() {
        coEvery { personKlient.hentPerson(any()) } returns opprettResponse("/pdl/personResponse.json")

        val person = runBlocking {
            service.hentPerson(Foedselsnummer.of(TRIVIELL_MIDTPUNKT))
        }

        assertEquals("TRIVIELL", person.fornavn)
        assertEquals("MIDTPUNKT", person.etternavn)
        assertEquals(TRIVIELL_MIDTPUNKT, person.foedselsnummer.value)
        assertEquals(2005, person.foedselsaar)
        assertEquals("2005-04-19", person.foedselsdato)

        //assertEquals("Hamnavikvegen", person.adresse)
       // assertEquals("30", person.husnummer)
       // assertNull(person.husbokstav)
      //  assertEquals("0380", person.postnummer)
        assertEquals("NOR", person.statsborgerskap)
        assertEquals(false, person.adressebeskyttelse)
        assertNull(person.sivilstatus)
    }

    @Test
    fun `Person med sivilstand-historikk mappes korrekt`() {
        coEvery { personKlient.hentPerson(any()) } returns opprettResponse("/pdl/endretSivilstand.json")

        val person = runBlocking {
            service.hentPerson(Foedselsnummer.of(TRIVIELL_MIDTPUNKT))
        }

        assertEquals(Sivilstandstype.ENKE_ELLER_ENKEMANN.name, person.sivilstatus)
    }

    @Test
    fun `Person med adressebeskyttelse mappes korrekt`() {
        coEvery { personKlient.hentPerson(any()) } returns opprettResponse("/pdl/adressebeskyttetPerson.json")

        val person = runBlocking {
            service.hentPerson(Foedselsnummer.of(TRIVIELL_MIDTPUNKT))
        }

        assertEquals(true, person.adressebeskyttelse)
        assertNull(person.adresse)
        //assertNull(person.husbokstav)
        //assertNull(person.husnummer)
        //assertNull(person.postnummer)
    }

    @Test
    fun `Person ikke finnes kaster exception`() {
        coEvery { personKlient.hentPerson(any()) } returns PersonResponse(data = null, errors = emptyList())

        assertThrows<NotFoundException> {
            runBlocking {
                service.hentPerson(Foedselsnummer.of(TREIG_FLOSKEL))
            }
        }
    }

    @Test
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
    @Test
    fun `Faktisk respons fra PDL kan hentes opp`() {
        coEvery { personKlient.hentPerson(any()) } returns opprettResponse("/pdl/personResponseFullPDL.json")

        val person = runBlocking {
            service.hentPerson(Foedselsnummer.of(STOR_SNERK))
        }

        assertEquals("STOR", person.fornavn)
    }

    @Test
    fun `Finner ikke person i PDL`() {
        coEvery { personKlient.hentPerson(any()) } returns opprettResponse("/pdl/personResponseIkkeFunnet.json")

        runBlocking {
            assertThrows<NotFoundException> {
                service.hentPerson(Foedselsnummer.of(STOR_SNERK))
            }
        }
    }

    @Test
    fun `Hent adresse med vegadresse mappes korrekt`() {
        coEvery { personKlient.hentAdresse(any(), true) } returns opprettResponse("/pdl/adresseResponseVegadresse.json")

        val adresse = runBlocking {
            service.hentAdresse(EyHentAdresseRequest( DOLL_KOPP, true))
        }


        assertEquals("Slettvikveien", adresse.bostedsadresse?.vegadresse?.adressenavn)
        assertEquals("34", adresse.bostedsadresse?.vegadresse?.husnummer)
        assertEquals(null, adresse.bostedsadresse?.vegadresse?.husbokstav)
        assertEquals("7318", adresse.bostedsadresse?.vegadresse?.postnummer)

        assertEquals("Tunnelveien", adresse.kontaktadresse?.vegadresse?.adressenavn)
        assertEquals("36", adresse.kontaktadresse?.vegadresse?.husnummer)
        assertEquals(null, adresse.kontaktadresse?.vegadresse?.husbokstav)
        assertEquals("3085", adresse.kontaktadresse?.vegadresse?.postnummer)

        assertEquals("Tjordal", adresse.oppholdsadresse?.vegadresse?.adressenavn)
        assertEquals("24", adresse.oppholdsadresse?.vegadresse?.husnummer)
        assertEquals(null, adresse.oppholdsadresse?.vegadresse?.husbokstav)
        assertEquals("5570", adresse.oppholdsadresse?.vegadresse?.postnummer)

    }


    private inline fun <reified T> opprettResponse(fil: String): T {
        val json = javaClass.getResource(fil)!!.readText()

        return mapper.readValue(json, jacksonTypeRef())
    }

}