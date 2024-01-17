import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import no.nav.etterlatte.BehandlingService
import no.nav.etterlatte.OpprettBrevRiver
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.event.BrevEventKeys
import no.nav.etterlatte.libs.common.rapidsandrivers.EVENT_NAME_KEY
import no.nav.etterlatte.libs.common.rapidsandrivers.SAK_TYPE_KEY
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import rapidsandrivers.BEHANDLING_ID_KEY
import rapidsandrivers.FNR_KEY
import rapidsandrivers.SAK_ID_KEY
import java.util.UUID

class OpprettBrevRiverTest {
    @Test
    fun `oppretter sak for fnr`() {
        val behandlingService =
            mockk<BehandlingService>().also {
                coEvery { it.finnEllerOpprettSak(any(), any()) } returns mockk<Sak>().also { every { it.id } returns 1 }
            }
        val testRapid = TestRapid().apply { OpprettBrevRiver(this, behandlingService) }
        testRapid.sendTestMessage(
            JsonMessage.newMessage(
                mapOf(
                    EVENT_NAME_KEY to BrevEventKeys.OPPRETT_BREV,
                    FNR_KEY to "123",
                    SAK_TYPE_KEY to SakType.BARNEPENSJON.name,
                ),
            ).toJson(),
        )
        assertEquals(1, testRapid.inspektør.size)
        with(testRapid.inspektør.message(0)) {
            assertEquals(BrevEventKeys.OPPRETT_JOURNALFOER_OG_DISTRIBUER, get(EVENT_NAME_KEY).asText())
            assertEquals(1L, get(SAK_ID_KEY).asLong())
        }
    }

    @Test
    fun `henter sak for behandling`() {
        val behandlingService =
            mockk<BehandlingService>().also {
                coEvery { it.hentBehandling(any()) } returns mockk<DetaljertBehandling>().also { every { it.sak } returns 1L }
            }
        val testRapid = TestRapid().apply { OpprettBrevRiver(this, behandlingService) }
        val behandlingId = UUID.randomUUID()
        testRapid.sendTestMessage(
            JsonMessage.newMessage(
                mapOf(
                    EVENT_NAME_KEY to BrevEventKeys.OPPRETT_BREV,
                    BEHANDLING_ID_KEY to behandlingId,
                    SAK_TYPE_KEY to SakType.BARNEPENSJON.name,
                ),
            ).toJson(),
        )
        assertEquals(1, testRapid.inspektør.size)
        with(testRapid.inspektør.message(0)) {
            assertEquals(BrevEventKeys.OPPRETT_JOURNALFOER_OG_DISTRIBUER, get(EVENT_NAME_KEY).asText())
            assertEquals(behandlingId.toString(), get(BEHANDLING_ID_KEY).asText())
        }
    }
}
