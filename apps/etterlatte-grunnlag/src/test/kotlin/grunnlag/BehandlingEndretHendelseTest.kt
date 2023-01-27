package no.nav.etterlatte.grunnlag

import io.mockk.every
import io.mockk.mockk
import no.nav.etterlatte.klienter.BehandlingKlient
import no.nav.etterlatte.libs.common.event.BehandlingGrunnlagEndret
import no.nav.etterlatte.libs.common.event.BehandlingGrunnlagEndretMedGrunnlag
import no.nav.etterlatte.libs.common.rapidsandrivers.eventNameKey
import no.nav.etterlatte.libs.testdata.grunnlag.GrunnlagTestData
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.*

internal class BehandlingEndretHendelseTest {
    companion object {
        val opplysningerMock = mockk<OpplysningDao>()
    }

    private val behandlingKlient = mockk<BehandlingKlient>()
    private val sendToRapid: (String, UUID) -> Unit = mockk(relaxed = true)

    private val inspector = TestRapid().apply {
        BehandlingEndretHendlese(this, RealGrunnlagService(opplysningerMock, sendToRapid, behandlingKlient, mockk()))
    }

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