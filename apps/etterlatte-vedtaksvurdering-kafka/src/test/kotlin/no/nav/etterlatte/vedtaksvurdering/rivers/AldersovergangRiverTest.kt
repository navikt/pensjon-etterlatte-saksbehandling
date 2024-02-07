package no.nav.etterlatte.vedtaksvurdering.rivers

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import no.nav.etterlatte.VedtakService
import no.nav.etterlatte.libs.common.rapidsandrivers.EVENT_NAME_KEY
import no.nav.etterlatte.libs.common.vedtak.LoependeYtelseDTO
import no.nav.etterlatte.libs.common.vedtak.VedtakAldersovergangStepEvents
import no.nav.etterlatte.libs.common.vedtak.VedtakAldersovergangStepEvents.VURDERT_LOEPENDE_YTELSE
import no.nav.etterlatte.rapidsandrivers.ALDERSOVERGANG_ID_KEY
import no.nav.etterlatte.rapidsandrivers.ALDERSOVERGANG_STEG_KEY
import no.nav.etterlatte.rapidsandrivers.ALDERSOVERGANG_TYPE_KEY
import no.nav.etterlatte.rapidsandrivers.DATO_KEY
import no.nav.etterlatte.rapidsandrivers.EventNames
import no.nav.etterlatte.rapidsandrivers.SAK_ID_KEY
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.Month

class AldersovergangRiverTest {
    private val vedtakService = mockk<VedtakService>()

    private val inspector = TestRapid().apply { AldersovergangRiver(this, vedtakService) }

    private val sakId = 1234L
    private val datoFom = LocalDate.of(2024, Month.FEBRUARY, 1)

    @Test
    fun `skal lese melding og sjekke loepende ytelse`() {
        every { vedtakService.harLoependeYtelserFra(sakId, datoFom) } returns LoependeYtelseDTO(true, datoFom)

        val melding =
            JsonMessage.newMessage(
                EventNames.ALDERSOVERGANG.name,
                mapOf(
                    ALDERSOVERGANG_STEG_KEY to VedtakAldersovergangStepEvents.IDENTIFISERT_SAK.name,
                    ALDERSOVERGANG_TYPE_KEY to "BP20",
                    ALDERSOVERGANG_ID_KEY to "123-123-123",
                    SAK_ID_KEY to sakId,
                    DATO_KEY to datoFom,
                ),
            )

        with(inspector.apply { sendTestMessage(melding.toJson()) }.inspektør) {
            size shouldBe 1
            field(0, EVENT_NAME_KEY).asText() shouldBe EventNames.ALDERSOVERGANG.name
            field(0, ALDERSOVERGANG_STEG_KEY).asText() shouldBe VURDERT_LOEPENDE_YTELSE.name
            field(0, ALDERSOVERGANG_TYPE_KEY).asText() shouldBe "BP20"
            field(0, ALDERSOVERGANG_ID_KEY).asText() shouldBe "123-123-123"
            field(0, "loependeYtelse").asBoolean() shouldBe true
        }
    }

    @Test
    fun `skal lese melding og returnere negativt svar for loepende ytelse`() {
        every { vedtakService.harLoependeYtelserFra(sakId, datoFom) } returns LoependeYtelseDTO(false, datoFom)

        val melding =
            JsonMessage.newMessage(
                EventNames.ALDERSOVERGANG.name,
                mapOf(
                    ALDERSOVERGANG_STEG_KEY to VedtakAldersovergangStepEvents.IDENTIFISERT_SAK.name,
                    ALDERSOVERGANG_TYPE_KEY to "BP20",
                    ALDERSOVERGANG_ID_KEY to "432-987-234",
                    SAK_ID_KEY to sakId,
                    DATO_KEY to datoFom,
                ),
            )

        with(inspector.apply { sendTestMessage(melding.toJson()) }.inspektør) {
            size shouldBe 1
            field(0, EVENT_NAME_KEY).asText() shouldBe EventNames.ALDERSOVERGANG.name
            field(0, ALDERSOVERGANG_STEG_KEY).asText() shouldBe VURDERT_LOEPENDE_YTELSE.name
            field(0, ALDERSOVERGANG_TYPE_KEY).asText() shouldBe "BP20"
            field(0, ALDERSOVERGANG_ID_KEY).asText() shouldBe "432-987-234"
            field(0, "loependeYtelse").asBoolean() shouldBe false
        }
    }
}
