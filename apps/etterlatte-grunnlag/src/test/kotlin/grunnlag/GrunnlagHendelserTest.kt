package grunnlag

import io.mockk.every
import io.mockk.mockk
import no.nav.etterlatte.Context
import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.Self
import no.nav.etterlatte.grunnlag.GrunnlagHendelser
import no.nav.etterlatte.grunnlag.OpplysningDao
import no.nav.etterlatte.grunnlag.RealGrunnlagService
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstyper
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import testutils.TestDbKontekst
import java.io.FileNotFoundException
import java.util.*

class GrunnlagHendelserTest {
    companion object {
        val melding = readFile("/opplysningsmeldingNy.json")
        val opplysningerMock = mockk<OpplysningDao>()
        fun readFile(file: String) = Companion::class.java.getResource(file)?.readText()
            ?: throw FileNotFoundException("Fant ikke filen $file")
    }


    private val inspector = TestRapid().apply { GrunnlagHendelser(this, RealGrunnlagService(opplysningerMock)) }

    @Test
    fun `skal lese opplysningsbehov og legge til opplysning`() {
        Kontekst.set(Context(Self("testApp"), TestDbKontekst))

        val grunnlagshendelser = listOf(
            OpplysningDao.GrunnlagHendelse(Grunnlagsopplysning(
                UUID.randomUUID(),
                Grunnlagsopplysning.Saksbehandler("S01"),
                Opplysningstyper.SOEKER_SOEKNAD_V1,
                objectMapper.createObjectNode(),
                objectMapper.createObjectNode()
            ), 2, 1)
            ,OpplysningDao.GrunnlagHendelse( Grunnlagsopplysning(
                UUID.randomUUID(),
                Grunnlagsopplysning.Saksbehandler("S01"),
                Opplysningstyper.AVDOED_SOEKNAD_V1,
                objectMapper.createObjectNode(),
                objectMapper.createObjectNode()
            ), 2, 2)
            ,
        )

        every { opplysningerMock.finnHendelserIGrunnlag(any())} returns grunnlagshendelser
        every { opplysningerMock.leggOpplysningTilGrunnlag(any(),any())} returns 1L
        val inspector = inspector.apply { sendTestMessage(melding) }.inspektør

        Assertions.assertEquals("GRUNNLAG:GRUNNLAGENDRET", inspector.message(0).get("@event_name").asText())
        Assertions.assertEquals(2, inspector.message(0).get("grunnlag").get("grunnlag").size())
        Assertions.assertEquals(2L, inspector.message(0).get("grunnlag").get("versjon").longValue())
    }
}
