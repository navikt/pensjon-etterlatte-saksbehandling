package no.nav.etterlatte.grunnlag

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import lagGrunnlagHendelse
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.behandling.Saksrolle
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.Opplysning
import no.nav.etterlatte.libs.common.grunnlag.hentBostedsadresse
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Navn
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype.BOSTEDSADRESSE
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype.FOEDELAND
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype.FOEDSELSDATO
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype.NAVN
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype.PERSONGALLERI_V1
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype.PERSONROLLE
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.person.PersonRolle
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.libs.common.toJsonNode
import no.nav.etterlatte.libs.testdata.grunnlag.ADRESSE_DEFAULT
import no.nav.etterlatte.libs.testdata.grunnlag.GrunnlagTestData
import no.nav.etterlatte.libs.testdata.grunnlag.kilde
import no.nav.etterlatte.libs.testdata.grunnlag.statiskUuid
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Month
import java.util.*

internal class GrunnlagServiceTest {
    private val opplysningerMock = mockk<OpplysningDao>()
    private val grunnlagService = RealGrunnlagService(opplysningerMock, mockk())

    private val testData = GrunnlagTestData()

    @Nested
    inner class MapperTilRiktigKategoriTest {
        private val nyttNavn = Navn("Mohammed", "ali", "Ali")
        private val nyFødselsdag = LocalDate.of(2013, 12, 24)

        private fun lagGrunnlagForPerson(fnr: Folkeregisteridentifikator, personRolle: PersonRolle) = listOf(
            lagGrunnlagHendelse(
                1,
                1,
                NAVN,
                id = statiskUuid,
                fnr = fnr,
                verdi = nyttNavn.toJsonNode(),
                kilde = kilde
            ),
            lagGrunnlagHendelse(
                1,
                2,
                FOEDSELSDATO,
                id = statiskUuid,
                fnr = fnr,
                verdi = nyFødselsdag.toJsonNode(),
                kilde = kilde
            ),
            lagGrunnlagHendelse(
                1,
                3,
                PERSONROLLE,
                id = statiskUuid,
                fnr = fnr,
                verdi = personRolle.toJsonNode(),
                kilde = kilde
            ),
            lagGrunnlagHendelse(
                1,
                4,
                PERSONGALLERI_V1,
                id = statiskUuid,
                fnr = fnr,
                verdi = testData.hentPersonGalleri().toJsonNode(),
                kilde = kilde
            )
        )

        @Test
        fun `skal hente opptil versjon av grunnlag`() {
            val grunnlagshendelser = lagGrunnlagForPerson(testData.soeker.foedselsnummer, PersonRolle.BARN)

            every { opplysningerMock.finnGrunnlagOpptilVersjon(1, 4) } returns grunnlagshendelser

            val actual = grunnlagService.hentOpplysningsgrunnlagMedVersjon(1, 4)
            val expected = mapOf(
                NAVN to Opplysning.Konstant(statiskUuid, kilde, nyttNavn.toJsonNode()),
                FOEDSELSDATO to Opplysning.Konstant(statiskUuid, kilde, nyFødselsdag.toJsonNode())
            )

            Assertions.assertEquals(expected[NAVN], actual?.soeker?.get(NAVN))
            Assertions.assertEquals(expected[FOEDSELSDATO], actual?.soeker?.get(FOEDSELSDATO))
        }

        @Test
        fun `skal mappe om dataen fra DB til søker`() {
            val grunnlagshendelser = lagGrunnlagForPerson(testData.soeker.foedselsnummer, PersonRolle.BARN)

            every { opplysningerMock.hentAlleGrunnlagForSak(1) } returns grunnlagshendelser

            val actual = grunnlagService.hentOpplysningsgrunnlag(1, testData.hentPersonGalleri())
            val expected = mapOf(
                NAVN to Opplysning.Konstant(statiskUuid, kilde, nyttNavn.toJsonNode()),
                FOEDSELSDATO to Opplysning.Konstant(statiskUuid, kilde, nyFødselsdag.toJsonNode())
            )

            Assertions.assertEquals(expected[NAVN], actual.soeker[NAVN])
            Assertions.assertEquals(expected[FOEDSELSDATO], actual.soeker[FOEDSELSDATO])
        }

        @Test
        fun `skal mappe om dataen fra DB til avdød`() {
            val grunnlagshendelser = lagGrunnlagForPerson(testData.avdoed.foedselsnummer, PersonRolle.AVDOED)

            every { opplysningerMock.hentAlleGrunnlagForSak(1) } returns grunnlagshendelser

            val actual = grunnlagService.hentOpplysningsgrunnlag(1, testData.hentPersonGalleri())
            val expected = mapOf(
                NAVN to Opplysning.Konstant(statiskUuid, kilde, nyttNavn.toJsonNode()),
                FOEDSELSDATO to Opplysning.Konstant(statiskUuid, kilde, nyFødselsdag.toJsonNode()),
                PERSONROLLE to Opplysning.Konstant(statiskUuid, kilde, PersonRolle.AVDOED.toJsonNode())
            )

            Assertions.assertEquals(expected[NAVN], actual.hentAvdoed()[NAVN])
            Assertions.assertEquals(expected[FOEDSELSDATO], actual.hentAvdoed()[FOEDSELSDATO])
        }

        @Test
        fun `skal mappe om dataen fra DB til gjenlevende`() {
            val grunnlagshendelser = lagGrunnlagForPerson(testData.gjenlevende.foedselsnummer, PersonRolle.GJENLEVENDE)

            every { opplysningerMock.hentAlleGrunnlagForSak(1) } returns grunnlagshendelser

            val actual = grunnlagService.hentOpplysningsgrunnlag(1, testData.hentPersonGalleri())
            val expected = mapOf(
                NAVN to Opplysning.Konstant(statiskUuid, kilde, nyttNavn.toJsonNode()),
                FOEDSELSDATO to Opplysning.Konstant(statiskUuid, kilde, nyFødselsdag.toJsonNode()),
                PERSONROLLE to Opplysning.Konstant(statiskUuid, kilde, PersonRolle.GJENLEVENDE.toJsonNode())
            )

            Assertions.assertEquals(expected[NAVN], actual.hentGjenlevende()[NAVN])
            Assertions.assertEquals(expected[FOEDSELSDATO], actual.hentGjenlevende()[FOEDSELSDATO])
        }

        @Test
        fun `skal mappe om dataen fra DB til søsken`() {
            val grunnlagshendelser = lagGrunnlagForPerson(testData.soesken.foedselsnummer, PersonRolle.BARN)

            every { opplysningerMock.hentAlleGrunnlagForSak(1) } returns grunnlagshendelser

            val actual = grunnlagService.hentOpplysningsgrunnlag(1, testData.hentPersonGalleri())
            val expected = mapOf(
                NAVN to Opplysning.Konstant(statiskUuid, kilde, nyttNavn.toJsonNode()),
                FOEDSELSDATO to Opplysning.Konstant(statiskUuid, kilde, nyFødselsdag.toJsonNode()),
                PERSONROLLE to Opplysning.Konstant(statiskUuid, kilde, PersonRolle.BARN.toJsonNode())
            )

            Assertions.assertEquals(expected[NAVN], actual.familie.single()[NAVN])
            Assertions.assertEquals(expected[FOEDSELSDATO], actual.familie.single()[FOEDSELSDATO])
        }
    }

