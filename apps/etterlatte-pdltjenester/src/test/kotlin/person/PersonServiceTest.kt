package no.nav.etterlatte.person

import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
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
import no.nav.etterlatte.pdl.mapper.ParallelleSannheterService
import no.nav.etterlatte.pdlFolkeregisteridentifikator
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.fail
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

internal class PersonServiceTest {
    private val pdlKlientMock = mockk<PdlKlient>()
    private val ppsKlientMock = mockk<ParallelleSannheterKlient>()

    private val ppsService = ParallelleSannheterService(ppsKlientMock, pdlKlientMock, mockk())
    private val featureToggleService = mockk<FeatureToggleService>()
    private val personService = PersonService(pdlKlientMock, ppsService)

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

        coEvery { pdlKlientMock.hentPerson(any()) } returns personResponse
        coEvery { pdlKlientMock.hentPersonBolk(any(), any()) } returns personBolkResponse
        coEvery {
            pdlKlientMock.hentPdlIdentifikator(
                HentPdlIdentRequest(PersonIdent(aktorIdMedFolkeregisterIdent)),
            )
        } returns personIdentResponse
        coEvery {
            pdlKlientMock.hentPdlIdentifikator(
                HentPdlIdentRequest(PersonIdent(aktorIdMedFolkeregisterIdent)),
            )
        } returns personIdentResponse
        coEvery {
            pdlKlientMock.hentPdlIdentifikator(
                HentPdlIdentRequest(
                    PersonIdent(aktorIdMedNpid),
                ),
            )
        } returns personNpidResponse
        coEvery { ppsKlientMock.avklarFolkeregisteridentifikator(any()) } returns hentPerson.folkeregisteridentifikator!!.first()
        coEvery { ppsKlientMock.avklarNavn(any()) } returns hentPerson.navn.first()
        coEvery { ppsKlientMock.avklarAdressebeskyttelse(any()) } returns null
        coEvery { ppsKlientMock.avklarStatsborgerskap(any()) } returns hentPerson.statsborgerskap?.first()
        coEvery { ppsKlientMock.avklarSivilstand(any(), any()) } returns null
        coEvery { ppsKlientMock.avklarFoedselsdato(any()) } returns hentPerson.foedselsdato.first()
        coEvery { ppsKlientMock.avklarFoedested(any()) } returns hentPerson.foedested.first()
        coEvery { ppsKlientMock.avklarDoedsfall(any()) } returns null
        coEvery { ppsKlientMock.avklarBostedsadresse(any()) } returns hentPerson.bostedsadresse?.first()
        coEvery { ppsKlientMock.avklarKontaktadresse(any()) } returns hentPerson.kontaktadresse?.first()
        coEvery { ppsKlientMock.avklarOppholdsadresse(any()) } returns hentPerson.oppholdsadresse?.first()
        coEvery { pdlKlientMock.hentGeografiskTilknytning(any()) } returns geografiskTilknytning
    }

    @AfterEach
    fun afterEach() {
        clearAllMocks()
    }

    @Test
    fun `skal mappe avdoed med barnekull og ta med barnas foreldrerelasjoner`() {
        val fnrBarn1 = pdlFolkeregisteridentifikator("09508229892")
        val fnrBarn2 = pdlFolkeregisteridentifikator("17418340118")

        val expectedBarnFnr = listOf("09508229892", "17418340118")

        coEvery {
            ppsKlientMock.avklarFolkeregisteridentifikator(match { it.any { it.identifikasjonsnummer == fnrBarn1.identifikasjonsnummer } })
        } returns fnrBarn1
        coEvery {
            ppsKlientMock.avklarFolkeregisteridentifikator(match { it.any { it.identifikasjonsnummer == fnrBarn2.identifikasjonsnummer } })
        } returns fnrBarn2

        val person =
            runBlocking {
                personService.hentPerson(
                    HentPersonRequest(TRIVIELL_MIDTPUNKT, rolle = PersonRolle.AVDOED, listOf(SakType.BARNEPENSJON)),
                )
            }
        val avdoedesBarn = person.avdoedesBarn!!
        avdoedesBarn.map { it.foedselsnummer.value } shouldContainExactlyInAnyOrder expectedBarnFnr

        avdoedesBarn.forEach { barn ->
            barn.familieRelasjon!!.barn shouldBe null
            barn.familieRelasjon!!.foreldre shouldNotBe null
            barn.familieRelasjon!!.ansvarligeForeldre shouldNotBe null
        }
    }

    @Test
    fun `skal mappe person som inkluderer familierelasjon (foreldre)`() {
        val person =
            runBlocking {
                personService.hentOpplysningsperson(
                    HentPersonRequest(TRIVIELL_MIDTPUNKT, rolle = PersonRolle.BARN, listOf(SakType.BARNEPENSJON)),
                )
            }

        val expectedForeldreFnr = listOf("18498248795", "16478313601")

        assertNotNull(person.familieRelasjon?.verdi?.foreldre)
        assertEquals(
            2,
            person.familieRelasjon
                ?.verdi
                ?.foreldre
                ?.size,
        )
        assertTrue(
            person.familieRelasjon
                ?.verdi
                ?.foreldre
                ?.get(0)
                ?.value in expectedForeldreFnr,
        )
        assertTrue(
            person.familieRelasjon
                ?.verdi
                ?.foreldre
                ?.get(1)
                ?.value in expectedForeldreFnr,
        )
    }

    @Test
    fun `skal mappe person som inkluderer familierelasjon (ansvarlige foreldre)`() {
        val person =
            runBlocking {
                personService.hentOpplysningsperson(
                    HentPersonRequest(TRIVIELL_MIDTPUNKT, rolle = PersonRolle.BARN, listOf(SakType.BARNEPENSJON)),
                )
            }

        val expectedForeldreFnr = listOf("18498248795", "16478313601")

        assertNotNull(person.familieRelasjon?.verdi?.ansvarligeForeldre)
        assertEquals(
            2,
            person.familieRelasjon
                ?.verdi
                ?.ansvarligeForeldre
                ?.size,
        )
        assertTrue(
            person.familieRelasjon
                ?.verdi
                ?.foreldre
                ?.get(0)
                ?.value in expectedForeldreFnr,
        )
        assertTrue(
            person.familieRelasjon
                ?.verdi
                ?.foreldre
                ?.get(1)
                ?.value in expectedForeldreFnr,
        )
    }

    @Test
    fun `Hent utland med innflytting mappes korrekt`() {
        val person =
            runBlocking {
                personService.hentOpplysningsperson(
                    HentPersonRequest(TRIVIELL_MIDTPUNKT, rolle = PersonRolle.BARN, listOf(SakType.BARNEPENSJON)),
                )
            }

        assertEquals(
            2,
            person.utland
                ?.verdi
                ?.innflyttingTilNorge
                ?.size,
        )
        assertEquals(
            "NIC",
            person.utland
                ?.verdi
                ?.innflyttingTilNorge
                ?.get(0)
                ?.fraflyttingsland,
        )
        assertEquals(
            "1970-09-14",
            person.utland
                ?.verdi
                ?.innflyttingTilNorge
                ?.get(0)
                ?.dato
                .toString(),
        )
    }

    @Test
    fun `Hent utland med utflytting mappes korrekt`() {
        val person =
            runBlocking {
                personService.hentOpplysningsperson(
                    HentPersonRequest(TRIVIELL_MIDTPUNKT, rolle = PersonRolle.BARN, listOf(SakType.BARNEPENSJON)),
                )
            }

        assertEquals(
            1,
            person.utland
                ?.verdi
                ?.utflyttingFraNorge
                ?.size,
        )
        assertEquals(
            "FRA",
            person.utland
                ?.verdi
                ?.utflyttingFraNorge
                ?.get(0)
                ?.tilflyttingsland,
        )
        assertEquals(
            "2021-07-01",
            person.utland
                ?.verdi
                ?.utflyttingFraNorge
                ?.get(0)
                ?.dato
                .toString(),
        )
    }

    @Test
    fun `Person ikke finnes kaster exception`() {
        coEvery { pdlKlientMock.hentPerson(any()) } returns PdlPersonResponse(data = null, errors = emptyList())

        assertThrows<PdlForesporselFeilet> {
            runBlocking {
                personService.hentPerson(
                    HentPersonRequest(TRIVIELL_MIDTPUNKT, rolle = PersonRolle.BARN, listOf(SakType.BARNEPENSJON)),
                )
            }
        }
    }

    @Test
    fun `Finner ikke person i PDL`() {
        coEvery { pdlKlientMock.hentPerson(any()) } returns mockResponse("/pdl/person_ikke_funnet.json")

        runBlocking {
            assertThrows<FantIkkePersonException> {
                personService.hentPerson(
                    HentPersonRequest(
                        STOR_SNERK,
                        rolle = PersonRolle.BARN,
                        listOf(SakType.BARNEPENSJON),
                    ),
                )
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
        coEvery { pdlKlientMock.hentPdlIdentifikator(any()) } returns mockResponse("/pdl/ident_ikke_funnet.json")
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
        coEvery { pdlKlientMock.hentGeografiskTilknytning(any()) } returns
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
        coEvery { pdlKlientMock.hentAdressebeskyttelse(any()) } returns
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
                personService.hentAdressebeskyttelseGradering(
                    HentAdressebeskyttelseRequest(PersonIdent(TRIVIELL_MIDTPUNKT.value), SakType.BARNEPENSJON),
                )
            }

        assertEquals(AdressebeskyttelseGradering.valueOf(pdlGradering.name), gradering)
    }
}
