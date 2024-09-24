package no.nav.etterlatte.grunnlag

import com.fasterxml.jackson.databind.JsonNode
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.called
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import lagGrunnlagHendelse
import mockPerson
import no.nav.etterlatte.grunnlag.klienter.PdlTjenesterKlientImpl
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.behandling.Saksrolle
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.Opplysning
import no.nav.etterlatte.libs.common.grunnlag.Opplysningsbehov
import no.nav.etterlatte.libs.common.grunnlag.hentBostedsadresse
import no.nav.etterlatte.libs.common.grunnlag.hentFoedselsnummer
import no.nav.etterlatte.libs.common.grunnlag.hentKonstantOpplysning
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
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.libs.common.toJsonNode
import no.nav.etterlatte.libs.testdata.grunnlag.ADRESSE_DEFAULT
import no.nav.etterlatte.libs.testdata.grunnlag.AVDOED_FOEDSELSNUMMER
import no.nav.etterlatte.libs.testdata.grunnlag.GJENLEVENDE_FOEDSELSNUMMER
import no.nav.etterlatte.libs.testdata.grunnlag.GrunnlagTestData
import no.nav.etterlatte.libs.testdata.grunnlag.HELSOESKEN_FOEDSELSNUMMER
import no.nav.etterlatte.libs.testdata.grunnlag.INNSENDER_FOEDSELSNUMMER
import no.nav.etterlatte.libs.testdata.grunnlag.SOEKER2_FOEDSELSNUMMER
import no.nav.etterlatte.libs.testdata.grunnlag.SOEKER_FOEDSELSNUMMER
import no.nav.etterlatte.libs.testdata.grunnlag.kilde
import no.nav.etterlatte.libs.testdata.grunnlag.statiskUuid
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
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
    private val grunnlagHenter = mockk<GrunnlagHenter>()
    private val grunnlagService =
        RealGrunnlagService(
            pdltjenesterKlient = pdlTjenesterKlientImpl,
            opplysningDao = opplysningDaoMock,
            grunnlagHenter = grunnlagHenter,
        )

    private val testData = GrunnlagTestData()
    private val persongalleri =
        lagGrunnlagHendelse(
            sakId = 1,
            hendelseNummer = 4,
            opplysningType = PERSONGALLERI_V1,
            id = statiskUuid,
            fnr = null,
            verdi = testData.hentPersonGalleri().toJsonNode(),
            kilde = kilde,
        )

    @BeforeAll
    fun beforeAll() {
        every { opplysningDaoMock.finnNyesteGrunnlagForBehandling(any(), PERSONGALLERI_V1) } returns persongalleri
        every { opplysningDaoMock.finnNyesteGrunnlagForSak(any(), PERSONGALLERI_V1) } returns persongalleri
    }

    @Nested
    inner class MapperTilRiktigKategoriTest {
        private val nyttNavn = Navn("Mohammed", "ali", "Ali")
        private val nyFødselsdag = LocalDate.of(2013, 12, 24)

        private fun lagGrunnlagForPerson(
            fnr: Folkeregisteridentifikator,
            personRolle: PersonRolle,
        ): List<OpplysningDao.GrunnlagHendelse> {
            val persongalleri =
                when (personRolle) {
                    PersonRolle.INNSENDER ->
                        Persongalleri(
                            soeker = "",
                            innsender = fnr.value,
                        )

                    PersonRolle.BARN ->
                        Persongalleri(
                            soeker = fnr.value,
                            soesken = listOf(fnr.value),
                        )

                    PersonRolle.AVDOED ->
                        Persongalleri(
                            soeker = "",
                            avdoed = listOf(fnr.value),
                        )

                    PersonRolle.GJENLEVENDE ->
                        Persongalleri(
                            soeker = "",
                            gjenlevende = listOf(fnr.value),
                        )

                    PersonRolle.TILKNYTTET_BARN ->
                        Persongalleri(
                            soeker = "",
                            soesken = listOf(fnr.value),
                        )
                }
            return listOf(
                lagGrunnlagHendelse(
                    sakId = 1,
                    hendelseNummer = 1,
                    opplysningType = NAVN,
                    id = statiskUuid,
                    fnr = fnr,
                    verdi = nyttNavn.toJsonNode(),
                    kilde = kilde,
                ),
                lagGrunnlagHendelse(
                    sakId = 1,
                    hendelseNummer = 2,
                    opplysningType = FOEDSELSDATO,
                    id = statiskUuid,
                    fnr = fnr,
                    verdi = nyFødselsdag.toJsonNode(),
                    kilde = kilde,
                ),
                lagGrunnlagHendelse(
                    sakId = 1,
                    hendelseNummer = 3,
                    opplysningType = PERSONROLLE,
                    id = statiskUuid,
                    fnr = fnr,
                    verdi = personRolle.toJsonNode(),
                    kilde = kilde,
                ),
                lagGrunnlagHendelse(
                    sakId = 1,
                    hendelseNummer = 5,
                    opplysningType = FOEDSELSNUMMER,
                    id = statiskUuid,
                    fnr = fnr,
                    verdi = fnr.toJsonNode(),
                    kilde = kilde,
                ),
                lagGrunnlagHendelse(
                    sakId = 1,
                    hendelseNummer = 4,
                    opplysningType = PERSONGALLERI_V1,
                    id = statiskUuid,
                    fnr = null,
                    verdi = persongalleri.toJsonNode(),
                    kilde = kilde,
                ),
            )
        }

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

            assertEquals(expected[NAVN], actual.soeker[NAVN])
            assertEquals(expected[FOEDSELSDATO], actual.soeker[FOEDSELSDATO])
        }

        @Test
        fun `Skal lagre grunnlaget for kun sak`() {
            val galleri =
                Persongalleri(
                    testData.soeker.foedselsnummer.value,
                    testData.soeker.foedselsnummer.value,
                    emptyList(),
                    listOf(
                        testData.avdoede
                            .first()
                            .foedselsnummer.value,
                    ),
                    listOf(testData.gjenlevende.foedselsnummer.value),
                )
            val sakId = 1L
            val grunnlagshendelser =
                listOf(
                    OpplysningDao.GrunnlagHendelse(
                        opplysning =
                            Grunnlagsopplysning(
                                id = UUID.randomUUID(),
                                kilde = kilde,
                                opplysningType = PERSONGALLERI_V1,
                                meta = objectMapper.createObjectNode(),
                                opplysning = galleri.toJsonNode(),
                                attestering = null,
                                fnr = null,
                            ),
                        sakId = sakId,
                        hendelseNummer = 1,
                    ),
                )

            val opplysningsperson = mockPerson()
            coEvery {
                grunnlagHenter.hentGrunnlagsdata(any())
            } returns sampleFetchedGrunnlag(opplysningsperson)
            every { opplysningDaoMock.finnHendelserIGrunnlag(sakId) } returns emptyList()
            every { opplysningDaoMock.leggOpplysningTilGrunnlag(any(), any(), any()) } returns sakId

            val opplysningsbehov = Opplysningsbehov(sakId, SakType.BARNEPENSJON, galleri)

            runBlocking { grunnlagService.opprettEllerOppdaterGrunnlagForSak(sakId, opplysningsbehov) }

            every { opplysningDaoMock.hentAlleGrunnlagForSak(sakId) } returns grunnlagshendelser
            every { opplysningDaoMock.finnNyesteGrunnlagForSak(sakId, PERSONGALLERI_V1) } returns grunnlagshendelser.first()

            val grunnlag = grunnlagService.hentOpplysningsgrunnlagForSak(sakId)
            assertNotNull(grunnlag)
            if (grunnlag != null) {
                val hentetGalleriPaaSoeker =
                    grunnlag.sak
                        .hentKonstantOpplysning<Persongalleri>(
                            PERSONGALLERI_V1,
                        )?.verdi ?: throw RuntimeException("failure")
                assertEquals(testData.soeker.foedselsnummer.value, hentetGalleriPaaSoeker.soeker)
                assertEquals(testData.soeker.foedselsnummer.value, hentetGalleriPaaSoeker.innsender)
                assertEquals(
                    testData.avdoede
                        .first()
                        .foedselsnummer.value,
                    hentetGalleriPaaSoeker.avdoed.first(),
                )
                assertEquals(testData.gjenlevende.foedselsnummer.value, hentetGalleriPaaSoeker.gjenlevende.first())
            }
        }

        @Test
        fun `skal mappe om dataen fra DB til avdød`() {
            val grunnlagshendelser = lagGrunnlagForPerson(testData.avdoede.first().foedselsnummer, PersonRolle.AVDOED)

            every { opplysningDaoMock.hentAlleGrunnlagForSak(1) } returns grunnlagshendelser

            val actual = grunnlagService.hentOpplysningsgrunnlagForSak(1)!!
            val expected =
                mapOf(
                    NAVN to Opplysning.Konstant(statiskUuid, kilde, nyttNavn.toJsonNode()),
                    FOEDSELSDATO to Opplysning.Konstant(statiskUuid, kilde, nyFødselsdag.toJsonNode()),
                    PERSONROLLE to Opplysning.Konstant(statiskUuid, kilde, PersonRolle.AVDOED.toJsonNode()),
                    FOEDSELSNUMMER to Opplysning.Konstant(statiskUuid, kilde, testData.avdoede.first().foedselsnummer),
                )

            assertEquals(expected[NAVN], actual.hentAvdoede()[0][NAVN])
            assertEquals(expected[FOEDSELSDATO], actual.hentAvdoede()[0][FOEDSELSDATO])
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

            assertEquals(expected[NAVN], actual.hentPotensiellGjenlevende()?.get(NAVN))
            assertEquals(expected[FOEDSELSDATO], actual.hentPotensiellGjenlevende()!![FOEDSELSDATO])
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

            assertEquals(expected[NAVN], actual.familie.single()[NAVN])
            assertEquals(expected[FOEDSELSDATO], actual.familie.single()[FOEDSELSDATO])
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

            assertEquals(
                1,
                grunnlagService
                    .hentOpplysningsgrunnlagForSak(1)!!
                    .soeker.values.size,
            )
        }

        @Test
        fun `tar alltid seneste versjon av samme opplysning`() {
            every { opplysningDaoMock.hentAlleGrunnlagForSak(1) } returns grunnlagshendelser
            val opplysningsgrunnlag = grunnlagService.hentOpplysningsgrunnlagForSak(1)!!

            assertEquals(
                2,
                opplysningsgrunnlag.hentVersjon(),
            )
            assertEquals(
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
        assertEquals(expected, actual.soeker.hentBostedsadresse())
    }

    @Test
    fun `kan hente og mappe opplysningsgrunnlag`() {
        every { opplysningDaoMock.finnNyesteGrunnlagForSak(any<SakId>(), PERSONGALLERI_V1) } returns
            lagGrunnlagHendelse(
                1,
                1,
                PERSONGALLERI_V1,
                id = statiskUuid,
                fnr = testData.soeker.foedselsnummer,
                verdi = testData.hentPersonGalleri().toJsonNode(),
                kilde = kilde,
            )

        every { opplysningDaoMock.hentAlleGrunnlagForSak(any<SakId>()) } returns
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

        assertEquals(1, opplysningsgrunnlag.sak.size)
        assertEquals(2, opplysningsgrunnlag.soeker.size)
        assertEquals(0, opplysningsgrunnlag.familie.size)
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
            assertEquals(2, barn1.sakiderOgRoller.size)
            assertEquals(barnepensjonSoeker1.value, barn1.fnr)

            val barn1ErSoekerIEgenSak = barn1.sakiderOgRoller.single { it.rolle == Saksrolle.SOEKER }
            assertEquals(grunnlaghendelse1.sakId, barn1ErSoekerIEgenSak.sakId)

            val barn1ErSoeskenIAnnenSak = barn1.sakiderOgRoller.single { it.rolle == Saksrolle.SOESKEN }
            assertEquals(grunnlaghendelse2.sakId, barn1ErSoeskenIAnnenSak.sakId)

            /*
             * Barn 2 søker barnepensjon og har rolle søsken i annen sak
             */
            val barn2 = grunnlagService.hentSakerOgRoller(barnepensjonSoeker2)
            assertEquals(2, barn2.sakiderOgRoller.size)
            assertEquals(barnepensjonSoeker2.value, barn2.fnr)

            val barn2ErSoekerIEgenSak = barn2.sakiderOgRoller.single { it.rolle == Saksrolle.SOEKER }
            assertEquals(grunnlaghendelse2.sakId, barn2ErSoekerIEgenSak.sakId)

            val barn2ErSoeskenIAnnenSak = barn2.sakiderOgRoller.single { it.rolle == Saksrolle.SOESKEN }
            assertEquals(grunnlaghendelse1.sakId, barn2ErSoeskenIAnnenSak.sakId)

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
            assertEquals(3, gjenlevende.sakiderOgRoller.size)
            assertEquals(gjenlevendeFnr.value, gjenlevende.fnr)

            val gjenlevendeSomForelder = gjenlevende.sakiderOgRoller.filter { it.rolle == Saksrolle.GJENLEVENDE }
            assertEquals(2, gjenlevendeSomForelder.size)

            val gjenlevendeErSoeker = gjenlevende.sakiderOgRoller.single { it.rolle == Saksrolle.SOEKER }
            assertEquals(grunnlaghendelse3.sakId, gjenlevendeErSoeker.sakId)

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
            assertEquals(2, soesken.sakiderOgRoller.size)
            assertTrue(soesken.sakiderOgRoller.all { it.rolle == Saksrolle.SOESKEN })

            verify(exactly = 1) { opplysningDaoMock.finnAllePersongalleriHvorPersonFinnes(soeskenFnr) }
        }

        @Test
        fun `Kan mappe og hente innsender`() {
            val persongalleri =
                lagGrunnlagHendelse(
                    1,
                    1,
                    PERSONGALLERI_V1,
                    id = statiskUuid,
                    verdi =
                        testData
                            .hentPersonGalleri()
                            .copy(innsender = INNSENDER_FOEDSELSNUMMER.value)
                            .toJsonNode(),
                    kilde = kilde,
                )
            every { opplysningDaoMock.finnNyesteGrunnlagForBehandling(any(), PERSONGALLERI_V1) } returns persongalleri
            every { opplysningDaoMock.finnNyesteGrunnlagForSak(any(), PERSONGALLERI_V1) } returns persongalleri
            every { opplysningDaoMock.hentAlleGrunnlagForSak(1) } returns
                listOf(
                    lagGrunnlagHendelse(
                        1,
                        2,
                        FOEDSELSNUMMER,
                        id = statiskUuid,
                        fnr = INNSENDER_FOEDSELSNUMMER,
                        verdi = INNSENDER_FOEDSELSNUMMER.toJsonNode(),
                        kilde = kilde,
                    ),
                    lagGrunnlagHendelse(
                        1,
                        3,
                        NAVN,
                        id = statiskUuid,
                        fnr = INNSENDER_FOEDSELSNUMMER,
                        verdi = Navn("Per", "Kalle", "Persson").toJsonNode(),
                        kilde = kilde,
                    ),
                    lagGrunnlagHendelse(
                        1,
                        4,
                        PERSONROLLE,
                        id = statiskUuid,
                        fnr = INNSENDER_FOEDSELSNUMMER,
                        verdi = PersonRolle.INNSENDER.toJsonNode(),
                        kilde = kilde,
                    ),
                    persongalleri,
                )

            val opplysningsgrunnlag = grunnlagService.hentOpplysningsgrunnlagForSak(1)!!

            assertEquals(1, opplysningsgrunnlag.sak.size)
            assertEquals(1, opplysningsgrunnlag.familie.size)
            assertEquals(INNSENDER_FOEDSELSNUMMER, opplysningsgrunnlag.hentInnsender().hentFoedselsnummer()?.verdi)
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
    }

    @Nested
    inner class `Test av samsvar persongalleri` {
        @Test
        fun `samsvar bryr seg ikke om rekkefølge på personer`() {
            val persongalleriSak =
                Persongalleri(
                    soeker = "søker",
                    soesken = listOf("1", "2", "3"),
                )
            val persongalleriPdl =
                Persongalleri(
                    soeker = "søker",
                    soesken = listOf("3", "1", "2"),
                )
            val valideringsfeil = grunnlagService.valideringsfeilPersongalleriSakPdl(persongalleriSak, persongalleriPdl)
            assertEquals(emptyList<MismatchPersongalleri>(), valideringsfeil)
        }

        @Test
        fun `samsvar registerer feil hvis vi mangler avdøde`() {
            val persongalleriEkstraAvdoed =
                Persongalleri(
                    soeker = "",
                    avdoed = listOf("1", "2"),
                )
            val persongalleriEnAvdoed =
                Persongalleri(
                    soeker = "",
                    avdoed = listOf("1"),
                )
            val valideringsfeilPdlEkstraAvdoed =
                grunnlagService.valideringsfeilPersongalleriSakPdl(
                    persongalleriEnAvdoed,
                    persongalleriEkstraAvdoed,
                )
            val valideringsfeilSakEkstraAvdoed =
                grunnlagService.valideringsfeilPersongalleriSakPdl(
                    persongalleriEkstraAvdoed,
                    persongalleriEnAvdoed,
                )
            assertEquals(listOf(MismatchPersongalleri.EKSTRA_AVDOED), valideringsfeilSakEkstraAvdoed)
            assertEquals(listOf(MismatchPersongalleri.MANGLER_AVDOED), valideringsfeilPdlEkstraAvdoed)
        }

        @Test
        fun `samsvar registerer feil hvis vi mangler gjenlevende`() {
            val persongalleriEkstraGjenlevende =
                Persongalleri(
                    soeker = "",
                    gjenlevende = listOf("1"),
                )
            val persongalleriIngenGjenlevende =
                Persongalleri(
                    soeker = "",
                    gjenlevende = emptyList(),
                )
            val valideringsfeilPdlEkstraGjenlevende =
                grunnlagService.valideringsfeilPersongalleriSakPdl(
                    persongalleriIngenGjenlevende,
                    persongalleriEkstraGjenlevende,
                )
            val valideringsfeilSakEkstraGjenlevende =
                grunnlagService.valideringsfeilPersongalleriSakPdl(
                    persongalleriEkstraGjenlevende,
                    persongalleriIngenGjenlevende,
                )
            assertEquals(listOf(MismatchPersongalleri.EKSTRA_GJENLEVENDE), valideringsfeilSakEkstraGjenlevende)
            assertEquals(listOf(MismatchPersongalleri.MANGLER_GJENLEVENDE), valideringsfeilPdlEkstraGjenlevende)
        }

        @Test
        fun `samsvar registerer feil hvis vi mangler søsken`() {
            val persongalleriEkstraSoesken =
                Persongalleri(
                    soeker = "",
                    soesken = listOf("1", "2"),
                )
            val persongalleriEtSoesken =
                Persongalleri(
                    soeker = "",
                    soesken = listOf("1"),
                )
            val valideringsfeilPdlEkstraSoesken =
                grunnlagService.valideringsfeilPersongalleriSakPdl(
                    persongalleriEtSoesken,
                    persongalleriEkstraSoesken,
                )
            val valideringsfeilSakEkstraSoesken =
                grunnlagService.valideringsfeilPersongalleriSakPdl(
                    persongalleriEkstraSoesken,
                    persongalleriEtSoesken,
                )
            assertEquals(listOf(MismatchPersongalleri.EKSTRA_SOESKEN), valideringsfeilSakEkstraSoesken)
            assertEquals(listOf(MismatchPersongalleri.MANGLER_SOESKEN), valideringsfeilPdlEkstraSoesken)
        }
    }

    @Nested
    inner class Personopplysninger {
        @Test
        fun `skal hente personopplysninger, seneste gyldige roller`() {
            val behandlingsid = UUID.randomUUID()
            every {
                opplysningDaoMock.hentGrunnlagAvTypeForBehandling(
                    behandlingsid,
                    PERSONGALLERI_V1,
                    Opplysningstype.INNSENDER_PDL_V1,
                    Opplysningstype.SOEKER_PDL_V1,
                    Opplysningstype.AVDOED_PDL_V1,
                    Opplysningstype.GJENLEVENDE_FORELDER_PDL_V1,
                )
            } returns
                listOf(
                    lagGrunnlagHendelse(
                        sakId = 1,
                        hendelseNummer = 1,
                        opplysningType = Opplysningstype.SOEKER_PDL_V1,
                        id = behandlingsid,
                        fnr = SOEKER_FOEDSELSNUMMER,
                        verdi = testData.soeker.toJsonNode(),
                        kilde = kilde,
                    ),
                    lagGrunnlagHendelse(
                        sakId = 1,
                        hendelseNummer = 2,
                        opplysningType = Opplysningstype.INNSENDER_PDL_V1,
                        id = behandlingsid,
                        fnr = SOEKER_FOEDSELSNUMMER,
                        verdi = testData.soeker.toJsonNode(),
                        kilde = kilde,
                    ),
                    lagGrunnlagHendelse(
                        sakId = 1,
                        hendelseNummer = 3,
                        opplysningType = Opplysningstype.AVDOED_PDL_V1,
                        id = behandlingsid,
                        fnr = AVDOED_FOEDSELSNUMMER,
                        verdi = testData.avdoede.first().toJsonNode(),
                        kilde = kilde,
                    ),
                    lagGrunnlagHendelse(
                        sakId = 1,
                        hendelseNummer = 4,
                        opplysningType = Opplysningstype.AVDOED_PDL_V1,
                        id = behandlingsid,
                        fnr = GJENLEVENDE_FOEDSELSNUMMER,
                        verdi = testData.gjenlevende.toJsonNode(),
                        kilde = kilde,
                    ),
                    lagGrunnlagHendelse(
                        sakId = 1,
                        hendelseNummer = 5,
                        opplysningType = PERSONGALLERI_V1,
                        id = behandlingsid,
                        verdi =
                            Persongalleri(
                                innsender = SOEKER_FOEDSELSNUMMER.value,
                                soeker = SOEKER_FOEDSELSNUMMER.value,
                                avdoed = listOf(AVDOED_FOEDSELSNUMMER.value),
                            ).toJsonNode(),
                        kilde = kilde,
                    ),
                )

            val resultat = grunnlagService.hentPersonopplysninger(behandlingsid, SakType.BARNEPENSJON)

            resultat.innsender?.opplysning?.foedselsnummer shouldBe SOEKER_FOEDSELSNUMMER
            resultat.soeker?.opplysning?.foedselsnummer shouldBe SOEKER_FOEDSELSNUMMER
            resultat.avdoede shouldHaveSize 1
            resultat.avdoede.map { it.opplysning.foedselsnummer } shouldContainExactlyInAnyOrder
                listOf(
                    Folkeregisteridentifikator.of(AVDOED_FOEDSELSNUMMER.value),
                )
            resultat.gjenlevende shouldHaveSize 0
        }

        @Test
        fun `skal håndtere at samme person har flere roller i persongalleriet`() {
            val behandlingsid = UUID.randomUUID()
            every {
                opplysningDaoMock.hentGrunnlagAvTypeForBehandling(
                    behandlingsid,
                    PERSONGALLERI_V1,
                    Opplysningstype.INNSENDER_PDL_V1,
                    Opplysningstype.SOEKER_PDL_V1,
                    Opplysningstype.AVDOED_PDL_V1,
                    Opplysningstype.GJENLEVENDE_FORELDER_PDL_V1,
                )
            } returns
                listOf(
                    lagGrunnlagHendelse(
                        1,
                        1,
                        Opplysningstype.SOEKER_PDL_V1,
                        id = behandlingsid,
                        fnr = GJENLEVENDE_FOEDSELSNUMMER,
                        verdi = testData.gjenlevende.toJsonNode(),
                        kilde = kilde,
                    ),
                    lagGrunnlagHendelse(
                        1,
                        2,
                        Opplysningstype.INNSENDER_PDL_V1,
                        id = behandlingsid,
                        fnr = GJENLEVENDE_FOEDSELSNUMMER,
                        verdi = testData.gjenlevende.toJsonNode(),
                        kilde = kilde,
                    ),
                    lagGrunnlagHendelse(
                        1,
                        3,
                        Opplysningstype.AVDOED_PDL_V1,
                        id = behandlingsid,
                        fnr = AVDOED_FOEDSELSNUMMER,
                        verdi = testData.avdoede.first().toJsonNode(),
                        kilde = kilde,
                    ),
                    lagGrunnlagHendelse(
                        1,
                        4,
                        Opplysningstype.GJENLEVENDE_FORELDER_PDL_V1,
                        id = behandlingsid,
                        fnr = GJENLEVENDE_FOEDSELSNUMMER,
                        verdi = testData.gjenlevende.toJsonNode(),
                        kilde = kilde,
                    ),
                    lagGrunnlagHendelse(
                        1,
                        5,
                        Opplysningstype.PERSONGALLERI_V1,
                        id = behandlingsid,
                        verdi =
                            Persongalleri(
                                innsender = GJENLEVENDE_FOEDSELSNUMMER.value,
                                soeker = GJENLEVENDE_FOEDSELSNUMMER.value,
                                avdoed = listOf(AVDOED_FOEDSELSNUMMER.value),
                                gjenlevende = listOf(GJENLEVENDE_FOEDSELSNUMMER.value),
                            ).toJsonNode(),
                        kilde = kilde,
                    ),
                )
            val resultat = grunnlagService.hentPersonopplysninger(behandlingsid, SakType.BARNEPENSJON)

            resultat.innsender?.opplysning?.foedselsnummer shouldBe GJENLEVENDE_FOEDSELSNUMMER
            resultat.soeker?.opplysning?.foedselsnummer shouldBe GJENLEVENDE_FOEDSELSNUMMER
            resultat.avdoede shouldHaveSize 1
            resultat.avdoede.map { it.opplysning.foedselsnummer } shouldContainExactlyInAnyOrder
                listOf(
                    Folkeregisteridentifikator.of(AVDOED_FOEDSELSNUMMER.value),
                )
            resultat.gjenlevende shouldHaveSize 1
            resultat.gjenlevende.map { it.opplysning.foedselsnummer } shouldContainExactlyInAnyOrder
                listOf(
                    GJENLEVENDE_FOEDSELSNUMMER,
                )
        }
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
        fnr = fnr,
        periode =
            Periode(
                fom = YearMonth.of(2022, 1),
                tom = YearMonth.of(2022, 12),
            ),
    )
}
