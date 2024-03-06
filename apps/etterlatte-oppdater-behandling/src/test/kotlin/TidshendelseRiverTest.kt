import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.etterlatte.BehandlingServiceImpl
import no.nav.etterlatte.TidshendelseRiver
import no.nav.etterlatte.libs.common.oppgave.OppgaveType
import no.nav.etterlatte.libs.common.rapidsandrivers.EVENT_NAME_KEY
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.rapidsandrivers.ALDERSOVERGANG_ID_KEY
import no.nav.etterlatte.rapidsandrivers.ALDERSOVERGANG_STEG_KEY
import no.nav.etterlatte.rapidsandrivers.ALDERSOVERGANG_TYPE_KEY
import no.nav.etterlatte.rapidsandrivers.DATO_KEY
import no.nav.etterlatte.rapidsandrivers.DRYRUN
import no.nav.etterlatte.rapidsandrivers.EventNames
import no.nav.etterlatte.rapidsandrivers.HENDELSE_DATA_KEY
import no.nav.etterlatte.rapidsandrivers.SAK_ID_KEY
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Test
import java.time.LocalTime
import java.time.Month
import java.time.YearMonth
import java.util.UUID

class TidshendelseRiverTest {
    private val behandlingService = mockk<BehandlingServiceImpl>()
    private val inspector = TestRapid().apply { TidshendelseRiver(this, behandlingService) }

    @Test
    fun `skal opprette oppgave og returnere dens id`() {
        val hendelseId = UUID.randomUUID()
        val sakId = 321L
        val behandlingsmaaned = YearMonth.of(2024, Month.APRIL)
        val frist = Tidspunkt.ofNorskTidssone(behandlingsmaaned.atEndOfMonth(), LocalTime.NOON)
        val nyOppgaveID = UUID.randomUUID()

        val melding = lagMeldingForVurdertLoependeYtelse(hendelseId, sakId, behandlingsmaaned, dryRun = false)

        every {
            behandlingService.opprettOppgave(
                sakId,
                OppgaveType.REVURDERING,
                any(),
                "Aldersovergang v/20 år",
                frist,
            )
        } returns nyOppgaveID

        with(inspector.apply { sendTestMessage(melding.toJson()) }.inspektør) {
            size shouldBe 1
            field(0, EVENT_NAME_KEY).asText() shouldBe EventNames.ALDERSOVERGANG.name
            field(0, ALDERSOVERGANG_STEG_KEY).asText() shouldBe "OPPGAVE_OPPRETTET"
            field(0, ALDERSOVERGANG_TYPE_KEY).asText() shouldBe "AO_BP20"
            field(0, ALDERSOVERGANG_ID_KEY).asText() shouldBe hendelseId.toString()
            field(0, DRYRUN).asBoolean() shouldBe false
            field(0, HENDELSE_DATA_KEY) shouldHaveSize 1
            field(0, HENDELSE_DATA_KEY)["opprettetOppgaveId"].asText() shouldBe nyOppgaveID.toString()
        }

        verify { behandlingService.opprettOppgave(sakId, OppgaveType.REVURDERING, any(), "Aldersovergang v/20 år", frist) }
    }

    @Test
    fun `skal ikke kalle tjeneste for aa opprette oppgave hvis dry-run`() {
        val hendelseId = UUID.randomUUID()
        val sakId = 37465L
        val behandlingsmaaned = YearMonth.of(2024, Month.MARCH)

        val melding = lagMeldingForVurdertLoependeYtelse(hendelseId, sakId, behandlingsmaaned, dryRun = true)

        with(inspector.apply { sendTestMessage(melding.toJson()) }.inspektør) {
            size shouldBe 1
            field(0, EVENT_NAME_KEY).asText() shouldBe EventNames.ALDERSOVERGANG.name
            field(0, ALDERSOVERGANG_STEG_KEY).asText() shouldBe "OPPGAVE_OPPRETTET"
            field(0, ALDERSOVERGANG_TYPE_KEY).asText() shouldBe "AO_BP20"
            field(0, ALDERSOVERGANG_ID_KEY).asText() shouldBe hendelseId.toString()
            field(0, DRYRUN).asBoolean() shouldBe true
            field(0, HENDELSE_DATA_KEY) shouldHaveSize 0
        }

        verify(exactly = 0) { behandlingService.opprettOppgave(sakId, any(), any(), "Aldersovergang", any()) }
    }

