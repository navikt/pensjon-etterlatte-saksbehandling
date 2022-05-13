package grunnlag

import io.mockk.every
import io.mockk.mockk
import no.nav.etterlatte.Context
import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.Self
import no.nav.etterlatte.grunnlag.GrunnlagFactory
import no.nav.etterlatte.grunnlag.GrunnlagHendelser
import no.nav.etterlatte.grunnlag.OpplysningDao
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


    private val inspector = TestRapid().apply { GrunnlagHendelser(this, GrunnlagFactory(opplysningerMock)) }

    @Test
    fun `skal lese opplysningsbehov og legge til opplysning`() {
        Kontekst.set(Context(Self("testApp"), TestDbKontekst))

        val opplysninger = listOf(
            Grunnlagsopplysning(
                UUID.randomUUID(),
                Grunnlagsopplysning.Saksbehandler("S01"),
                Opplysningstyper.SOEKER_SOEKNAD_V1,
                objectMapper.createObjectNode(),
                objectMapper.createObjectNode()
            ),
            Grunnlagsopplysning(
                UUID.randomUUID(),
                Grunnlagsopplysning.Saksbehandler("S01"),
                Opplysningstyper.AVDOED_SOEKNAD_V1,
                objectMapper.createObjectNode(),
                objectMapper.createObjectNode()
            ),
        )

        every { opplysningerMock.finnOpplysningerIGrunnlag(any())} returns opplysninger
        every { opplysningerMock.leggOpplysningTilGrunnlag(any(),any())} returns Unit
        every { opplysningerMock.slettSpesifikkOpplysningISak(any(),any())} returns Unit
        val inspector = inspector.apply { sendTestMessage(melding) }.inspektør

        Assertions.assertEquals("GRUNNLAG:GRUNNLAGENDRET", inspector.message(0).get("@event_name").asText())
        Assertions.assertEquals(3, inspector.message(0).get("grunnlag").get("grunnlag").size())
    }
}
