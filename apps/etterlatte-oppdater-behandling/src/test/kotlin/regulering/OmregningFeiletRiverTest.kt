package no.nav.etterlatte.regulering

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import no.nav.etterlatte.BehandlingService
import no.nav.etterlatte.behandling.randomSakId
import no.nav.etterlatte.libs.common.behandling.Revurderingaarsak
import no.nav.etterlatte.libs.common.rapidsandrivers.lagParMedEventNameKey
import no.nav.etterlatte.libs.common.sak.KjoeringStatus
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.rapidsandrivers.EventNames.FEILA
import no.nav.etterlatte.rapidsandrivers.HENDELSE_DATA_KEY
import no.nav.etterlatte.rapidsandrivers.KONTEKST_KEY
import no.nav.etterlatte.rapidsandrivers.Kontekst
import no.nav.etterlatte.rapidsandrivers.OmregningData
import no.nav.etterlatte.rapidsandrivers.SAK_ID_KEY
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

internal class OmregningFeiletRiverTest {
    @Test
    fun `Skal varsle behandling om at det er en feilet omregning i en sak`() {
        val kjoering = slot<String>()
        val status = slot<KjoeringStatus>()
        val sakIdSlot = slot<SakId>()
        val sakId = randomSakId()

        val melding =
            JsonMessage.newMessage(
                mapOf(
                    FEILA.lagParMedEventNameKey(),
                    KONTEKST_KEY to Kontekst.OMREGNING,
                    SAK_ID_KEY to sakId,
                    HENDELSE_DATA_KEY to
                        OmregningData(
                            kjoering = "OmregningKjoering",
                            sakId = sakId,
                            revurderingaarsak = Revurderingaarsak.OMREGNING,
                        ).toPacket(),
                ),
            )

        val behandlingService = mockk<BehandlingService>(relaxed = true)
        every { behandlingService.lagreKjoering(capture(sakIdSlot), capture(status), capture(kjoering)) } returns Unit
        val inspector = TestRapid().apply { OmregningFeiletRiver(this, behandlingService) }

        inspector.sendTestMessage(melding.toJson())
        val sendteMeldinger = inspector.inspektør.size
        Assertions.assertEquals(0, sendteMeldinger)
        Assertions.assertEquals("OmregningKjoering", kjoering.captured)
        Assertions.assertEquals(sakId, sakIdSlot.captured)
        Assertions.assertEquals(KjoeringStatus.FEILA, status.captured)
    }

    @Test
    fun `Skal varsle behandling om at det er en feilet regulering i en sak`() {
        val sakId = randomSakId()
        val melding =
            JsonMessage.newMessage(
                mapOf(
                    FEILA.lagParMedEventNameKey(),
                    KONTEKST_KEY to Kontekst.REGULERING,
                    SAK_ID_KEY to sakId,
                    HENDELSE_DATA_KEY to
                        OmregningData(
                            kjoering = "Regulering2023",
                            sakId = sakId,
                            revurderingaarsak = Revurderingaarsak.REGULERING,
                        ).toPacket(),
                ),
            )

        val behandlingService = mockk<BehandlingService>(relaxed = true)
        every { behandlingService.lagreKjoering(any(), any(), any()) } returns Unit
        val inspector = TestRapid().apply { OmregningFeiletRiver(this, behandlingService) }

        inspector.sendTestMessage(melding.toJson())
        val sendteMeldinger = inspector.inspektør.size
        Assertions.assertEquals(0, sendteMeldinger)
    }
}
