package no.nav.etterlatte.person

import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.STOR_SNERK
import no.nav.etterlatte.TRIVIELL_MIDTPUNKT
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.pdl.FantIkkePersonException
import no.nav.etterlatte.libs.common.person.AdressebeskyttelseGradering
import no.nav.etterlatte.libs.common.person.HentAdressebeskyttelseRequest
import no.nav.etterlatte.libs.common.person.HentGeografiskTilknytningRequest
import no.nav.etterlatte.libs.common.person.HentPdlIdentRequest
import no.nav.etterlatte.libs.common.person.HentPersonRequest
import no.nav.etterlatte.libs.common.person.PdlIdentifikator
import no.nav.etterlatte.libs.common.person.PersonIdent
import no.nav.etterlatte.libs.common.person.PersonRolle
import no.nav.etterlatte.libs.testdata.grunnlag.AVDOED_FOEDSELSNUMMER
import no.nav.etterlatte.mockResponse
import no.nav.etterlatte.pdl.ParallelleSannheterKlient
import no.nav.etterlatte.pdl.PdlAdressebeskyttelse
import no.nav.etterlatte.pdl.PdlAdressebeskyttelseData
import no.nav.etterlatte.pdl.PdlAdressebeskyttelseResponse
import no.nav.etterlatte.pdl.PdlGeografiskTilknytning
import no.nav.etterlatte.pdl.PdlGeografiskTilknytningData
import no.nav.etterlatte.pdl.PdlGeografiskTilknytningResponse
import no.nav.etterlatte.pdl.PdlGradering
import no.nav.etterlatte.pdl.PdlGtType
import no.nav.etterlatte.pdl.PdlHentPerson
import no.nav.etterlatte.pdl.PdlHentPersonAdressebeskyttelse
import no.nav.etterlatte.pdl.PdlIdentResponse
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
import org.junit.jupiter.api.fail
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

internal class PersonServiceTest {
    private val pdlKlient = mockk<PdlKlient>()
    private val ppsKlient = mockk<ParallelleSannheterKlient>()
    private val featureToggleService = mockk<FeatureToggleService>()
    private val personService = PersonService(pdlKlient, ppsKlient)

    private val aktorIdMedNpid = "1234567890123"
    private val aktorIdMedFolkeregisterIdent = AVDOED_FOEDSELSNUMMER.value

    @BeforeEach
    fun beforeEach() {
        val personResponse: PdlPersonResponse = mockResponse("/pdl/person.json")
        val personBolkResponse: PdlPersonResponseBolk = mockResponse("/pdl/personBolk.json")
        val personIdentResponse: PdlIdentResponse = mockResponse("/pdl/folkeregisterident.json")
        val personNpidResponse: PdlIdentResponse = mockResponse("/pdl/npid.json")
        val hentPerson: PdlHentPerson = personResponse.data?.hentPerson!!
        val geografiskTilknytning =
            PdlGeografiskTilknytningResponse(
                data =
                    PdlGeografiskTilknytningData(
                        PdlGeografiskTilknytning(
                            gtKommune = "0301",
                            gtBydel = null,
                            gtLand = null,
                            gtType = PdlGtType.KOMMUNE,
                        ),
                    ),
            )

        every { featureToggleService.isEnabled(any(), any()) } returns true

        coEvery { pdlKlient.hentPerson(any()) } returns personResponse
        coEvery { pdlKlient.hentPersonBolk(any(), any()) } returns personBolkResponse
        coEvery {
            pdlKlient.hentPdlIdentifikator(
                HentPdlIdentRequest(PersonIdent(aktorIdMedFolkeregisterIdent)),
            )
        } returns personIdentResponse
        coEvery {
            pdlKlient.hentPdlIdentifikator(
                HentPdlIdentRequest(PersonIdent(aktorIdMedFolkeregisterIdent)),
            )
        } returns personIdentResponse
        coEvery {
            pdlKlient.hentPdlIdentifikator(
                HentPdlIdentRequest(
                    PersonIdent(aktorIdMedNpid),
                ),
            )
        } returns personNpidResponse
        coEvery { ppsKlient.avklarNavn(any()) } returns hentPerson.navn.first()
        coEvery { ppsKlient.avklarAdressebeskyttelse(any()) } returns null
        coEvery { ppsKlient.avklarStatsborgerskap(any()) } returns hentPerson.statsborgerskap?.first()
        coEvery { ppsKlient.avklarSivilstand(any()) } returns null
        coEvery { ppsKlient.avklarFoedsel(any()) } returns hentPerson.foedsel.first()
        coEvery { ppsKlient.avklarDoedsfall(any()) } returns null
        coEvery { ppsKlient.avklarBostedsadresse(any()) } returns hentPerson.bostedsadresse?.first()
        coEvery { ppsKlient.avklarKontaktadresse(any()) } returns hentPerson.kontaktadresse?.first()
        coEvery { ppsKlient.avklarOppholdsadresse(any()) } returns hentPerson.oppholdsadresse?.first()
        coEvery { pdlKlient.hentGeografiskTilknytning(any()) } returns geografiskTilknytning
    }

