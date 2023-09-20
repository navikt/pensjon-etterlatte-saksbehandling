package regulering

import io.mockk.mockk
import io.mockk.verify
import no.nav.etterlatte.VedtakService
import no.nav.etterlatte.libs.common.rapidsandrivers.EVENT_NAME_KEY
import no.nav.etterlatte.rapidsandrivers.EventNames.OPPRETT_VEDTAK
import no.nav.etterlatte.regulering.OpprettVedtakforespoersel
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Test
import rapidsandrivers.BEHANDLING_ID_KEY
import rapidsandrivers.DATO_KEY
import rapidsandrivers.SAK_ID_KEY
import java.time.LocalDate
import java.util.UUID

internal class OpprettVedtakforespoerselTest {
    private val foersteMai2023 = LocalDate.of(2023, 5, 1)
    private val sakId = 1L

    private fun genererOpprettVedtakforespoersel(behandlingId: UUID) =
        JsonMessage.newMessage(
            mapOf(
                EVENT_NAME_KEY to OPPRETT_VEDTAK,
                SAK_ID_KEY to sakId,
                DATO_KEY to foersteMai2023,
                BEHANDLING_ID_KEY to behandlingId,
            ),
        )

    @Test
    fun `skal baade opprette vedtak og fatte det samt attestere`() {
        val behandlingId = UUID.randomUUID()
        val melding = genererOpprettVedtakforespoersel(behandlingId)
        val vedtakServiceMock = mockk<VedtakService>(relaxed = true)
        val inspector = TestRapid().apply { OpprettVedtakforespoersel(this, vedtakServiceMock) }

        inspector.sendTestMessage(melding.toJson())

        verify { vedtakServiceMock.opprettVedtakFattOgAttester(sakId, behandlingId) }
    }
}
