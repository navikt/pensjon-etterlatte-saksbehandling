package no.nav.etterlatte

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import no.nav.etterlatte.behandling.sakId1
import no.nav.etterlatte.brev.BrevRequestHendelseType
import no.nav.etterlatte.funksjonsbrytere.DummyFeatureToggleService
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.rapidsandrivers.EVENT_NAME_KEY
import no.nav.etterlatte.libs.common.rapidsandrivers.SAK_TYPE_KEY
import no.nav.etterlatte.libs.common.rapidsandrivers.lagParMedEventNameKey
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.rapidsandrivers.BEHANDLING_ID_KEY
import no.nav.etterlatte.rapidsandrivers.FNR_KEY
import no.nav.etterlatte.rapidsandrivers.SAK_ID_KEY
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.UUID

class OpprettBrevRiverTest {
    @Test
    fun `oppretter sak for fnr`() {
        val behandlingService =
            mockk<BehandlingService>().also {
                coEvery { it.finnEllerOpprettSak(any(), any()) } returns mockk<Sak>().also { every { it.id } returns sakId1 }
            }
        val testRapid = TestRapid().apply { OpprettBrevRiver(this, behandlingService, featureToggleService()) }
        testRapid.sendTestMessage(
            JsonMessage
                .newMessage(
                    mapOf(
                        BrevRequestHendelseType.OPPRETT_BREV.lagParMedEventNameKey(),
                        FNR_KEY to "123",
                        SAK_TYPE_KEY to SakType.BARNEPENSJON.name,
                    ),
                ).toJson(),
        )
        assertEquals(1, testRapid.inspektør.size)
        with(testRapid.inspektør.message(0)) {
            assertEquals(BrevRequestHendelseType.OPPRETT_JOURNALFOER_OG_DISTRIBUER.lagEventnameForType(), get(EVENT_NAME_KEY).asText())
            assertEquals(1L, get(SAK_ID_KEY).asLong())
        }
    }

    @Test
    fun `henter sak for behandling`() {
        val behandlingService =
            mockk<BehandlingService>().also {
                coEvery { it.hentBehandling(any()) } returns mockk<DetaljertBehandling>().also { every { it.sak } returns sakId1 }
            }
        val testRapid = TestRapid().apply { OpprettBrevRiver(this, behandlingService, featureToggleService()) }
        val behandlingId = UUID.randomUUID()
        testRapid.sendTestMessage(
            JsonMessage
                .newMessage(
                    mapOf(
                        BrevRequestHendelseType.OPPRETT_BREV.lagParMedEventNameKey(),
                        BEHANDLING_ID_KEY to behandlingId,
                        SAK_TYPE_KEY to SakType.BARNEPENSJON.name,
                    ),
                ).toJson(),
        )
        assertEquals(1, testRapid.inspektør.size)
        with(testRapid.inspektør.message(0)) {
            assertEquals(BrevRequestHendelseType.OPPRETT_JOURNALFOER_OG_DISTRIBUER.lagEventnameForType(), get(EVENT_NAME_KEY).asText())
            assertEquals(behandlingId.toString(), get(BEHANDLING_ID_KEY).asText())
        }
    }

    private fun featureToggleService() =
        DummyFeatureToggleService().also {
            it.settBryter(InformasjonsbrevFeatureToggle.SendInformasjonsbrev, true)
        }
}
