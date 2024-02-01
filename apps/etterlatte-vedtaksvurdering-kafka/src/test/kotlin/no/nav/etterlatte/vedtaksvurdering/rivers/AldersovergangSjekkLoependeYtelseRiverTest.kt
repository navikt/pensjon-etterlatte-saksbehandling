package no.nav.etterlatte.vedtaksvurdering.rivers

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import no.nav.etterlatte.VedtakService
import no.nav.etterlatte.libs.common.vedtak.LoependeYtelseDTO
import no.nav.etterlatte.libs.common.vedtak.VedtakAldersovergangEvents.SJEKK_LOEPENDE_YTELSE
import no.nav.etterlatte.libs.common.vedtak.VedtakAldersovergangEvents.SJEKK_LOEPENDE_YTELSE_RESULTAT
import no.nav.etterlatte.rapidsandrivers.DATO_KEY
import no.nav.etterlatte.rapidsandrivers.SAK_ID_KEY
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.Month

class AldersovergangSjekkLoependeYtelseRiverTest {
    private val vedtakService = mockk<VedtakService>()

    private val inspector = TestRapid().apply { AldersovergangSjekkLoependeYtelseRiver(this, vedtakService) }

    private val sakId = 1234L
    private val datoFom = LocalDate.of(2024, Month.FEBRUARY, 1)

    @Test
    fun `skal lese melding og sjekke loepende ytelse`() {
        every { vedtakService.harLoependeYtelserFra(sakId, datoFom) } returns LoependeYtelseDTO(true, datoFom)

        val melding =
            JsonMessage.newMessage(
                SJEKK_LOEPENDE_YTELSE.lagEventnameForType(),
                mapOf(
                    SAK_ID_KEY to sakId,
                    DATO_KEY to datoFom,
                    "ao-type" to "AO20",
                ),
            )

        with(inspector.apply { sendTestMessage(melding.toJson()) }.inspektør) {
            size shouldBe 1
            field(0, "@event_name").asText() shouldBe SJEKK_LOEPENDE_YTELSE_RESULTAT.lagEventnameForType()
            field(0, "loependeYtelse").asBoolean() shouldBe true
            field(0, "ao-type").asText() shouldBe "AO20"
        }
    }

    @Test
    fun `skal lese melding og returnere negativt svar for loepende ytelse`() {
        every { vedtakService.harLoependeYtelserFra(sakId, datoFom) } returns LoependeYtelseDTO(false, datoFom)

        val melding =
            JsonMessage.newMessage(
                SJEKK_LOEPENDE_YTELSE.lagEventnameForType(),
                mapOf(
                    SAK_ID_KEY to sakId,
                    DATO_KEY to datoFom,
                    "ao-type" to "AO20",
                ),
            )

        with(inspector.apply { sendTestMessage(melding.toJson()) }.inspektør) {
            size shouldBe 1
            field(0, "@event_name").asText() shouldBe SJEKK_LOEPENDE_YTELSE_RESULTAT.lagEventnameForType()
            field(0, "loependeYtelse").asBoolean() shouldBe false
            field(0, "ao-type").asText() shouldBe "AO20"
        }
    }
}
