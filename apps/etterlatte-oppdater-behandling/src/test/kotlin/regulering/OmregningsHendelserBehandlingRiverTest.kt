package no.nav.etterlatte.regulering

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import no.nav.etterlatte.BehandlingServiceImpl
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.deserialize
import no.nav.etterlatte.libs.common.revurdering.AutomatiskRevurderingRequest
import no.nav.etterlatte.libs.common.revurdering.AutomatiskRevurderingResponse
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.rapidsandrivers.HENDELSE_DATA_KEY
import no.nav.etterlatte.rapidsandrivers.OmregningData
import no.nav.helse.rapids_rivers.testsupport.TestRapid
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

        revurderingRequestSlot.captured.sakId shouldBe 1L
        inspector.inspektør.size shouldBe 2
        with(
            deserialize<OmregningData>(
                inspector.inspektør
                    .message(1)
                    .get(HENDELSE_DATA_KEY)
                    .toJson(),
            ),
        ) {
            hentBehandlingId() shouldBe behandlingId
            hentForrigeBehandlingid() shouldBe behandlingViOmregnerFra
        }
    }

    companion object {
        val fullMelding = readFile("/omregningshendelse.json")
    }
}

fun readFile(file: String) =
    OmregningsHendelserBehandlingRiverTest::class.java.getResource(file)?.readText()
        ?: throw FileNotFoundException("Fant ikke filen $file")
