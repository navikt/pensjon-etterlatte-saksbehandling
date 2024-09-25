package no.nav.etterlatte.regulering

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import no.nav.etterlatte.BehandlingServiceImpl
import no.nav.etterlatte.behandling.sakId1
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.revurdering.AutomatiskRevurderingRequest
import no.nav.etterlatte.libs.common.revurdering.AutomatiskRevurderingResponse
import no.nav.etterlatte.rapidsandrivers.BEHANDLING_ID_KEY
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.io.FileNotFoundException
import java.util.UUID

internal class OmregningsHendelserBehandlingRiverTest {
    private val behandlingService = mockk<BehandlingServiceImpl>()
    private val inspector = TestRapid().apply { OmregningsHendelserBehandlingRiver(this, behandlingService) }

    @Test
    fun `skal opprette omregning`() {
        val revurderingRequestSlot = slot<AutomatiskRevurderingRequest>()
        val behandlingId = UUID.randomUUID()
        val behandlingViOmregnerFra = UUID.randomUUID()

        val returnValue = AutomatiskRevurderingResponse(behandlingId, behandlingViOmregnerFra, SakType.BARNEPENSJON)

        every { behandlingService.opprettAutomatiskRevurdering(capture(revurderingRequestSlot)) }.returns(returnValue)

        val inspector = inspector.apply { sendTestMessage(fullMelding) }

        inspector.sendTestMessage(fullMelding)

        Assertions.assertEquals(sakId1, revurderingRequestSlot.captured.sakId)
        Assertions.assertEquals(2, inspector.inspektør.size)
        Assertions.assertEquals(
            behandlingId.toString(),
            inspector.inspektør
                .message(1)
                .get(BEHANDLING_ID_KEY)
                .asText(),
        )
    }

    companion object {
        val fullMelding = readFile("/omregningshendelse.json")
    }
}

fun readFile(file: String) =
    OmregningsHendelserBehandlingRiverTest::class.java.getResource(file)?.readText()
        ?: throw FileNotFoundException("Fant ikke filen $file")
