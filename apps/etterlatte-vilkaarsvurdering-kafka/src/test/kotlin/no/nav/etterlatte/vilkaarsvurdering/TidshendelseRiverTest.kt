package no.nav.etterlatte.vilkaarsvurdering

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.etterlatte.libs.common.rapidsandrivers.EVENT_NAME_KEY
import no.nav.etterlatte.rapidsandrivers.ALDERSOVERGANG_ID_KEY
import no.nav.etterlatte.rapidsandrivers.ALDERSOVERGANG_STEG_KEY
import no.nav.etterlatte.rapidsandrivers.ALDERSOVERGANG_TYPE_KEY
import no.nav.etterlatte.rapidsandrivers.DATO_KEY
import no.nav.etterlatte.rapidsandrivers.DRYRUN
import no.nav.etterlatte.rapidsandrivers.EventNames
import no.nav.etterlatte.rapidsandrivers.HENDELSE_DATA_KEY
import no.nav.etterlatte.rapidsandrivers.SAK_ID_KEY
import no.nav.etterlatte.vilkaarsvurdering.services.VilkaarsvurderingService
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Month
import java.time.YearMonth
import java.util.UUID

class TidshendelseRiverTest {
    private val vilkaarsvurderingService = mockk<VilkaarsvurderingService>()
    private val inspector = TestRapid().apply { TidshendelseRiver(this, vilkaarsvurderingService) }

    @Test
    fun `skal sjekke yrkesskadefordel og angi property i melding lik true`() {
        val hendelseId = UUID.randomUUID()
        val sakId = 321L
        val behandlingsmaaned = YearMonth.of(2024, Month.APRIL)
        val behandlingIdPerReformtidspunkt = UUID.randomUUID().toString()
        val melding =
            lagMeldingForVurdertLoependeYtelse(
                hendelseId,
                sakId,
                behandlingsmaaned,
                behandlingIdPerReformtidspunkt,
            )
        every { vilkaarsvurderingService.harMigrertYrkesskadefordel(behandlingIdPerReformtidspunkt) } returns true

        with(inspector.apply { sendTestMessage(melding.toJson()) }.inspektør) {
            size shouldBe 1
            field(0, EVENT_NAME_KEY).asText() shouldBe EventNames.ALDERSOVERGANG.name
            field(0, ALDERSOVERGANG_STEG_KEY).asText() shouldBe "VURDERT_LOEPENDE_YTELSE_OG_VILKAAR"
            field(0, ALDERSOVERGANG_TYPE_KEY).asText() shouldBe "BP20"
            field(0, ALDERSOVERGANG_ID_KEY).asText() shouldBe hendelseId.toString()
            field(0, DRYRUN).asBoolean() shouldBe false
            field(0, HENDELSE_DATA_KEY) shouldHaveSize 1
            field(0, "yrkesskadefordel_pre_20240101").asBoolean() shouldBe true
        }

        verify { vilkaarsvurderingService.harMigrertYrkesskadefordel(behandlingIdPerReformtidspunkt) }
    }

    @Test
    fun `skal ikke sjekke yrkesskadefordel da det ikke finnes en behandlingId for det i melding`() {
        val hendelseId = UUID.randomUUID()
        val sakId = 321L
        val behandlingsmaaned = YearMonth.of(2024, Month.APRIL)
        val behandlingIdPerReformtidspunkt = UUID.randomUUID().toString()
        val melding = lagMeldingForVurdertLoependeYtelse(hendelseId, sakId, behandlingsmaaned)

        with(inspector.apply { sendTestMessage(melding.toJson()) }.inspektør) {
            size shouldBe 1
            field(0, EVENT_NAME_KEY).asText() shouldBe EventNames.ALDERSOVERGANG.name
            field(0, ALDERSOVERGANG_STEG_KEY).asText() shouldBe "VURDERT_LOEPENDE_YTELSE_OG_VILKAAR"
            field(0, ALDERSOVERGANG_TYPE_KEY).asText() shouldBe "BP20"
            field(0, ALDERSOVERGANG_ID_KEY).asText() shouldBe hendelseId.toString()
            field(0, DRYRUN).asBoolean() shouldBe false
            field(0, HENDELSE_DATA_KEY) shouldHaveSize 0
            assertThrows<IllegalArgumentException> { field(0, "yrkesskadefordel_pre_20240101") }
        }

        verify(exactly = 0) { vilkaarsvurderingService.harMigrertYrkesskadefordel(behandlingIdPerReformtidspunkt) }
    }

    private fun lagMeldingForVurdertLoependeYtelse(
        hendelseId: UUID,
        sakId: Long,
        behandlingsmaaned: YearMonth,
        behandlingIdPerReformtidspunkt: String? = null,
    ): JsonMessage {
        val hendelsedata = mutableMapOf<String, Any>()
        behandlingIdPerReformtidspunkt?.let {
            hendelsedata["loependeYtelse_januar2024_behandlingId"] = it
        }

        return JsonMessage.newMessage(
            EventNames.ALDERSOVERGANG.lagEventnameForType(),
            mapOf(
                ALDERSOVERGANG_STEG_KEY to "VURDERT_LOEPENDE_YTELSE",
                ALDERSOVERGANG_TYPE_KEY to "BP20",
                ALDERSOVERGANG_ID_KEY to hendelseId,
                SAK_ID_KEY to sakId,
                DATO_KEY to behandlingsmaaned.atDay(1),
                DRYRUN to false,
                HENDELSE_DATA_KEY to hendelsedata,
            ),
        )
    }
}