    @Test
    fun `skal ikke kalle tjeneste for aa opprette oppgave hvis BP20 og yrkesskadefordel`() {
        val hendelseId = UUID.randomUUID()
        val sakId = 37465L
        val behandlingsmaaned = YearMonth.of(2024, Month.MARCH)

        val melding = lagMeldingForVurdertLoependeYtelse(hendelseId, sakId, behandlingsmaaned)
        melding["yrkesskadefordel_pre_20240101"] = true

        with(inspector.apply { sendTestMessage(melding.toJson()) }.inspektør) {
            size shouldBe 1
            field(0, EVENT_NAME_KEY).asText() shouldBe EventNames.ALDERSOVERGANG.name
            field(0, ALDERSOVERGANG_STEG_KEY).asText() shouldBe "OPPGAVE_OPPRETTET"
            field(0, ALDERSOVERGANG_TYPE_KEY).asText() shouldBe "AO_BP20"
            field(0, ALDERSOVERGANG_ID_KEY).asText() shouldBe hendelseId.toString()
            field(0, DRYRUN).asBoolean() shouldBe false
            field(0, HENDELSE_DATA_KEY) shouldHaveSize 0
        }

        verify(exactly = 0) { behandlingService.opprettOppgave(sakId, any(), any(), "Aldersovergang", any()) }
    }

    @Test
    fun `OMS tre aar siden doedsfall, skal ikke opprette oppgave hvis rett uten tidsbegrensning`() {
        val hendelseId = UUID.randomUUID()
        val sakId = 93L
        val behandlingsmaaned = YearMonth.of(2024, Month.APRIL)

        val melding = lagMeldingForVurdertLoependeYtelse(hendelseId, sakId, behandlingsmaaned)
        melding[ALDERSOVERGANG_TYPE_KEY] = "OMS_DOED_3AAR"
        melding["oms_rett_uten_tidsbegrensning"] = true

        with(inspector.apply { sendTestMessage(melding.toJson()) }.inspektør) {
            size shouldBe 1
            field(0, EVENT_NAME_KEY).asText() shouldBe EventNames.ALDERSOVERGANG.name
            field(0, ALDERSOVERGANG_STEG_KEY).asText() shouldBe "OPPGAVE_OPPRETTET"
            field(0, ALDERSOVERGANG_TYPE_KEY).asText() shouldBe "OMS_DOED_3AAR"
            field(0, ALDERSOVERGANG_ID_KEY).asText() shouldBe hendelseId.toString()
            field(0, DRYRUN).asBoolean() shouldBe false
            field(0, HENDELSE_DATA_KEY) shouldHaveSize 0
        }

        verify(exactly = 0) { behandlingService.opprettOppgave(sakId, any(), any(), "Aldersovergang", any()) }
    }

    private fun lagMeldingForVurdertLoependeYtelse(
        hendelseId: UUID,
        sakId: Long,
        behandlingsmaaned: YearMonth,
        dryRun: Boolean = false,
    ) = JsonMessage.newMessage(
        EventNames.ALDERSOVERGANG.lagEventnameForType(),
        mapOf(
            ALDERSOVERGANG_STEG_KEY to "VURDERT_LOEPENDE_YTELSE_OG_VILKAAR",
            ALDERSOVERGANG_TYPE_KEY to "AO_BP20",
            ALDERSOVERGANG_ID_KEY to hendelseId,
            SAK_ID_KEY to sakId,
            DATO_KEY to behandlingsmaaned.atDay(1),
            DRYRUN to dryRun,
            HENDELSE_DATA_KEY to
                mapOf(
                    "loependeYtelse" to true,
                ),
        ),
    )
}
