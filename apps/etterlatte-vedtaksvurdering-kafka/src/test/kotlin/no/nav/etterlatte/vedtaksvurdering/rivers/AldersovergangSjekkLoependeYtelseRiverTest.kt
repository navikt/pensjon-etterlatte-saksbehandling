package no.nav.etterlatte.vedtaksvurdering.rivers

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import no.nav.etterlatte.VedtakService
import no.nav.etterlatte.libs.common.rapidsandrivers.EVENT_NAME_KEY
import no.nav.etterlatte.libs.common.vedtak.LoependeYtelseDTO
import no.nav.etterlatte.libs.common.vedtak.VedtakAldersovergangStepEvents
import no.nav.etterlatte.libs.common.vedtak.VedtakAldersovergangStepEvents.LOEPENDE_YTELSE_RESULTAT
import no.nav.etterlatte.rapidsandrivers.EventNames
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Test
import rapidsandrivers.ALDERSOVERGANG_ID_KEY
import rapidsandrivers.ALDERSOVERGANG_STEP_KEY
import rapidsandrivers.ALDERSOVERGANG_TYPE_KEY
import rapidsandrivers.DATO_KEY
import rapidsandrivers.SAK_ID_KEY
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
                EventNames.ALDERSOVERANG.name,
                mapOf(
                    ALDERSOVERGANG_STEP_KEY to VedtakAldersovergangStepEvents.SJEKK_LOEPENDE_YTELSE.name,
                    ALDERSOVERGANG_TYPE_KEY to "BP20",
                    ALDERSOVERGANG_ID_KEY to "123",
                    SAK_ID_KEY to sakId,
                    DATO_KEY to datoFom,
                ),
            )

        with(inspector.apply { sendTestMessage(melding.toJson()) }.inspektør) {
            size shouldBe 1
            field(0, EVENT_NAME_KEY).asText() shouldBe EventNames.ALDERSOVERANG.name
            field(0, ALDERSOVERGANG_STEP_KEY).asText() shouldBe LOEPENDE_YTELSE_RESULTAT.name
            field(0, ALDERSOVERGANG_TYPE_KEY).asText() shouldBe "BP20"
            field(0, ALDERSOVERGANG_ID_KEY).asText() shouldBe "123"
            field(0, "loependeYtelse").asBoolean() shouldBe true
        }
    }

    @Test
    fun `skal lese melding og returnere negativt svar for loepende ytelse`() {
        every { vedtakService.harLoependeYtelserFra(sakId, datoFom) } returns LoependeYtelseDTO(false, datoFom)

        val melding =
            JsonMessage.newMessage(
                EventNames.ALDERSOVERANG.name,
                mapOf(
                    ALDERSOVERGANG_STEP_KEY to VedtakAldersovergangStepEvents.SJEKK_LOEPENDE_YTELSE.name,
                    ALDERSOVERGANG_TYPE_KEY to "BP20",
                    ALDERSOVERGANG_ID_KEY to "432",
                    SAK_ID_KEY to sakId,
                    DATO_KEY to datoFom,
                ),
            )

        with(inspector.apply { sendTestMessage(melding.toJson()) }.inspektør) {
            size shouldBe 1
            field(0, EVENT_NAME_KEY).asText() shouldBe EventNames.ALDERSOVERANG.name
            field(0, ALDERSOVERGANG_STEP_KEY).asText() shouldBe LOEPENDE_YTELSE_RESULTAT.name
            field(0, ALDERSOVERGANG_TYPE_KEY).asText() shouldBe "BP20"
            field(0, ALDERSOVERGANG_ID_KEY).asText() shouldBe "432"
            field(0, "loependeYtelse").asBoolean() shouldBe false
        }
    }
}
