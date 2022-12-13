package grunnlag

import GrunnlagTestData
import io.mockk.every
import io.mockk.mockk
import no.nav.etterlatte.grunnlag.BehandlingEndretHendlese
import no.nav.etterlatte.grunnlag.OpplysningDao
import no.nav.etterlatte.grunnlag.RealGrunnlagService
import no.nav.etterlatte.libs.common.event.BehandlingGrunnlagEndret
import no.nav.etterlatte.libs.common.event.BehandlingGrunnlagEndretMedGrunnlag
import no.nav.etterlatte.libs.common.rapidsandrivers.eventNameKey
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class BehandlingEndretHendleseTest {
    companion object {
        val opplysningerMock = mockk<OpplysningDao>()
    }

    private val inspector = TestRapid().apply { BehandlingEndretHendlese(this, RealGrunnlagService(opplysningerMock)) }

    @BeforeEach
    fun init() {
        val grunnlagshendelser = emptyList<OpplysningDao.GrunnlagHendelse>()

        every { opplysningerMock.finnHendelserIGrunnlag(any()) } returns grunnlagshendelser
        every { opplysningerMock.hentAlleGrunnlagForSak(any()) } returns grunnlagshendelser
        every { opplysningerMock.leggOpplysningTilGrunnlag(any(), any()) } returns 1L
    }

    @Test
    fun `legger paa grunnlag naar behandling er endret`() {
        val inspector = inspector.apply { sendTestMessage(behandlingGrunnlagEndretMelding().toJson()) }.inspektør

        assertEquals(1, inspector.size)
        assertNotNull(inspector.message(0).get(BehandlingGrunnlagEndretMedGrunnlag.grunnlagKey))
    }

    private fun behandlingGrunnlagEndretMelding() = JsonMessage.newMessage(
        mapOf(
            eventNameKey to BehandlingGrunnlagEndret.eventName,
            BehandlingGrunnlagEndret.sakIdKey to 1,
            "persongalleri" to GrunnlagTestData().hentPersonGalleri()
        )
    )
}