package regulering

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.etterlatte.libs.common.behandling.Omberegningshendelse
import no.nav.etterlatte.libs.common.behandling.RevurderingAarsak
import no.nav.etterlatte.libs.common.loependeYtelse.LoependeYtelseDTO
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.rapidsandrivers.eventNameKey
import no.nav.etterlatte.regulering.OMBEREGNING_EVENT_NAME
import no.nav.etterlatte.regulering.Reguleringsforespoersel
import no.nav.etterlatte.regulering.VedtakService
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class ReguleringsforespoerselTest {

    private val `1_mai_2023` = LocalDate.of(2023, 5, 1)
    private val sakId = 1L

    private fun genererReguleringMelding(dato: LocalDate, sakId: Long) = JsonMessage.newMessage(
        mapOf(
            eventNameKey to "REGULERING",
            "sakId" to sakId,
            "dato" to dato
        )
    )

    @Test
    fun `kan ta imot reguleringsmelding og kalle paa vedtakservice med riktige verdier`() {
        val melding = genererReguleringMelding(`1_mai_2023`, sakId)
        val vedtakServiceMock = mockk<VedtakService>(relaxed = true)
        val inspector = TestRapid().apply { Reguleringsforespoersel(this, vedtakServiceMock) }

        inspector.sendTestMessage(melding.toJson())
        verify(exactly = 1) {
            vedtakServiceMock.harLoependeYtelserFra(1L, LocalDate.of(2023, 5, 1))
        }
    }

    @Test
    fun `skal lage ny melding med dato basert paa hva ytelsen har som foerste mulige dato`() {
        val fraDato = LocalDate.of(2023, 8, 1)
        val melding = genererReguleringMelding(`1_mai_2023`, sakId)
        val vedtakServiceMock = mockk<VedtakService>(relaxed = true)
        every { vedtakServiceMock.harLoependeYtelserFra(sakId, `1_mai_2023`) } returns LoependeYtelseDTO(true, fraDato)
        val inspector = TestRapid().apply { Reguleringsforespoersel(this, vedtakServiceMock) }

        inspector.sendTestMessage(melding.toJson())
        val sendtMelding = inspector.inspektør.message(0)
        Assertions.assertEquals(OMBEREGNING_EVENT_NAME, sendtMelding.get(eventNameKey).asText())
        Assertions.assertEquals(
            Omberegningshendelse(
                sakId = sakId,
                fradato = fraDato,
                aarsak = RevurderingAarsak.GRUNNBELOEPREGULERING
            ),
            objectMapper.readValue(sendtMelding.get("hendelse_data").toString(), Omberegningshendelse::class.java)
        )
    }

    @Test
    fun `sender ikke ny melding dersom det ikke er en loepende ytelse`() {
        val melding = genererReguleringMelding(`1_mai_2023`, sakId)
        val vedtakServiceMock = mockk<VedtakService>(relaxed = true)
        every { vedtakServiceMock.harLoependeYtelserFra(sakId, `1_mai_2023`) } returns LoependeYtelseDTO(
            false,
            `1_mai_2023`
        )
        val inspector = TestRapid().apply { Reguleringsforespoersel(this, vedtakServiceMock) }

        inspector.sendTestMessage(melding.toJson())
        Assertions.assertEquals(0, inspector.inspektør.size)
    }
}