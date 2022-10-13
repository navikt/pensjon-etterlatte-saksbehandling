package grunnlag

import io.mockk.every
import io.mockk.mockk
import lagGrunnlagHendelse
import no.nav.etterlatte.grunnlag.BehandlingEndretHendlese
import no.nav.etterlatte.grunnlag.OpplysningDao
import no.nav.etterlatte.grunnlag.RealGrunnlagService
import no.nav.etterlatte.libs.common.event.BehandlingGrunnlagEndretMedGrunnlag
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstyper.AVDOED_SOEKNAD_V1
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstyper.SOEKER_SOEKNAD_V1
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.FileNotFoundException

internal class BehandlingEndretHendleseTest {
    companion object {
        val melding = readFile("/behandlinggrunnlagendret.json")
        val opplysningerMock = mockk<OpplysningDao>()
        private fun readFile(file: String) = Companion::class.java.getResource(file)?.readText()
            ?: throw FileNotFoundException("Fant ikke filen $file")
    }

    private val inspector = TestRapid().apply { BehandlingEndretHendlese(this, RealGrunnlagService(opplysningerMock)) }

    @BeforeEach
    fun init() {
        val grunnlagshendelser = listOf(
            lagGrunnlagHendelse(2, 1, SOEKER_SOEKNAD_V1),
            lagGrunnlagHendelse(2, 2, AVDOED_SOEKNAD_V1)
        )

        every { opplysningerMock.finnHendelserIGrunnlag(any()) } returns grunnlagshendelser
        every { opplysningerMock.hentAlleGrunnlagForSak(any()) } returns grunnlagshendelser
        every { opplysningerMock.leggOpplysningTilGrunnlag(any(), any()) } returns 1L
    }

    @Test
    fun `legger paa grunnlagV2 naar behandling er endret`() {
        val inspector = inspector.apply { sendTestMessage(melding) }.inspektør

        assertEquals(1, inspector.size)
        assertNotNull(inspector.message(0).get(BehandlingGrunnlagEndretMedGrunnlag.grunnlagV2Key))
    }
}