package grunnlag

import io.mockk.every
import io.mockk.mockk
import lagGrunnlagsopplysning
import no.nav.etterlatte.grunnlag.OpplysningDao
import no.nav.etterlatte.grunnlag.RealGrunnlagService
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsdata
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.Metadata
import no.nav.etterlatte.libs.common.grunnlag.Opplysning
import no.nav.etterlatte.libs.common.grunnlag.Opplysningsgrunnlag
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Navn
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstyper
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.toJson
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import personTestData
import java.time.Instant

internal class GrunnlagServiceTest {
    private val instant = Instant.now()

    @Test
    fun `hentOpplysningsgrunnlag skal mappe om dataen fra DB på rett struktur`() {
        val opplysningerMock = mockk<OpplysningDao>()
        val søker = personTestData()
        val kilde1 = Grunnlagsopplysning.Pdl("PDL", instant, null, "opplysningsId1")
        val kilde2 = Grunnlagsopplysning.Pdl("PDL", instant, null, "opplysningsId2")

        val grunnlagshendelser = listOf(
            OpplysningDao.GrunnlagHendelse(
                lagGrunnlagsopplysning(
                    Opplysningstyper.NAVN,
                    kilde1,
                    fnr = søker.foedselsnummer,
                    verdi = objectMapper.valueToTree(Navn(søker.fornavn, søker.etternavn))
                ),
                1,
                1
            ),
            OpplysningDao.GrunnlagHendelse(
                lagGrunnlagsopplysning(
                    Opplysningstyper.FOEDSELSDATO,
                    kilde2,
                    fnr = søker.foedselsnummer,
                    verdi = objectMapper.valueToTree(søker.foedselsdato)
                ),
                1,
                2
            )
        )

        every { opplysningerMock.finnHendelserIGrunnlag(1) } returns grunnlagshendelser
        val service = RealGrunnlagService(opplysningerMock)

        val expected = Opplysningsgrunnlag(
            grunnlagsdata = Grunnlagsdata(
                søker = mapOf(
                    Opplysningstyper.NAVN to Opplysning.Konstant(
                        kilde = kilde1,
                        verdi = objectMapper.valueToTree(Navn(søker.fornavn, søker.etternavn))
                    ),
                    Opplysningstyper.FOEDSELSDATO to Opplysning.Konstant(
                        kilde = kilde2,
                        verdi = objectMapper.valueToTree(søker.foedselsdato)
                    )
                )
            ),
            metadata = Metadata(1, 2)
        )

        assertEquals(expected.toJson(), service.hentOpplysningsgrunnlag(1).toJson())
    }
}