package grunnlag

import io.mockk.every
import io.mockk.mockk
import lagGrunnlagsopplysning
import no.nav.etterlatte.grunnlag.GrunnlagHendelser
import no.nav.etterlatte.grunnlag.OpplysningDao
import no.nav.etterlatte.grunnlag.RealGrunnlagService
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstyper
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstyper.NAVN
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.person.Foedselsnummer
import no.nav.etterlatte.libs.common.rapidsandrivers.eventNameKey
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import soeknad.soekerBarnSoeknad
import java.time.Instant

class GrunnlagHendelserTest {
    private val opplysningerMock = mockk<OpplysningDao>()
    private val inspector = TestRapid().apply { GrunnlagHendelser(this, RealGrunnlagService(opplysningerMock)) }
    private val melding = JsonMessage.newMessage(
        mapOf(
            "@behov" to Opplysningstyper.SOEKER_PDL_V1,
            "sakId" to 1,
            "fnr" to "\"11057523044\"",
            "opplysning" to listOf(lagGrunnlagsopplysning(NAVN, verdi = objectMapper.readTree("\"Ola\"")))
        )
    )

    @Test
    fun `skal lese opplysningsbehov og legge til opplysning`() {
        val grunnlagshendelser = listOf(
            OpplysningDao.GrunnlagHendelse(
                lagGrunnlagsopplysning(
                    NAVN,
                    Grunnlagsopplysning.Saksbehandler("S01", Instant.now()),
                    fnr = Foedselsnummer.of("11057523044"),
                    verdi = objectMapper.valueToTree(soekerBarnSoeknad().fornavn)
                ),
                1,
                1
            )
        )

        every { opplysningerMock.finnHendelserIGrunnlag(any()) } returns grunnlagshendelser
        every { opplysningerMock.leggOpplysningTilGrunnlag(any(), any(), any()) } returns 1L
        val inspector = inspector.apply { sendTestMessage(melding.toJson()) }.inspektør

        Assertions.assertEquals("GRUNNLAG:GRUNNLAGENDRET", inspector.message(0).get(eventNameKey).asText())
        Assertions.assertEquals(1, inspector.message(0).get("grunnlag").get("grunnlag").size())
        Assertions.assertEquals(1L, inspector.message(0).get("grunnlag").get("versjon").longValue())
    }
}