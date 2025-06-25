package no.nav.etterlatte.brukerdialog.soeknad

import io.mockk.clearAllMocks
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import no.nav.etterlatte.behandling.randomSakId
import no.nav.etterlatte.behandling.tilSakId
import no.nav.etterlatte.brukerdialog.soeknad.client.BehandlingClient
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.event.GyldigSoeknadVurdert
import no.nav.etterlatte.libs.common.event.SoeknadInnsendtHendelseType
import no.nav.etterlatte.libs.common.rapidsandrivers.EVENT_NAME_KEY
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.util.UUID

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class OpprettBehandlingRiverTest {
    private val behandlingClientMock = mockk<BehandlingClient>()

    @AfterEach
    fun afterEach() {
        clearAllMocks()
    }

    @Test
    fun `OMSTILLINGSSTOENAD - Skal opprette sak og behandling`() {
        val soeker = "13848599411"
        val sakId = randomSakId()
        val behandlingId = UUID.randomUUID()

        every {
            behandlingClientMock.finnEllerOpprettSak(any(), any())
        } returns Sak(soeker, SakType.OMSTILLINGSSTOENAD, sakId, Enheter.PORSGRUNN.enhetNr, null, null)
        every { behandlingClientMock.opprettBehandling(any(), any(), any()) } returns behandlingId

        val inspector = testRapid().apply { sendTestMessage(getJson("/behandlingsbehov/omstillingsstoenad.json")) }.inspektør
        val message = inspector.message(0)

        assertEquals(1, inspector.size)
        assertEquals(
            SoeknadInnsendtHendelseType.EVENT_NAME_BEHANDLINGBEHOV.lagEventnameForType(),
            message.get(EVENT_NAME_KEY).asText(),
        )
        assertEquals(sakId, message.get(GyldigSoeknadVurdert.sakIdKey).tilSakId())
        assertEquals(behandlingId.toString(), message.get(GyldigSoeknadVurdert.behandlingIdKey).asText())

        coVerify(exactly = 1) { behandlingClientMock.finnEllerOpprettSak(soeker, SakType.OMSTILLINGSSTOENAD) }
        coVerify(exactly = 1) { behandlingClientMock.opprettBehandling(sakId, any(), any()) }
    }

    @Test
    fun `BARNEPENSJON - Skal opprette sak og behandling`() {
        val soeker = "24111258054"
        val sakId = randomSakId()
        val behandlingId = UUID.randomUUID()

        every {
            behandlingClientMock.finnEllerOpprettSak(any(), any())
        } returns Sak(soeker, SakType.BARNEPENSJON, sakId, Enheter.PORSGRUNN.enhetNr, null, null)
        every { behandlingClientMock.opprettBehandling(any(), any(), any()) } returns behandlingId

        val inspector = testRapid().apply { sendTestMessage(getJson("/behandlingsbehov/barnepensjon.json")) }.inspektør
        val message = inspector.message(0)

        assertEquals(
            SoeknadInnsendtHendelseType.EVENT_NAME_BEHANDLINGBEHOV.lagEventnameForType(),
            message.get(EVENT_NAME_KEY).asText(),
        )
        assertEquals(sakId, message.get(GyldigSoeknadVurdert.sakIdKey).tilSakId())
        assertEquals(behandlingId.toString(), message.get(GyldigSoeknadVurdert.behandlingIdKey).asText())

        assertEquals(1, inspector.size)

        coVerify(exactly = 1) { behandlingClientMock.finnEllerOpprettSak(soeker, SakType.BARNEPENSJON) }
        coVerify(exactly = 1) { behandlingClientMock.opprettBehandling(sakId, any(), any()) }
    }

    private fun testRapid() =
        TestRapid().apply {
            OpprettBehandlingRiver(this, behandlingClientMock)
        }

    private fun getJson(file: String) = javaClass.getResource(file)!!.readText()
}
