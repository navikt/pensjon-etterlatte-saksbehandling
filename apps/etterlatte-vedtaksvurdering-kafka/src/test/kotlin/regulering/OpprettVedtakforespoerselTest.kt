package regulering

import io.mockk.mockk
import io.mockk.verify
import no.nav.etterlatte.libs.common.rapidsandrivers.eventNameKey
import no.nav.etterlatte.rapidsandrivers.EventNames.OPPRETT_VEDTAK
import no.nav.etterlatte.rapidsandrivers.EventNames.TIL_UTBETALING
import no.nav.etterlatte.regulering.OpprettVedtakforespoersel
import no.nav.etterlatte.regulering.VedtakService
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import rapidsandrivers.behandlingIdKey
import rapidsandrivers.datoKey
import rapidsandrivers.sakIdKey
import java.time.LocalDate
import java.util.*

internal class OpprettVedtakforespoerselTest {

    private val `1_mai_2023` = LocalDate.of(2023, 5, 1)
    private val sakId = 1L

    private fun genererOpprettVedtakforespoersel(behandlingId: UUID) = JsonMessage.newMessage(
        mapOf(
            eventNameKey to OPPRETT_VEDTAK,
            sakIdKey to sakId,
            datoKey to `1_mai_2023`,
            behandlingIdKey to behandlingId
        )
    )

    @Test
    fun `kan ta imot opprettVedtak-melding og kalle paa vedtakservice med riktige verdier`() {
        val behandlingId = UUID.randomUUID()
        val melding = genererOpprettVedtakforespoersel(behandlingId)
        val vedtakServiceMock = mockk<VedtakService>(relaxed = true)
        val inspector = TestRapid().apply { OpprettVedtakforespoersel(this, vedtakServiceMock) }
        inspector.sendTestMessage(melding.toJson())

        verify(exactly = 1) {
            vedtakServiceMock.upsertVedtak(behandlingId)
        }
    }

    @Test
    fun `skal lage ny melding når ting har gått bra`() {
        val behandlingId = UUID.randomUUID()
        val melding = genererOpprettVedtakforespoersel(behandlingId)
        val vedtakServiceMock = mockk<VedtakService>(relaxed = true)
        val inspector = TestRapid().apply { OpprettVedtakforespoersel(this, vedtakServiceMock) }

        inspector.sendTestMessage(melding.toJson())
        val sendtMelding = inspector.inspektør.message(0)
        Assertions.assertEquals(TIL_UTBETALING, sendtMelding.get(eventNameKey).asText())
    }
}