    @Nested
    inner class DuplikaterTest {
        private val grunnlagshendelser = listOf(
            lagGrunnlagHendelse(
                sakId = 1,
                hendelseNummer = 1,
                opplysningType = FOEDELAND,
                id = statiskUuid,
                fnr = testData.soeker.foedselsnummer,
                verdi = "Norge".toJsonNode(),
                kilde = kilde
            ),
            lagGrunnlagHendelse(
                sakId = 1,
                hendelseNummer = 2,
                opplysningType = FOEDELAND,
                id = statiskUuid,
                fnr = testData.soeker.foedselsnummer,
                verdi = "Sverige".toJsonNode(),
                kilde = kilde
            )
        )

        @Test
        fun `fjerner duplikater av samme opplysning for konstante opplysninger`() {
            every { opplysningerMock.hentAlleGrunnlagForSak(1) } returns grunnlagshendelser

            Assertions.assertEquals(
                1,
                grunnlagService.hentOpplysningsgrunnlag(1, testData.hentPersonGalleri()).soeker.values.size
            )
        }

        @Test
        fun `tar alltid seneste versjon av samme opplysning`() {
            every { opplysningerMock.hentAlleGrunnlagForSak(1) } returns grunnlagshendelser
            val opplysningsgrunnlag = grunnlagService.hentOpplysningsgrunnlag(1, testData.hentPersonGalleri())

            Assertions.assertEquals(
                2,
                opplysningsgrunnlag.hentVersjon()
            )
            Assertions.assertEquals(
                Opplysning.Konstant.create(grunnlagshendelser[1].opplysning).toJson(),
                opplysningsgrunnlag.soeker[FOEDELAND]!!.toJson()
            )
        }
    }