    @AfterEach
    fun afterEach() {
        clearAllMocks()
    }

    @Test
    fun `skal mappe avdoed med barnekull og ikke ta med barnas relasjoner`() {
        val person =
            runBlocking {
                personService.hentPerson(
                    HentPersonRequest(TRIVIELL_MIDTPUNKT, rolle = PersonRolle.AVDOED, SakType.BARNEPENSJON),
                )
            }

        val expectedBarnFnr = listOf("09508229892", "17418340118")

        val avdoedesBarn = person.avdoedesBarn!!
        avdoedesBarn.map { it.foedselsnummer.value } shouldContainExactlyInAnyOrder expectedBarnFnr

        avdoedesBarn.forEach { barn ->
            barn.familieRelasjon!!.barn shouldBe null
            barn.familieRelasjon!!.foreldre shouldBe null
            barn.familieRelasjon!!.ansvarligeForeldre shouldBe null
        }
    }

    @Test
    fun `skal mappe person som inkluderer familierelasjon (foreldre)`() {
        val person =
            runBlocking {
                personService.hentOpplysningsperson(
                    HentPersonRequest(TRIVIELL_MIDTPUNKT, rolle = PersonRolle.BARN, SakType.BARNEPENSJON),
                )
            }

        val expectedForeldreFnr = listOf("18498248795", "16478313601")

        assertNotNull(person.familieRelasjon?.verdi?.foreldre)
        assertEquals(2, person.familieRelasjon?.verdi?.foreldre?.size)
        assertTrue(person.familieRelasjon?.verdi?.foreldre?.get(0)?.value in expectedForeldreFnr)
        assertTrue(person.familieRelasjon?.verdi?.foreldre?.get(1)?.value in expectedForeldreFnr)
    }

    @Test
    fun `skal mappe person som inkluderer familierelasjon (ansvarlige foreldre)`() {
        val person =
            runBlocking {
                personService.hentOpplysningsperson(
                    HentPersonRequest(TRIVIELL_MIDTPUNKT, rolle = PersonRolle.BARN, SakType.BARNEPENSJON),
                )
            }

        val expectedForeldreFnr = listOf("18498248795", "16478313601")

        assertNotNull(person.familieRelasjon?.verdi?.ansvarligeForeldre)
        assertEquals(2, person.familieRelasjon?.verdi?.ansvarligeForeldre?.size)
        assertTrue(person.familieRelasjon?.verdi?.foreldre?.get(0)?.value in expectedForeldreFnr)
        assertTrue(person.familieRelasjon?.verdi?.foreldre?.get(1)?.value in expectedForeldreFnr)
    }

    @Test
    @Disabled("TODO - få inn datagrunnlag for denne")
    fun `skal mappe person som inkluderer familierelasjon (barn)`() {
        val person =
            runBlocking {
                personService.hentOpplysningsperson(
                    HentPersonRequest(TRIVIELL_MIDTPUNKT, rolle = PersonRolle.BARN, SakType.BARNEPENSJON),
                )
            }

        val foreldreFnr = listOf("18498248795", "16478313601")

        assertNotNull(person.familieRelasjon?.verdi?.barn)
        assertEquals(2, person.familieRelasjon?.verdi?.barn?.size)
        assertTrue(person.familieRelasjon?.verdi?.barn?.get(0)?.value in foreldreFnr)
        assertTrue(person.familieRelasjon?.verdi?.barn?.get(1)?.value in foreldreFnr)
    }

    @Test
    fun `Hent utland med innflytting mappes korrekt`() {
        val person =
            runBlocking {
                personService.hentOpplysningsperson(
                    HentPersonRequest(TRIVIELL_MIDTPUNKT, rolle = PersonRolle.BARN, SakType.BARNEPENSJON),
                )
            }

        assertEquals(2, person.utland?.verdi?.innflyttingTilNorge?.size)
        assertEquals("NIC", person.utland?.verdi?.innflyttingTilNorge?.get(0)?.fraflyttingsland)
        assertEquals("1970-09-14", person.utland?.verdi?.innflyttingTilNorge?.get(0)?.dato.toString())
    }

