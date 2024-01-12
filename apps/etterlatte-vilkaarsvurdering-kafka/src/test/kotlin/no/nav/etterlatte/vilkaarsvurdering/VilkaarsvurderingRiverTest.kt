package no.nav.etterlatte.vilkaarsvurdering

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import no.nav.etterlatte.rapidsandrivers.ReguleringEvents
import no.nav.etterlatte.vilkaarsvurdering.services.VilkaarsvurderingService
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.util.UUID

internal class VilkaarsvurderingRiverTest {
    private val vilkaarsvurderingServiceMock =
        mockk<VilkaarsvurderingService> {
            coEvery { kopierForrigeVilkaarsvurdering(any(), any()) } returns mockk()
        }
    private val testRapid = TestRapid().apply { VilkaarsvurderingRiver(this, vilkaarsvurderingServiceMock) }

    @Test
    fun `tar opp VILKAARSVURDER-event, kopierer vilkaarsvurdering og poster ny BEREGN-meldig på koen`() {
        val behandlingId = UUID.randomUUID()
        val behandlingViOmregnerFra = UUID.randomUUID()

        val melding =
            JsonMessage.newMessage(
                mapOf(
                    "@event_name" to ReguleringEvents.VILKAARSVURDER,
                    "sakId" to 1,
                    "behandlingId" to behandlingId,
                    "behandling_vi_omregner_fra" to behandlingViOmregnerFra,
                ),
            ).toJson()
        testRapid.sendTestMessage(melding)

        coVerify(exactly = 1) {
            vilkaarsvurderingServiceMock.kopierForrigeVilkaarsvurdering(
                behandlingId,
                behandlingViOmregnerFra,
            )
        }
        with(testRapid.inspektør.message(0)) {
            Assertions.assertEquals(ReguleringEvents.BEREGN, this["@event_name"].asText())
        }
    }
}
