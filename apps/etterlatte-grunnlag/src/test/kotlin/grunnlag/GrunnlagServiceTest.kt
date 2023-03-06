package no.nav.etterlatte.grunnlag

import io.mockk.every
import io.mockk.mockk
import lagGrunnlagHendelse
import no.nav.etterlatte.klienter.BehandlingKlient
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.Opplysning
import no.nav.etterlatte.libs.common.grunnlag.PeriodisertOpplysning
import no.nav.etterlatte.libs.common.grunnlag.hentBostedsadresse
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Navn
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype.BOSTEDSADRESSE
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype.FOEDELAND
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype.FOEDSELSDATO
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype.NAVN
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype.PERSONGALLERI_V1
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype.PERSONROLLE
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.periode.Periode
import no.nav.etterlatte.libs.common.person.Foedselsnummer
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
import java.time.YearMonth
import java.util.UUID

internal class GrunnlagServiceTest {
    private val opplysningerMock = mockk<OpplysningDao>()
    private val behandlingKlient = mockk<BehandlingKlient>()
    private val grunnlagService = RealGrunnlagService(opplysningerMock, behandlingKlient, mockk())

    private val testData = GrunnlagTestData()

    @Nested
    inner class MapperTilRiktigKategoriTest {
        private val nyttNavn = Navn("Mohammed", "Ali")
        private val nyFødselsdag = LocalDate.of(2013, 12, 24)

        private fun lagGrunnlagForPerson(fnr: Foedselsnummer, personRolle: PersonRolle) = listOf(
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
        val uuid2 = UUID.randomUUID()
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
                    opplysning = bostedsadresse1.toJsonNode(),
                    attestering = null,
                    fnr = testData.soeker.foedselsnummer,
                    periode = Periode(
                        fom = bostedsadresse1.gyldigFraOgMed!!.let { YearMonth.of(it.year, it.month) },
                        tom = bostedsadresse1.gyldigTilOgMed?.let { YearMonth.of(it.year, it.month) }
                    )
                ),
                sakId = 1,
                hendelseNummer = 1
            ),
            OpplysningDao.GrunnlagHendelse(
                opplysning = Grunnlagsopplysning(
                    id = uuid2,
                    kilde = kilde,
                    opplysningType = BOSTEDSADRESSE,
                    meta = objectMapper.createObjectNode(),
                    opplysning = bostedsadresse2.toJsonNode(),
                    attestering = null,
                    fnr = testData.soeker.foedselsnummer,
                    periode = Periode(
                        fom = bostedsadresse2.gyldigFraOgMed!!.let { YearMonth.of(it.year, it.month) },
                        tom = bostedsadresse2.gyldigTilOgMed?.let { YearMonth.of(it.year, it.month) }
                    )
                ),
                sakId = 1,
                hendelseNummer = 2
            )
        )

        every { opplysningerMock.hentAlleGrunnlagForSak(1) } returns grunnlagshendelser

        val actual = grunnlagService.hentOpplysningsgrunnlag(1, testData.hentPersonGalleri())
        val expected = Opplysning.Periodisert(
            listOf(
                PeriodisertOpplysning(
                    uuid1,
                    kilde = kilde,
                    verdi = bostedsadresse1,
                    fom = YearMonth.of(2022, Month.JANUARY),
                    tom = YearMonth.of(2022, Month.JUNE)
                ),
                PeriodisertOpplysning(
                    uuid2,
                    kilde = kilde,
                    verdi = bostedsadresse2,
                    fom = YearMonth.of(2022, Month.JULY),
                    tom = YearMonth.of(2022, Month.DECEMBER)
                )
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
                verdi = Navn("Per", "Persson").toJsonNode(),
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
}