    @Test
    fun `Hent utland med utflytting mappes korrekt`() {
        val person =
            runBlocking {
                personService.hentOpplysningsperson(
                    HentPersonRequest(TRIVIELL_MIDTPUNKT, rolle = PersonRolle.BARN, SakType.BARNEPENSJON),
                )
            }

        assertEquals(1, person.utland?.verdi?.utflyttingFraNorge?.size)
        assertEquals("FRA", person.utland?.verdi?.utflyttingFraNorge?.get(0)?.tilflyttingsland)
        assertEquals("2021-07-01", person.utland?.verdi?.utflyttingFraNorge?.get(0)?.dato.toString())
    }

    @Test
    fun `Person ikke finnes kaster exception`() {
        coEvery { pdlKlient.hentPerson(any()) } returns PdlPersonResponse(data = null, errors = emptyList())

        assertThrows<PdlForesporselFeilet> {
            runBlocking {
                personService.hentPerson(
                    HentPersonRequest(TRIVIELL_MIDTPUNKT, rolle = PersonRolle.BARN, SakType.BARNEPENSJON),
                )
            }
        }
    }

    @Test
    fun `Finner ikke person i PDL`() {
        coEvery { pdlKlient.hentPerson(any()) } returns mockResponse("/pdl/person_ikke_funnet.json")

        runBlocking {
            assertThrows<FantIkkePersonException> {
                personService.hentPerson(HentPersonRequest(STOR_SNERK, rolle = PersonRolle.BARN, SakType.BARNEPENSJON))
            }
        }
    }

    @Test
    fun `Skal hente folkeregisterident for aktoerid`() {
        val personIdentResponse =
            runBlocking {
                personService.hentPdlIdentifikator(
                    HentPdlIdentRequest(PersonIdent(aktorIdMedFolkeregisterIdent)),
                )
            }

        if (personIdentResponse !is PdlIdentifikator.FolkeregisterIdent) {
            fail("Fikk ikke folkeregisteridentifikator")
        }
        val expectedFolkeregisterIdent = "09508229892"
        assertEquals(expectedFolkeregisterIdent, personIdentResponse.folkeregisterident.value)
    }

    @Test
    fun `Skal hente npid for aktoerid som ikke har folkeregisteridentifikator`() {
        val personIdentResponse =
            runBlocking {
                personService.hentPdlIdentifikator(
                    HentPdlIdentRequest(PersonIdent(aktorIdMedNpid)),
                )
            }

        if (personIdentResponse !is PdlIdentifikator.Npid) {
            fail("Fikk ikke Npid")
        }
        val expectedNpid = "09706511617"
        assertEquals(expectedNpid, personIdentResponse.npid.ident)
    }

    @Test
    fun `finner ikke folkeregisterident`() {
        coEvery { pdlKlient.hentPdlIdentifikator(any()) } returns mockResponse("/pdl/ident_ikke_funnet.json")
        runBlocking {
            assertThrows<FantIkkePersonException> {
                personService.hentPdlIdentifikator(HentPdlIdentRequest(PersonIdent("1234")))
            }
        }
    }

    @Test
    fun `skal mappe geografisk tilknytning`() {
        val tilknytning =
            runBlocking {
                personService.hentGeografiskTilknytning(
                    HentGeografiskTilknytningRequest(TRIVIELL_MIDTPUNKT, SakType.BARNEPENSJON),
                )
            }

        assertEquals(tilknytning.ukjent, false)
        assertEquals(tilknytning.geografiskTilknytning(), "0301")
    }

    @Test
    fun `Bruker uten geografisk tilknytning`() {
        coEvery { pdlKlient.hentGeografiskTilknytning(any()) } returns
            PdlGeografiskTilknytningResponse(
                data =
                    PdlGeografiskTilknytningData(
                        null,
                    ),
            )

        val tilknytning =
            runBlocking {
                personService.hentGeografiskTilknytning(
                    HentGeografiskTilknytningRequest(TRIVIELL_MIDTPUNKT, SakType.BARNEPENSJON),
                )
            }

        assertNotNull(tilknytning)
        assertTrue(tilknytning.ukjent)
    }

    @ParameterizedTest
    @EnumSource(PdlGradering::class)
    fun `Skal hente gradering for person`(pdlGradering: PdlGradering) {
        coEvery { pdlKlient.hentAdressebeskyttelse(any()) } returns
            PdlAdressebeskyttelseResponse(
                data =
                    PdlAdressebeskyttelseData(
                        PdlHentPersonAdressebeskyttelse(
                            listOf(PdlAdressebeskyttelse(pdlGradering, null, mockk())),
                        ),
                    ),
            )

        val gradering =
            runBlocking {
                personService.hentAdressebeskyttelseGradering(HentAdressebeskyttelseRequest(PersonIdent(TRIVIELL_MIDTPUNKT.value)))
            }

        assertEquals(AdressebeskyttelseGradering.valueOf(pdlGradering.name), gradering)
    }
}
