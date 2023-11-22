package no.nav.etterlatte.grunnlag

import com.fasterxml.jackson.databind.JsonNode
import io.mockk.called
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import lagGrunnlagHendelse
import mockPerson
import no.nav.etterlatte.grunnlag.klienter.PdlTjenesterKlientImpl
import no.nav.etterlatte.grunnlag.klienter.PersondataKlient
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.behandling.Saksrolle
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.Opplysning
import no.nav.etterlatte.libs.common.grunnlag.hentBostedsadresse
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Navn
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype.BOSTEDSADRESSE
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype.FOEDELAND
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype.FOEDSELSDATO
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype.FOEDSELSNUMMER
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype.NAVN
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype.PERSONGALLERI_V1
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype.PERSONROLLE
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.pdl.PersonDTO
import no.nav.etterlatte.libs.common.periode.Periode
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.person.PersonRolle
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.libs.common.toJsonNode
import no.nav.etterlatte.libs.testdata.grunnlag.ADRESSE_DEFAULT
import no.nav.etterlatte.libs.testdata.grunnlag.AVDOED_FOEDSELSNUMMER
import no.nav.etterlatte.libs.testdata.grunnlag.GJENLEVENDE_FOEDSELSNUMMER
import no.nav.etterlatte.libs.testdata.grunnlag.GrunnlagTestData
import no.nav.etterlatte.libs.testdata.grunnlag.HELSOESKEN_FOEDSELSNUMMER
import no.nav.etterlatte.libs.testdata.grunnlag.INNSENDER_FOEDSELSNUMMER
import no.nav.etterlatte.libs.testdata.grunnlag.SOEKER2_FOEDSELSNUMMER
import no.nav.etterlatte.libs.testdata.grunnlag.kilde
import no.nav.etterlatte.libs.testdata.grunnlag.statiskUuid
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Month
import java.time.YearMonth
import java.util.UUID

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class GrunnlagServiceTest {
    private val opplysningDaoMock = mockk<OpplysningDao>()
    private val pdlTjenesterKlientImpl = mockk<PdlTjenesterKlientImpl>()
    private val persondataKlient = mockk<PersondataKlient>()
    private val grunnlagHenter = mockk<GrunnlagHenter>()
    private val grunnlagService =
        RealGrunnlagService(
            pdlTjenesterKlientImpl,
            opplysningDaoMock,
            mockk(),
            persondataKlient,
            grunnlagHenter,
        )

    private val testData = GrunnlagTestData()

    @BeforeAll
    fun beforeAll() {
        every { opplysningDaoMock.finnNyesteGrunnlagForBehandling(any(), PERSONGALLERI_V1) } returns
            lagGrunnlagHendelse(
                1,
                4,
                PERSONGALLERI_V1,
                id = statiskUuid,
                fnr = testData.soeker.foedselsnummer,
                verdi = testData.hentPersonGalleri().toJsonNode(),
                kilde = kilde,
            )
    }

    @Nested
    inner class MapperTilRiktigKategoriTest {
        private val nyttNavn = Navn("Mohammed", "ali", "Ali")
        private val nyFødselsdag = LocalDate.of(2013, 12, 24)

        private fun lagGrunnlagForPerson(
            fnr: Folkeregisteridentifikator,
            personRolle: PersonRolle,
        ) = listOf(
            lagGrunnlagHendelse(
                1,
                1,
                NAVN,
                id = statiskUuid,
                fnr = fnr,
                verdi = nyttNavn.toJsonNode(),
                kilde = kilde,
            ),
            lagGrunnlagHendelse(
                1,
                2,
                FOEDSELSDATO,
                id = statiskUuid,
                fnr = fnr,
                verdi = nyFødselsdag.toJsonNode(),
                kilde = kilde,
            ),
            lagGrunnlagHendelse(
                1,
                3,
                PERSONROLLE,
                id = statiskUuid,
                fnr = fnr,
                verdi = personRolle.toJsonNode(),
                kilde = kilde,
            ),
            lagGrunnlagHendelse(
                1,
                4,
                PERSONGALLERI_V1,
                id = statiskUuid,
                fnr = fnr,
                verdi = testData.hentPersonGalleri().toJsonNode(),
                kilde = kilde,
            ),
        )

        @Test
        fun `skal mappe om dataen fra DB til søker`() {
            val grunnlagshendelser = lagGrunnlagForPerson(testData.soeker.foedselsnummer, PersonRolle.BARN)

            every { opplysningDaoMock.hentAlleGrunnlagForSak(1) } returns grunnlagshendelser

            val actual = grunnlagService.hentOpplysningsgrunnlagForSak(1)!!
            val expected =
                mapOf(
                    NAVN to Opplysning.Konstant(statiskUuid, kilde, nyttNavn.toJsonNode()),
                    FOEDSELSDATO to Opplysning.Konstant(statiskUuid, kilde, nyFødselsdag.toJsonNode()),
                )

            Assertions.assertEquals(expected[NAVN], actual.soeker[NAVN])
            Assertions.assertEquals(expected[FOEDSELSDATO], actual.soeker[FOEDSELSDATO])
        }

        @Test
        fun `skal mappe om dataen fra DB til avdød`() {
            val grunnlagshendelser = lagGrunnlagForPerson(testData.avdoed.foedselsnummer, PersonRolle.AVDOED)

            every { opplysningDaoMock.hentAlleGrunnlagForSak(1) } returns grunnlagshendelser

            val actual = grunnlagService.hentOpplysningsgrunnlagForSak(1)!!
            val expected =
                mapOf(
                    NAVN to Opplysning.Konstant(statiskUuid, kilde, nyttNavn.toJsonNode()),
                    FOEDSELSDATO to Opplysning.Konstant(statiskUuid, kilde, nyFødselsdag.toJsonNode()),
                    PERSONROLLE to Opplysning.Konstant(statiskUuid, kilde, PersonRolle.AVDOED.toJsonNode()),
                )

            Assertions.assertEquals(expected[NAVN], actual.hentAvdoed()[NAVN])
            Assertions.assertEquals(expected[FOEDSELSDATO], actual.hentAvdoed()[FOEDSELSDATO])
        }

        @Test
        fun `skal mappe om dataen fra DB til gjenlevende`() {
            val grunnlagshendelser = lagGrunnlagForPerson(testData.gjenlevende.foedselsnummer, PersonRolle.GJENLEVENDE)

            every { opplysningDaoMock.hentAlleGrunnlagForSak(1) } returns grunnlagshendelser

            val actual = grunnlagService.hentOpplysningsgrunnlagForSak(1)!!
            val expected =
                mapOf(
                    NAVN to Opplysning.Konstant(statiskUuid, kilde, nyttNavn.toJsonNode()),
                    FOEDSELSDATO to Opplysning.Konstant(statiskUuid, kilde, nyFødselsdag.toJsonNode()),
                    PERSONROLLE to Opplysning.Konstant(statiskUuid, kilde, PersonRolle.GJENLEVENDE.toJsonNode()),
                )

            Assertions.assertEquals(expected[NAVN], actual.hentGjenlevende()[NAVN])
            Assertions.assertEquals(expected[FOEDSELSDATO], actual.hentGjenlevende()[FOEDSELSDATO])
        }

        @Test
        fun `skal mappe om dataen fra DB til søsken`() {
            val grunnlagshendelser = lagGrunnlagForPerson(testData.soesken.foedselsnummer, PersonRolle.BARN)

            every { opplysningDaoMock.hentAlleGrunnlagForSak(1) } returns grunnlagshendelser

            val actual = grunnlagService.hentOpplysningsgrunnlagForSak(1)!!
            val expected =
                mapOf(
                    NAVN to Opplysning.Konstant(statiskUuid, kilde, nyttNavn.toJsonNode()),
                    FOEDSELSDATO to Opplysning.Konstant(statiskUuid, kilde, nyFødselsdag.toJsonNode()),
                    PERSONROLLE to Opplysning.Konstant(statiskUuid, kilde, PersonRolle.BARN.toJsonNode()),
                )

            Assertions.assertEquals(expected[NAVN], actual.familie.single()[NAVN])
            Assertions.assertEquals(expected[FOEDSELSDATO], actual.familie.single()[FOEDSELSDATO])
        }
    }

    @Nested
    inner class DuplikaterTest {
        private val grunnlagshendelser =
            listOf(
                lagGrunnlagHendelse(
                    sakId = 1,
                    hendelseNummer = 1,
                    opplysningType = FOEDELAND,
                    id = statiskUuid,
                    fnr = testData.soeker.foedselsnummer,
                    verdi = "Norge".toJsonNode(),
                    kilde = kilde,
                ),
                lagGrunnlagHendelse(
                    sakId = 1,
                    hendelseNummer = 2,
                    opplysningType = FOEDELAND,
                    id = statiskUuid,
                    fnr = testData.soeker.foedselsnummer,
                    verdi = "Sverige".toJsonNode(),
                    kilde = kilde,
                ),
            )

        @Test
        fun `fjerner duplikater av samme opplysning for konstante opplysninger`() {
            every { opplysningDaoMock.hentAlleGrunnlagForSak(1) } returns grunnlagshendelser

            Assertions.assertEquals(
                1,
                grunnlagService.hentOpplysningsgrunnlagForSak(1)!!.soeker.values.size,
            )
        }

        @Test
        fun `tar alltid seneste versjon av samme opplysning`() {
            every { opplysningDaoMock.hentAlleGrunnlagForSak(1) } returns grunnlagshendelser
            val opplysningsgrunnlag = grunnlagService.hentOpplysningsgrunnlagForSak(1)!!

            Assertions.assertEquals(
                2,
                opplysningsgrunnlag.hentVersjon(),
            )
            Assertions.assertEquals(
                Opplysning.Konstant.create(grunnlagshendelser[1].opplysning).toJson(),
                opplysningsgrunnlag.soeker[FOEDELAND]!!.toJson(),
            )
        }
    }

    @Test
    fun `periodisert opplysning skal gi flere gjeldende opplysninger`() {
        val uuid1 = UUID.randomUUID()
        val bostedsadresse1 =
            ADRESSE_DEFAULT.first().copy(
                adresseLinje1 = "GammelAdresse 33",
                gyldigFraOgMed = LocalDateTime.of(2022, Month.JANUARY, 1, 0, 0),
                gyldigTilOgMed = LocalDateTime.of(2022, Month.JUNE, 1, 0, 0),
            )
        val bostedsadresse2 =
            ADRESSE_DEFAULT.first().copy(
                adresseLinje1 = "AktivAdresse 55",
                gyldigFraOgMed = LocalDateTime.of(2022, Month.JULY, 1, 0, 0),
                gyldigTilOgMed = LocalDateTime.of(2022, Month.DECEMBER, 1, 0, 0),
            )
        val grunnlagshendelser =
            listOf(
                OpplysningDao.GrunnlagHendelse(
                    opplysning =
                        Grunnlagsopplysning(
                            id = uuid1,
                            kilde = kilde,
                            opplysningType = BOSTEDSADRESSE,
                            meta = objectMapper.createObjectNode(),
                            opplysning = listOf(bostedsadresse1, bostedsadresse2).toJsonNode(),
                            attestering = null,
                            fnr = testData.soeker.foedselsnummer,
                        ),
                    sakId = 1,
                    hendelseNummer = 1,
                ),
            )

        every { opplysningDaoMock.hentAlleGrunnlagForSak(1) } returns grunnlagshendelser

        val actual = grunnlagService.hentOpplysningsgrunnlagForSak(1)!!
        val expected =
            Opplysning.Konstant(
                uuid1,
                kilde = kilde,
                verdi =
                    listOf(
                        bostedsadresse1,
                        bostedsadresse2,
                    ),
            )
        Assertions.assertEquals(expected, actual.soeker.hentBostedsadresse())
    }

    @Test
    fun `kan hente og mappe opplysningsgrunnlag`() {
        every { opplysningDaoMock.finnNyesteGrunnlagForSak(any<Long>(), PERSONGALLERI_V1) } returns
            lagGrunnlagHendelse(
                1,
                1,
                PERSONGALLERI_V1,
                id = statiskUuid,
                fnr = testData.soeker.foedselsnummer,
                verdi = testData.hentPersonGalleri().toJsonNode(),
                kilde = kilde,
            )

        every { opplysningDaoMock.hentAlleGrunnlagForSak(any<Long>()) } returns
            listOf(
                lagGrunnlagHendelse(
                    1,
                    2,
                    NAVN,
                    id = statiskUuid,
                    fnr = testData.soeker.foedselsnummer,
                    verdi = Navn("Per", "Kalle", "Persson").toJsonNode(),
                    kilde = kilde,
                ),
                lagGrunnlagHendelse(
                    1,
                    2,
                    PERSONROLLE,
                    id = statiskUuid,
                    fnr = testData.soeker.foedselsnummer,
                    verdi = PersonRolle.BARN.toJsonNode(),
                    kilde = kilde,
                ),
                lagGrunnlagHendelse(
                    1,
                    3,
                    PERSONGALLERI_V1,
                    id = statiskUuid,
                    verdi = testData.hentPersonGalleri().toJsonNode(),
                    kilde = kilde,
                ),
            )

        val opplysningsgrunnlag = grunnlagService.hentOpplysningsgrunnlagForSak(1)!!

        Assertions.assertEquals(1, opplysningsgrunnlag.sak.size)
        Assertions.assertEquals(2, opplysningsgrunnlag.soeker.size)
        Assertions.assertEquals(0, opplysningsgrunnlag.familie.size)
    }

    @Nested
    inner class `Test uthenting av saker og roller for person` {
        private val gjenlevendeFnr = GJENLEVENDE_FOEDSELSNUMMER

        private val barnepensjonSoeker1 = INNSENDER_FOEDSELSNUMMER
        private val grunnlaghendelse1 =
            lagGrunnlagHendelse(
                sakId = 1,
                hendelseNummer = 1,
                opplysningType = PERSONGALLERI_V1,
                verdi =
                    Persongalleri(
                        soeker = barnepensjonSoeker1.value,
                        innsender = gjenlevendeFnr.value,
                        soesken =
                            listOf(
                                SOEKER2_FOEDSELSNUMMER.value,
                                HELSOESKEN_FOEDSELSNUMMER.value,
                            ),
                        avdoed = listOf(AVDOED_FOEDSELSNUMMER.value),
                        gjenlevende = listOf(gjenlevendeFnr.value),
                    ).toJsonNode(),
            )

        private val barnepensjonSoeker2 = SOEKER2_FOEDSELSNUMMER
        private val grunnlaghendelse2 =
            lagGrunnlagHendelse(
                sakId = 2,
                hendelseNummer = 2,
                opplysningType = PERSONGALLERI_V1,
                verdi =
                    Persongalleri(
                        soeker = barnepensjonSoeker2.value,
                        innsender = gjenlevendeFnr.value,
                        soesken =
                            listOf(
                                INNSENDER_FOEDSELSNUMMER.value,
                                HELSOESKEN_FOEDSELSNUMMER.value,
                            ),
                        avdoed = listOf(AVDOED_FOEDSELSNUMMER.value),
                        gjenlevende = listOf(gjenlevendeFnr.value),
                    ).toJsonNode(),
            )

        private val grunnlaghendelse3 =
            lagGrunnlagHendelse(
                sakId = 3,
                hendelseNummer = 3,
                opplysningType = PERSONGALLERI_V1,
                verdi =
                    Persongalleri(
                        soeker = gjenlevendeFnr.value,
                        innsender = gjenlevendeFnr.value,
                        avdoed = listOf(AVDOED_FOEDSELSNUMMER.value),
                    ).toJsonNode(),
            )

        @Test
        fun `Hent sak og rolle for person hvor det finnes to soesken`() {
            every { opplysningDaoMock.finnAllePersongalleriHvorPersonFinnes(any()) } returns
                listOf(
                    grunnlaghendelse1,
                    grunnlaghendelse2,
                )

            /*
             * Barn 1 søker barnepensjon og har rolle søsken i annen sak
             */
            val barn1 = grunnlagService.hentSakerOgRoller(barnepensjonSoeker1)
            Assertions.assertEquals(2, barn1.sakerOgRoller.size)
            Assertions.assertEquals(barnepensjonSoeker1.value, barn1.fnr)

            val barn1ErSoekerIEgenSak = barn1.sakerOgRoller.single { it.rolle == Saksrolle.SOEKER }
            Assertions.assertEquals(grunnlaghendelse1.sakId, barn1ErSoekerIEgenSak.sakId)

            val barn1ErSoeskenIAnnenSak = barn1.sakerOgRoller.single { it.rolle == Saksrolle.SOESKEN }
            Assertions.assertEquals(grunnlaghendelse2.sakId, barn1ErSoeskenIAnnenSak.sakId)

            /*
             * Barn 2 søker barnepensjon og har rolle søsken i annen sak
             */
            val barn2 = grunnlagService.hentSakerOgRoller(barnepensjonSoeker2)
            Assertions.assertEquals(2, barn2.sakerOgRoller.size)
            Assertions.assertEquals(barnepensjonSoeker2.value, barn2.fnr)

            val barn2ErSoekerIEgenSak = barn2.sakerOgRoller.single { it.rolle == Saksrolle.SOEKER }
            Assertions.assertEquals(grunnlaghendelse2.sakId, barn2ErSoekerIEgenSak.sakId)

            val barn2ErSoeskenIAnnenSak = barn2.sakerOgRoller.single { it.rolle == Saksrolle.SOESKEN }
            Assertions.assertEquals(grunnlaghendelse1.sakId, barn2ErSoeskenIAnnenSak.sakId)

            verify(exactly = 1) { opplysningDaoMock.finnAllePersongalleriHvorPersonFinnes(barnepensjonSoeker1) }
            verify(exactly = 1) { opplysningDaoMock.finnAllePersongalleriHvorPersonFinnes(barnepensjonSoeker2) }
        }

        @Test
        fun `Hent sak og rolle for gjenlevende person med barn`() {
            every { opplysningDaoMock.finnAllePersongalleriHvorPersonFinnes(gjenlevendeFnr) } returns
                listOf(
                    grunnlaghendelse1,
                    grunnlaghendelse2,
                    grunnlaghendelse3,
                )

            val gjenlevende = grunnlagService.hentSakerOgRoller(gjenlevendeFnr)
            Assertions.assertEquals(3, gjenlevende.sakerOgRoller.size)
            Assertions.assertEquals(gjenlevendeFnr.value, gjenlevende.fnr)

            val gjenlevendeSomForelder = gjenlevende.sakerOgRoller.filter { it.rolle == Saksrolle.GJENLEVENDE }
            Assertions.assertEquals(2, gjenlevendeSomForelder.size)

            val gjenlevendeErSoeker = gjenlevende.sakerOgRoller.single { it.rolle == Saksrolle.SOEKER }
            Assertions.assertEquals(grunnlaghendelse3.sakId, gjenlevendeErSoeker.sakId)

            verify(exactly = 1) { opplysningDaoMock.finnAllePersongalleriHvorPersonFinnes(gjenlevendeFnr) }
        }

        @Test
        fun `Hent sak og rolle for person kun har tilknytning til saker hvor soesken soeker barnepensjon`() {
            val soeskenFnr = HELSOESKEN_FOEDSELSNUMMER

            every { opplysningDaoMock.finnAllePersongalleriHvorPersonFinnes(soeskenFnr) } returns
                listOf(
                    grunnlaghendelse1,
                    grunnlaghendelse2,
                )

            val soesken = grunnlagService.hentSakerOgRoller(soeskenFnr)
            Assertions.assertEquals(2, soesken.sakerOgRoller.size)
            assertTrue(soesken.sakerOgRoller.all { it.rolle == Saksrolle.SOESKEN })

            verify(exactly = 1) { opplysningDaoMock.finnAllePersongalleriHvorPersonFinnes(soeskenFnr) }
        }

        @Test
        fun `Kan mappe og hente innsender`() {
            every { opplysningDaoMock.finnNyesteGrunnlagForBehandling(any(), PERSONGALLERI_V1) } returns
                lagGrunnlagHendelse(
                    1,
                    1,
                    PERSONGALLERI_V1,
                    id = statiskUuid,
                    verdi = testData.hentPersonGalleri().toJsonNode(),
                    kilde = kilde,
                )

            every { opplysningDaoMock.hentAlleGrunnlagForSak(1) } returns
                listOf(
                    lagGrunnlagHendelse(
                        1,
                        2,
                        NAVN,
                        id = statiskUuid,
                        fnr = INNSENDER_FOEDSELSNUMMER,
                        verdi = Navn("Per", "Kalle", "Persson").toJsonNode(),
                        kilde = kilde,
                    ),
                    lagGrunnlagHendelse(
                        1,
                        2,
                        PERSONROLLE,
                        id = statiskUuid,
                        fnr = INNSENDER_FOEDSELSNUMMER,
                        verdi = PersonRolle.INNSENDER.toJsonNode(),
                        kilde = kilde,
                    ),
                    lagGrunnlagHendelse(
                        1,
                        3,
                        PERSONGALLERI_V1,
                        id = statiskUuid,
                        verdi = testData.hentPersonGalleri().toJsonNode(),
                        kilde = kilde,
                    ),
                )

            val opplysningsgrunnlag = grunnlagService.hentOpplysningsgrunnlagForSak(1)!!

            Assertions.assertEquals(1, opplysningsgrunnlag.sak.size)
            Assertions.assertEquals(1, opplysningsgrunnlag.familie.size)
            Assertions.assertEquals(2, opplysningsgrunnlag.hentInnsender().size)
        }
    }

    @Test
    fun `Kan oppdatere grunnlag`() {
        val sakId = 1L
        val behandlingId = UUID.randomUUID()
        val sakType = SakType.BARNEPENSJON

        every { opplysningDaoMock.finnNyesteGrunnlagForSak(any(), PERSONGALLERI_V1) } returns
            lagGrunnlagHendelse(
                sakId,
                1,
                PERSONGALLERI_V1,
                id = statiskUuid,
                verdi = testData.hentPersonGalleri().toJsonNode(),
                kilde = kilde,
            )

        every { opplysningDaoMock.hentAlleGrunnlagForSak(sakId) } returns
            listOf(
                lagGrunnlagHendelse(
                    sakId,
                    2,
                    NAVN,
                    id = statiskUuid,
                    fnr = INNSENDER_FOEDSELSNUMMER,
                    verdi = Navn("Per", "Kalle", "Persson").toJsonNode(),
                    kilde = kilde,
                ),
                lagGrunnlagHendelse(
                    sakId,
                    3,
                    PERSONGALLERI_V1,
                    id = statiskUuid,
                    verdi = testData.hentPersonGalleri().toJsonNode(),
                    kilde = kilde,
                ),
            )

        every { opplysningDaoMock.finnHendelserIGrunnlag(sakId) } returns
            listOf(
                lagGrunnlagHendelse(
                    sakId,
                    3,
                    PERSONGALLERI_V1,
                    id = statiskUuid,
                    verdi = testData.hentPersonGalleri().toJsonNode(),
                    kilde = kilde,
                ),
            )

        val opplysningsperson = mockPerson()
        every {
            opplysningDaoMock.hentBehandlingVersjon(behandlingId)
        } returns BehandlingGrunnlagVersjon(behandlingId, sakId, 12L, false)
        every {
            opplysningDaoMock.leggOpplysningTilGrunnlag(any(), any(), any())
        } returns 7L
        every {
            opplysningDaoMock.oppdaterVersjonForBehandling(any(), any(), any())
        } returns 1
        coEvery {
            grunnlagHenter.hentGrunnlagsdata(any())
        } returns sampleFetchedGrunnlag(opplysningsperson)

        runBlocking { grunnlagService.oppdaterGrunnlag(behandlingId, sakId, sakType) }

        val slot = mutableListOf<Grunnlagsopplysning<JsonNode>>()
        verify {
            opplysningDaoMock.leggOpplysningTilGrunnlag(sakId, capture(slot), opplysningsperson.foedselsnummer.verdi)
            opplysningDaoMock.oppdaterVersjonForBehandling(behandlingId, sakId, 7)
        }
        assertTrue(slot.filter { oppl -> oppl.opplysningType == FOEDSELSNUMMER }.size == 1)
        verify { pdlTjenesterKlientImpl wasNot called }
        verify { pdlTjenesterKlientImpl wasNot called }
        verify { persondataKlient wasNot called }
    }

    private fun sampleFetchedGrunnlag(opplysningsperson: PersonDTO) =
        HentetGrunnlag(
            personopplysninger =
                listOf(
                    Pair(
                        opplysningsperson.foedselsnummer.verdi,
                        listOf(
                            grunnlagsopplysning(
                                opplysningsperson.foedselsnummer.verdi,
                                FOEDSELSNUMMER,
                                opplysningsperson.foedselsnummer.verdi.toJsonNode(),
                            ),
                        ),
                    ),
                ),
            saksopplysninger =
                listOf(
                    grunnlagsopplysning(
                        null,
                        BOSTEDSADRESSE,
                        ADRESSE_DEFAULT.first().toJsonNode(),
                    ),
                ),
        )

    private fun grunnlagsopplysning(
        fnr: Folkeregisteridentifikator?,
        opplysningstype: Opplysningstype,
        opplysning: JsonNode,
    ) = Grunnlagsopplysning(
        id = UUID.randomUUID(),
        kilde = kilde,
        opplysningType = opplysningstype,
        meta = objectMapper.createObjectNode(),
        opplysning = opplysning,
        attestering = null,
        fnr,
        periode =
            Periode(
                fom = YearMonth.of(2022, 1),
                tom = YearMonth.of(2022, 12),
            ),
    )
}
