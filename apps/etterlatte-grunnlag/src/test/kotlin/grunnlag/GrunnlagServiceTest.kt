package grunnlag

import GrunnlagTestData
import GrunnlagTestData.Companion.opplysningsgrunnlag
import GrunnlagTestData.Companion.søker
import io.mockk.every
import io.mockk.mockk
import lagGrunnlagsopplysning
import no.nav.etterlatte.grunnlag.OpplysningDao
import no.nav.etterlatte.grunnlag.RealGrunnlagService
import no.nav.etterlatte.libs.common.grunnlag.Opplysning
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Navn
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstyper.FOEDSELSDATO
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstyper.NAVN
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.libs.common.toJsonNode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class GrunnlagServiceTest {

    @Disabled // TODO sj: Fiks til å tilpasse ny uthenting
    @Test
    fun `hentOpplysningsgrunnlag skal mappe om dataen fra DB på rett struktur`() {
        val nyttNavn = Navn("Mohammed", "Ali")
        val nyFødselsdag = LocalDate.of(2013, 12, 24)

        val kilde1 = kilde
        val testData = GrunnlagTestData(
            opplysningsmapSøkerOverrides = mapOf(
                NAVN to Opplysning.Konstant(kilde1, nyttNavn.toJsonNode()),
                FOEDSELSDATO to Opplysning.Konstant(kilde1, nyFødselsdag.toJsonNode())
            )
        )
        val opplysningerMock = mockk<OpplysningDao>()
        val søker = testData.søker()

        val grunnlagshendelser = listOf(
            OpplysningDao.GrunnlagHendelse(
                lagGrunnlagsopplysning(
                    NAVN,
                    kilde1,
                    fnr = søker.foedselsnummer,
                    verdi = objectMapper.valueToTree(Navn(søker.fornavn, søker.etternavn))
                ),
                1,
                1
            ),
            OpplysningDao.GrunnlagHendelse(
                lagGrunnlagsopplysning(
                    FOEDSELSDATO,
                    kilde1,
                    fnr = søker.foedselsnummer,
                    verdi = objectMapper.valueToTree(søker.foedselsdato)
                ),
                1,
                2
            )
        )

        every { opplysningerMock.finnHendelserIGrunnlag(1) } returns grunnlagshendelser
        val service = RealGrunnlagService(opplysningerMock)

        val expected = testData.opplysningsgrunnlag()

        assertEquals(expected.toJson(), service.hentOpplysningsgrunnlag(1).toJson())
    }
}