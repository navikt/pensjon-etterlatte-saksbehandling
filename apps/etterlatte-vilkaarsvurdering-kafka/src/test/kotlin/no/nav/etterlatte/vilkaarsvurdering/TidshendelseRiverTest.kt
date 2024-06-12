package no.nav.etterlatte.vilkaarsvurdering

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.ktor.client.statement.HttpResponse
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.etterlatte.libs.common.rapidsandrivers.EVENT_NAME_KEY
import no.nav.etterlatte.rapidsandrivers.TIDSHENDELSE_ID_KEY
import no.nav.etterlatte.rapidsandrivers.TIDSHENDELSE_STEG_KEY
import no.nav.etterlatte.rapidsandrivers.TIDSHENDELSE_TYPE_KEY
import no.nav.etterlatte.rapidsandrivers.BEHANDLING_ID_KEY
import no.nav.etterlatte.rapidsandrivers.BEHANDLING_VI_OMREGNER_FRA_KEY
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
            field(0, EVENT_NAME_KEY).asText() shouldBe EventNames.TIDSHENDELSE.name
            field(0, TIDSHENDELSE_STEG_KEY).asText() shouldBe "VURDERT_LOEPENDE_YTELSE_OG_VILKAAR"
            field(0, TIDSHENDELSE_TYPE_KEY).asText() shouldBe "BP20"
            field(0, TIDSHENDELSE_ID_KEY).asText() shouldBe hendelseId.toString()
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
            field(0, EVENT_NAME_KEY).asText() shouldBe EventNames.TIDSHENDELSE.name
            field(0, TIDSHENDELSE_STEG_KEY).asText() shouldBe "VURDERT_LOEPENDE_YTELSE_OG_VILKAAR"
            field(0, TIDSHENDELSE_TYPE_KEY).asText() shouldBe "BP20"
            field(0, TIDSHENDELSE_ID_KEY).asText() shouldBe hendelseId.toString()
            field(0, DRYRUN).asBoolean() shouldBe false
            field(0, HENDELSE_DATA_KEY) shouldHaveSize 0
            assertThrows<IllegalArgumentException> { field(0, "yrkesskadefordel_pre_20240101") }
        }

        verify(exactly = 0) { vilkaarsvurderingService.harMigrertYrkesskadefordel(behandlingIdPerReformtidspunkt) }
    }

    @Test
    fun `skal sjekke status for vilkaar om rett uten tidsbegrensning`() {
        val hendelseId = UUID.randomUUID()
        val sakId = 321L
        val behandlingsmaaned = YearMonth.of(2024, Month.APRIL)
        val behandlingId = UUID.randomUUID().toString()
        val melding =
            lagMeldingForVurdertLoependeYtelse(
                hendelseId,
                sakId,
                behandlingsmaaned,
            )
        melding[TIDSHENDELSE_TYPE_KEY] = "OMS_DOED_3AAR"
        melding[HENDELSE_DATA_KEY] =
            mapOf(
                "loependeYtelse" to true,
                "loependeYtelse_behandlingId" to behandlingId,
            )

        every { vilkaarsvurderingService.harRettUtenTidsbegrensning(behandlingId) } returns true

        with(inspector.apply { sendTestMessage(melding.toJson()) }.inspektør) {
            size shouldBe 1
            field(0, EVENT_NAME_KEY).asText() shouldBe EventNames.TIDSHENDELSE.name
            field(0, TIDSHENDELSE_STEG_KEY).asText() shouldBe "VURDERT_LOEPENDE_YTELSE_OG_VILKAAR"
            field(0, TIDSHENDELSE_TYPE_KEY).asText() shouldBe "OMS_DOED_3AAR"
            field(0, TIDSHENDELSE_ID_KEY).asText() shouldBe hendelseId.toString()
            field(0, DRYRUN).asBoolean() shouldBe false
            field(0, HENDELSE_DATA_KEY) shouldHaveSize 2
            field(0, "oms_rett_uten_tidsbegrensning").asBoolean() shouldBe true
        }

        verify { vilkaarsvurderingService.harRettUtenTidsbegrensning(behandlingId) }
    }

    @Test
    fun `skal opprette vilkaarsvurdering og opphoere pga aldersovergang`() {
        val hendelseId = UUID.randomUUID()
        val sakId = 321L
        val behandlingsmaaned = YearMonth.of(2024, Month.APRIL)
        val behandlingIdLoepende = UUID.randomUUID()
        val behandlingId = UUID.randomUUID()
        val melding =
            lagMeldingForVurdertLoependeYtelse(
                hendelseId = hendelseId,
                sakId = sakId,
                behandlingsmaaned = behandlingsmaaned,
                steg = "BEHANDLING_OPPRETTET",
            ).also {
                it[BEHANDLING_ID_KEY] = behandlingId
                it[BEHANDLING_VI_OMREGNER_FRA_KEY] = behandlingIdLoepende
            }

        every { vilkaarsvurderingService.opphoerAldersovergang(behandlingId, behandlingIdLoepende) } returns mockk<HttpResponse>()

        with(inspector.apply { sendTestMessage(melding.toJson()) }.inspektør) {
            size shouldBe 1
            field(0, EVENT_NAME_KEY).asText() shouldBe EventNames.TIDSHENDELSE.name
            field(0, TIDSHENDELSE_STEG_KEY).asText() shouldBe "VILKAARSVURDERT"
            field(0, TIDSHENDELSE_TYPE_KEY).asText() shouldBe "BP20"
            field(0, TIDSHENDELSE_ID_KEY).asText() shouldBe hendelseId.toString()
        }

        verify { vilkaarsvurderingService.opphoerAldersovergang(behandlingId, behandlingIdLoepende) }
    }

    private fun lagMeldingForVurdertLoependeYtelse(
        hendelseId: UUID,
        sakId: Long,
        behandlingsmaaned: YearMonth,
        behandlingIdPerReformtidspunkt: String? = null,
        steg: String = "VURDERT_LOEPENDE_YTELSE",
    ): JsonMessage {
        val hendelsedata = mutableMapOf<String, Any>()
        behandlingIdPerReformtidspunkt?.let {
            hendelsedata["loependeYtelse_januar2024_behandlingId"] = it
        }

        return JsonMessage.newMessage(
            EventNames.TIDSHENDELSE.lagEventnameForType(),
            mapOf(
                TIDSHENDELSE_STEG_KEY to steg,
                TIDSHENDELSE_TYPE_KEY to "BP20",
                TIDSHENDELSE_ID_KEY to hendelseId,
                SAK_ID_KEY to sakId,
                DATO_KEY to behandlingsmaaned.atDay(1),
                DRYRUN to false,
                HENDELSE_DATA_KEY to hendelsedata,
            ),
        )
    }
}
