package regulering

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.etterlatte.libs.common.behandling.Omberegningshendelse
import no.nav.etterlatte.libs.common.behandling.Prosesstype
import no.nav.etterlatte.libs.common.behandling.RevurderingAarsak
import no.nav.etterlatte.libs.common.loependeYtelse.LoependeYtelseDTO
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.rapidsandrivers.EVENT_NAME_KEY
import no.nav.etterlatte.rapidsandrivers.EventNames.FINN_LOEPENDE_YTELSER
import no.nav.etterlatte.rapidsandrivers.EventNames.OMBEREGNINGSHENDELSE
import no.nav.etterlatte.regulering.LoependeYtelserforespoersel
import no.nav.etterlatte.regulering.VedtakService
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import rapidsandrivers.DATO_KEY
import rapidsandrivers.HENDELSE_DATA_KEY
import rapidsandrivers.SAK_ID_KEY
import java.time.LocalDate

internal class LoependeYtelserforespoerselTest {

    private val `1_mai_2023` = LocalDate.of(2023, 5, 1)
    private val sakId = 1L

    private fun genererReguleringMelding(dato: LocalDate, sakId: Long) = JsonMessage.newMessage(
        mapOf(
            EVENT_NAME_KEY to FINN_LOEPENDE_YTELSER,
            SAK_ID_KEY to sakId,
            DATO_KEY to dato
        )
    )

    @Test
    fun `kan ta imot reguleringsmelding og kalle paa vedtakservice med riktige verdier`() {
        val melding = genererReguleringMelding(`1_mai_2023`, sakId)
        val vedtakServiceMock = mockk<VedtakService>(relaxed = true)
        val inspector = TestRapid().apply { LoependeYtelserforespoersel(this, vedtakServiceMock) }

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
        val inspector = TestRapid().apply { LoependeYtelserforespoersel(this, vedtakServiceMock) }

        inspector.sendTestMessage(melding.toJson())
        val sendtMelding = inspector.inspektør.message(0)
        Assertions.assertEquals(OMBEREGNINGSHENDELSE, sendtMelding.get(EVENT_NAME_KEY).asText())
        Assertions.assertEquals(
            Omberegningshendelse(
                sakId = sakId,
                fradato = fraDato,
                aarsak = RevurderingAarsak.GRUNNBELOEPREGULERING,
                prosesstype = Prosesstype.AUTOMATISK
            ),
            objectMapper.readValue(sendtMelding.get(HENDELSE_DATA_KEY).toString(), Omberegningshendelse::class.java)
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
        val inspector = TestRapid().apply { LoependeYtelserforespoersel(this, vedtakServiceMock) }

        inspector.sendTestMessage(melding.toJson())
        Assertions.assertEquals(0, inspector.inspektør.size)
    }
}