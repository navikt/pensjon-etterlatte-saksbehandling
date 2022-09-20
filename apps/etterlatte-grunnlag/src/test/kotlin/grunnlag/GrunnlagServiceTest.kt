package grunnlag

import GrunnlagTestData
import io.mockk.every
import io.mockk.mockk
import lagGrunnlagHendelse
import no.nav.etterlatte.grunnlag.OpplysningDao
import no.nav.etterlatte.grunnlag.RealGrunnlagService
import no.nav.etterlatte.libs.common.grunnlag.Opplysning
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Navn
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstyper.FOEDSELSDATO
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstyper.NAVN
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstyper.PERSONROLLE
import no.nav.etterlatte.libs.common.person.Foedselsnummer
import no.nav.etterlatte.libs.common.person.PersonRolle
import no.nav.etterlatte.libs.common.toJsonNode
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class GrunnlagServiceTest {
    private val opplysningerMock = mockk<OpplysningDao>()
    private val grunnlagService = RealGrunnlagService(opplysningerMock)

    @Nested
    inner class MapperTilRiktigKategori {
        private val nyttNavn = Navn("Mohammed", "Ali")
        private val nyFødselsdag = LocalDate.of(2013, 12, 24)

        private val testData = GrunnlagTestData()

        private fun lagGrunnlagForPerson(fnr: Foedselsnummer, personRolle: PersonRolle) = listOf(
            lagGrunnlagHendelse(
                1,
                1,
                NAVN,
                fnr = fnr,
                verdi = nyttNavn.toJsonNode(),
                kilde = kilde
            ),
            lagGrunnlagHendelse(
                1,
                2,
                FOEDSELSDATO,
                fnr = fnr,
                verdi = nyFødselsdag.toJsonNode(),
                kilde = kilde
            ),
            lagGrunnlagHendelse(
                1,
                3,
                PERSONROLLE,
                fnr = fnr,
                verdi = personRolle.toJsonNode(),
                kilde = kilde
            )
        )

        @Test
        fun `hentOpplysningsgrunnlag skal mappe om dataen fra DB til søker`() {
            val grunnlagshendelser = lagGrunnlagForPerson(testData.søker.foedselsnummer, PersonRolle.BARN)

            every { opplysningerMock.hentAlleGrunnlagForSak(1) } returns grunnlagshendelser

            val actual = grunnlagService.hentOpplysningsgrunnlag(1, testData.hentPersonGalleri())
            val expected = mapOf(
                NAVN to Opplysning.Konstant(kilde, nyttNavn.toJsonNode()),
                FOEDSELSDATO to Opplysning.Konstant(kilde, nyFødselsdag.toJsonNode())
            )

            Assertions.assertEquals(expected[NAVN], actual.søker[NAVN])
            Assertions.assertEquals(expected[FOEDSELSDATO], actual.søker[FOEDSELSDATO])
        }

        @Test
        fun `hentOpplysningsgrunnlag skal mappe om dataen fra DB til avdød`() {
            val grunnlagshendelser = lagGrunnlagForPerson(testData.avdød.foedselsnummer, PersonRolle.AVDOED)

            every { opplysningerMock.hentAlleGrunnlagForSak(1) } returns grunnlagshendelser

            val actual = grunnlagService.hentOpplysningsgrunnlag(1, testData.hentPersonGalleri())
            val expected = mapOf(
                NAVN to Opplysning.Konstant(kilde, nyttNavn.toJsonNode()),
                FOEDSELSDATO to Opplysning.Konstant(kilde, nyFødselsdag.toJsonNode()),
                PERSONROLLE to Opplysning.Konstant(kilde, PersonRolle.AVDOED.toJsonNode())
            )

            Assertions.assertEquals(expected[NAVN], actual.hentAvdoed()[NAVN])
            Assertions.assertEquals(expected[FOEDSELSDATO], actual.hentAvdoed()[FOEDSELSDATO])
        }

        @Test
        fun `hentOpplysningsgrunnlag skal mappe om dataen fra DB til gjenlevende`() {
            val grunnlagshendelser = lagGrunnlagForPerson(testData.gjenlevende.foedselsnummer, PersonRolle.GJENLEVENDE)

            every { opplysningerMock.hentAlleGrunnlagForSak(1) } returns grunnlagshendelser

            val actual = grunnlagService.hentOpplysningsgrunnlag(1, testData.hentPersonGalleri())
            val expected = mapOf(
                NAVN to Opplysning.Konstant(kilde, nyttNavn.toJsonNode()),
                FOEDSELSDATO to Opplysning.Konstant(kilde, nyFødselsdag.toJsonNode()),
                PERSONROLLE to Opplysning.Konstant(kilde, PersonRolle.GJENLEVENDE.toJsonNode())
            )

            Assertions.assertEquals(expected[NAVN], actual.hentGjenlevende()[NAVN])
            Assertions.assertEquals(expected[FOEDSELSDATO], actual.hentGjenlevende()[FOEDSELSDATO])
        }

        @Test
        fun `hentOpplysningsgrunnlag skal mappe om dataen fra DB til søsken`() {
            val grunnlagshendelser = lagGrunnlagForPerson(testData.søsken.foedselsnummer, PersonRolle.BARN)

            every { opplysningerMock.hentAlleGrunnlagForSak(1) } returns grunnlagshendelser

            val actual = grunnlagService.hentOpplysningsgrunnlag(1, testData.hentPersonGalleri())
            val expected = mapOf(
                NAVN to Opplysning.Konstant(kilde, nyttNavn.toJsonNode()),
                FOEDSELSDATO to Opplysning.Konstant(kilde, nyFødselsdag.toJsonNode()),
                PERSONROLLE to Opplysning.Konstant(kilde, PersonRolle.BARN.toJsonNode())
            )

            Assertions.assertEquals(expected[NAVN], actual.familie.single()[NAVN])
            Assertions.assertEquals(expected[FOEDSELSDATO], actual.familie.single()[FOEDSELSDATO])
        }
    }
}