    @Test
    fun `periodisert opplysning skal gi flere gjeldende opplysninger`() {
        val uuid1 = UUID.randomUUID()
        val bostedsadresse1 = ADRESSE_DEFAULT.first().copy(
            adresseLinje1 = "GammelAdresse 33",
            gyldigFraOgMed = LocalDateTime.of(2022, Month.JANUARY, 1, 0, 0),
            gyldigTilOgMed = LocalDateTime.of(2022, Month.JUNE, 1, 0, 0)
        )
        val bostedsadresse2 = ADRESSE_DEFAULT.first().copy(
            adresseLinje1 = "AktivAdresse 55",
            gyldigFraOgMed = LocalDateTime.of(2022, Month.JULY, 1, 0, 0),
            gyldigTilOgMed = LocalDateTime.of(2022, Month.DECEMBER, 1, 0, 0)
        )
        val grunnlagshendelser = listOf(
            OpplysningDao.GrunnlagHendelse(
                opplysning = Grunnlagsopplysning(
                    id = uuid1,
                    kilde = kilde,
                    opplysningType = BOSTEDSADRESSE,
                    meta = objectMapper.createObjectNode(),
                    opplysning = listOf(bostedsadresse1, bostedsadresse2).toJsonNode(),
                    attestering = null,
                    fnr = testData.soeker.foedselsnummer
                ),
                sakId = 1,
                hendelseNummer = 1
            )
        )

        every { opplysningerMock.hentAlleGrunnlagForSak(1) } returns grunnlagshendelser

        val actual = grunnlagService.hentOpplysningsgrunnlag(1, testData.hentPersonGalleri())
        val expected = Opplysning.Konstant(
            uuid1,
            kilde = kilde,
            verdi = listOf(
                bostedsadresse1,
                bostedsadresse2
            )
        )
        Assertions.assertEquals(expected, actual.soeker.hentBostedsadresse())
    }

    @Test
    fun `kan hente og mappe opplysningsgrunnlag`() {
        every { opplysningerMock.finnNyesteGrunnlag(1, PERSONGALLERI_V1) } returns lagGrunnlagHendelse(
            1,
            1,
            PERSONGALLERI_V1,
            id = statiskUuid,
            fnr = testData.soeker.foedselsnummer,
            verdi = testData.hentPersonGalleri().toJsonNode(),
            kilde = kilde
        )

        every { opplysningerMock.hentAlleGrunnlagForSak(1) } returns listOf(
            lagGrunnlagHendelse(
                1,
                2,
                NAVN,
                id = statiskUuid,
                fnr = testData.soeker.foedselsnummer,
                verdi = Navn("Per", "Kalle", "Persson").toJsonNode(),
                kilde = kilde
            ),
            lagGrunnlagHendelse(
                1,
                2,
                PERSONROLLE,
                id = statiskUuid,
                fnr = testData.soeker.foedselsnummer,
                verdi = PersonRolle.BARN.toJsonNode(),
                kilde = kilde
            ),
            lagGrunnlagHendelse(
                1,
                3,
                PERSONGALLERI_V1,
                id = statiskUuid,
                verdi = testData.hentPersonGalleri().toJsonNode(),
                kilde = kilde
            )
        )

        val opplysningsgrunnlag = grunnlagService.hentOpplysningsgrunnlag(1)!!

        Assertions.assertEquals(1, opplysningsgrunnlag.sak.size)
        Assertions.assertEquals(2, opplysningsgrunnlag.soeker.size)
        Assertions.assertEquals(0, opplysningsgrunnlag.familie.size)
    }

    @Nested
    inner class `Test uthenting av saker og roller for person` {
        private val gjenlevendeFnr = TRIVIELL_MIDTPUNKT

        private val barnepensjonSoeker1 = BLAAOEYD_SAKS
        private val grunnlaghendelse1 = lagGrunnlagHendelse(
            sakId = 1,
            hendelseNummer = 1,
            opplysningType = PERSONGALLERI_V1,
            verdi = Persongalleri(
                soeker = barnepensjonSoeker1.value,
                innsender = gjenlevendeFnr.value,
                soesken = listOf(
                    GOEYAL_KRONJUVEL.value,
                    GROENN_STAUDE.value
                ),
                avdoed = listOf(STOR_SNERK.value),
                gjenlevende = listOf(gjenlevendeFnr.value)
            ).toJsonNode()
        )

        private val barnepensjonSoeker2 = GOEYAL_KRONJUVEL
        private val grunnlaghendelse2 = lagGrunnlagHendelse(
            sakId = 2,
            hendelseNummer = 2,
            opplysningType = PERSONGALLERI_V1,
            verdi = Persongalleri(
                soeker = barnepensjonSoeker2.value,
                innsender = gjenlevendeFnr.value,
                soesken = listOf(
                    BLAAOEYD_SAKS.value,
                    GROENN_STAUDE.value
                ),
                avdoed = listOf(STOR_SNERK.value),
                gjenlevende = listOf(gjenlevendeFnr.value)
            ).toJsonNode()
        )

        private val grunnlaghendelse3 = lagGrunnlagHendelse(
            sakId = 3,
            hendelseNummer = 3,
            opplysningType = PERSONGALLERI_V1,
            verdi = Persongalleri(
                soeker = gjenlevendeFnr.value,
                innsender = gjenlevendeFnr.value,
                avdoed = listOf(STOR_SNERK.value)
            ).toJsonNode()
        )

        @Test
        fun `Hent sak og rolle for person hvor det finnes to soesken`() {
            every { opplysningerMock.finnAllePersongalleriHvorPersonFinnes(any()) } returns listOf(
                grunnlaghendelse1,
                grunnlaghendelse2
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

            verify(exactly = 1) { opplysningerMock.finnAllePersongalleriHvorPersonFinnes(barnepensjonSoeker1) }
            verify(exactly = 1) { opplysningerMock.finnAllePersongalleriHvorPersonFinnes(barnepensjonSoeker2) }
        }

        @Test
        fun `Hent sak og rolle for gjenlevende person med barn`() {
            every { opplysningerMock.finnAllePersongalleriHvorPersonFinnes(gjenlevendeFnr) } returns listOf(
                grunnlaghendelse1,
                grunnlaghendelse2,
                grunnlaghendelse3
            )

            val gjenlevende = grunnlagService.hentSakerOgRoller(gjenlevendeFnr)
            Assertions.assertEquals(3, gjenlevende.sakerOgRoller.size)
            Assertions.assertEquals(gjenlevendeFnr.value, gjenlevende.fnr)

            val gjenlevendeSomForelder = gjenlevende.sakerOgRoller.filter { it.rolle == Saksrolle.GJENLEVENDE }
            Assertions.assertEquals(2, gjenlevendeSomForelder.size)

            val gjenlevendeErSoeker = gjenlevende.sakerOgRoller.single { it.rolle == Saksrolle.SOEKER }
            Assertions.assertEquals(grunnlaghendelse3.sakId, gjenlevendeErSoeker.sakId)

            verify(exactly = 1) { opplysningerMock.finnAllePersongalleriHvorPersonFinnes(gjenlevendeFnr) }
        }

        @Test
        fun `Hent sak og rolle for person kun har tilknytning til saker hvor soesken soeker barnepensjon`() {
            val soeskenFnr = GROENN_STAUDE

            every { opplysningerMock.finnAllePersongalleriHvorPersonFinnes(soeskenFnr) } returns listOf(
                grunnlaghendelse1,
                grunnlaghendelse2
            )

            val soesken = grunnlagService.hentSakerOgRoller(soeskenFnr)
            Assertions.assertEquals(2, soesken.sakerOgRoller.size)
            Assertions.assertTrue(soesken.sakerOgRoller.all { it.rolle == Saksrolle.SOESKEN })

            verify(exactly = 1) { opplysningerMock.finnAllePersongalleriHvorPersonFinnes(soeskenFnr) }
        }
    }

    companion object {
        val TRIVIELL_MIDTPUNKT = Folkeregisteridentifikator.of("19040550081")
        val STOR_SNERK = Folkeregisteridentifikator.of("11057523044")

        // barn
        val BLAAOEYD_SAKS = Folkeregisteridentifikator.of("05111850870")
        val GOEYAL_KRONJUVEL = Folkeregisteridentifikator.of("27121779531")
        val GROENN_STAUDE = Folkeregisteridentifikator.of("09011350027")
    }
}