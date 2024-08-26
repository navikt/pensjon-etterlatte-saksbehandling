package no.nav.etterlatte

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import no.nav.etterlatte.TidshendelseService.TidshendelserJobbType.AO_BP20
import no.nav.etterlatte.TidshendelseService.TidshendelserJobbType.OMS_DOED_5AAR
import no.nav.etterlatte.libs.common.rapidsandrivers.EVENT_NAME_KEY
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.rapidsandrivers.BEHANDLING_ID_KEY
import no.nav.etterlatte.rapidsandrivers.BEHANDLING_VI_OMREGNER_FRA_KEY
import no.nav.etterlatte.rapidsandrivers.DATO_KEY
import no.nav.etterlatte.rapidsandrivers.DRYRUN
import no.nav.etterlatte.rapidsandrivers.EventNames
import no.nav.etterlatte.rapidsandrivers.HENDELSE_DATA_KEY
import no.nav.etterlatte.rapidsandrivers.SAK_ID_KEY
import no.nav.etterlatte.rapidsandrivers.TIDSHENDELSE_ID_KEY
import no.nav.etterlatte.rapidsandrivers.TIDSHENDELSE_STEG_KEY
import no.nav.etterlatte.rapidsandrivers.TIDSHENDELSE_TYPE_KEY
import no.nav.etterlatte.rapidsandrivers.asUUID
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Month
import java.time.YearMonth
import java.util.UUID

class TidshendelseRiverTest {
    private val behandlingService = mockk<BehandlingServiceImpl>()
    private val tidshendelseService = mockk<TidshendelseService>()
    private val inspector = TestRapid().apply { TidshendelseRiver(this, tidshendelseService) }

    private val opprettetOppgaveID = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        every {
            behandlingService.opprettOppgave(any(), any(), any(), any(), any())
        } returns opprettetOppgaveID
    }

    @Test
    fun `skal haandtere hendelse og sette oppgave id paa meldingen`() {
        val hendelseId = UUID.randomUUID()
        val sakId = 321L
        val behandlingsmaaned = YearMonth.of(2024, Month.APRIL)
        val melding = lagMeldingForVurdertLoependeYtelse(hendelseId, sakId, behandlingsmaaned)

        val packetSlot = slot<TidshendelsePacket>()
        every {
            tidshendelseService.haandterHendelse(capture(packetSlot))
        } returns
            TidshendelseResult.OpprettetOppgave(opprettetOppgaveID)

        with(inspector.apply { sendTestMessage(melding.toJson()) }.inspektør) {
            size shouldBe 1
            field(0, EVENT_NAME_KEY).asText() shouldBe EventNames.TIDSHENDELSE.name
            field(0, TIDSHENDELSE_STEG_KEY).asText() shouldBe "OPPGAVE_OPPRETTET"
            field(0, TIDSHENDELSE_TYPE_KEY).asText() shouldBe "AO_BP20"
            field(0, TIDSHENDELSE_ID_KEY).asUUID() shouldBe hendelseId
            field(0, DRYRUN).asBoolean() shouldBe false
            field(0, HENDELSE_DATA_KEY) shouldHaveSize 1
            field(0, HENDELSE_DATA_KEY)["opprettetOppgaveId"].asUUID() shouldBe opprettetOppgaveID
        }

        // Verifiser det som ble sendt til service
        packetSlot.captured.hendelseId shouldBeEqual hendelseId.toString()
        packetSlot.captured.sakId shouldBe sakId
        packetSlot.captured.jobbtype shouldBe AO_BP20
        packetSlot.captured.dryrun shouldBe false
        packetSlot.captured.behandlingsmaaned shouldBe behandlingsmaaned
        packetSlot.captured.behandlingId shouldBe null
        packetSlot.captured.harLoependeYtelse shouldBe true
        packetSlot.captured.harMigrertYrkesskadeFordel shouldBe false
        packetSlot.captured.harRettUtenTidsbegrensning shouldBe false
    }

    @Test
    fun `skal haandtere hendelse og sette omregning id paa meldingen`() {
        val hendelseId = UUID.randomUUID()
        val behandlingsmaaned = YearMonth.of(2024, Month.APRIL)
        val melding =
            lagMeldingForVurdertLoependeYtelse(
                hendelseId = hendelseId,
                sakId = 321L,
                behandlingsmaaned = behandlingsmaaned,
                type = OMS_DOED_5AAR,
            )
        val forrigeBehandlingId = UUID.randomUUID()
        val nyBehandlingId = UUID.randomUUID()

        val packetSlot = slot<TidshendelsePacket>()
        every {
            tidshendelseService.haandterHendelse(capture(packetSlot))
        } returns TidshendelseResult.OpprettetOmregning(nyBehandlingId, forrigeBehandlingId)

        with(inspector.apply { sendTestMessage(melding.toJson()) }.inspektør) {
            size shouldBe 1
            field(0, EVENT_NAME_KEY).asText() shouldBe EventNames.TIDSHENDELSE.name
            field(0, TIDSHENDELSE_STEG_KEY).asText() shouldBe "BEHANDLING_OPPRETTET"
            field(0, TIDSHENDELSE_TYPE_KEY).asText() shouldBe "OMS_DOED_5AAR"
            field(0, TIDSHENDELSE_ID_KEY).asUUID() shouldBe hendelseId
            field(0, DRYRUN).asBoolean() shouldBe false
            field(0, HENDELSE_DATA_KEY) shouldHaveSize 0
            field(0, BEHANDLING_ID_KEY).asUUID() shouldBe nyBehandlingId
            field(0, BEHANDLING_VI_OMREGNER_FRA_KEY).asUUID() shouldBe forrigeBehandlingId
        }

        // Verifiser det som ble sendt til service
        packetSlot.captured.hendelseId shouldBeEqual hendelseId.toString()
        packetSlot.captured.sakId shouldBe 321L
        packetSlot.captured.jobbtype shouldBe OMS_DOED_5AAR
        packetSlot.captured.dryrun shouldBe false
        packetSlot.captured.behandlingsmaaned shouldBe behandlingsmaaned
        packetSlot.captured.behandlingId shouldBe null
        packetSlot.captured.harLoependeYtelse shouldBe true
        packetSlot.captured.harMigrertYrkesskadeFordel shouldBe false
        packetSlot.captured.harRettUtenTidsbegrensning shouldBe false
    }

    @Test
    fun `skal haandtere hendelse som blir skippet`() {
        val hendelseId = UUID.randomUUID()
        val sakId = 321L
        val april2024 = YearMonth.of(2024, Month.APRIL)

        val melding = lagMeldingForVurdertLoependeYtelse(hendelseId, sakId, april2024, dryRun = false)
        melding["yrkesskadefordel_pre_20240101"] = true

        val packetSlot = slot<TidshendelsePacket>()
        every {
            tidshendelseService.haandterHendelse(capture(packetSlot))
        } returns TidshendelseResult.Skipped

        with(inspector.apply { sendTestMessage(melding.toJson()) }.inspektør) {
            size shouldBe 1
            field(0, EVENT_NAME_KEY).asText() shouldBe EventNames.TIDSHENDELSE.name
            field(0, TIDSHENDELSE_STEG_KEY).asText() shouldBe "HOPPET_OVER"
            field(0, TIDSHENDELSE_TYPE_KEY).asText() shouldBe "AO_BP20"
            field(0, TIDSHENDELSE_ID_KEY).asUUID() shouldBe hendelseId
            field(0, DRYRUN).asBoolean() shouldBe false
            field(0, HENDELSE_DATA_KEY) shouldHaveSize 0
        }

        // Verifiser det som ble sendt til service
        with(packetSlot.captured) {
            this.hendelseId shouldBeEqual hendelseId.toString()
            this.sakId shouldBe 321L
            this.jobbtype shouldBe AO_BP20
            this.dryrun shouldBe false
            this.behandlingsmaaned shouldBe april2024
            this.behandlingId shouldBe null
            this.harLoependeYtelse shouldBe true
            this.harMigrertYrkesskadeFordel shouldBe true
            this.harRettUtenTidsbegrensning shouldBe false
        }
    }
}

fun lagMeldingForVurdertLoependeYtelse(
    hendelseId: UUID = UUID.randomUUID(),
    sakId: SakId,
    behandlingsmaaned: YearMonth,
    type: TidshendelseService.TidshendelserJobbType = AO_BP20,
    dryRun: Boolean = false,
    behandlingId: UUID? = null,
): JsonMessage {
    val newMessage =
        JsonMessage.newMessage(
            EventNames.TIDSHENDELSE.lagEventnameForType(),
            emptyMap(),
        )

    newMessage[TIDSHENDELSE_STEG_KEY] = "VURDERT_LOEPENDE_YTELSE_OG_VILKAAR"
    newMessage[TIDSHENDELSE_TYPE_KEY] = type.name
    newMessage[TIDSHENDELSE_ID_KEY] = hendelseId
    newMessage[SAK_ID_KEY] = sakId
    newMessage[DATO_KEY] = behandlingsmaaned.atDay(1)
    newMessage[DRYRUN] = dryRun
    newMessage[HENDELSE_DATA_KEY] =
        listOfNotNull(
            "loependeYtelse" to true,
            behandlingId?.let { "loependeYtelse_behandlingId" to behandlingId },
        ).toMap()
    newMessage.interestedIn("yrkesskadefordel_pre_20240101")
    newMessage.interestedIn("oms_rett_uten_tidsbegrensning")

    return newMessage
}

fun lagMeldingForVurdertLoependeYtelse(
    sakId: SakId,
    behandlingsmaaned: YearMonth,
    type: TidshendelseService.TidshendelserJobbType = AO_BP20,
    dryRun: Boolean = false,
    behandlingId: UUID? = null,
): JsonMessage =
    lagMeldingForVurdertLoependeYtelse(
        hendelseId = UUID.randomUUID(),
        sakId = sakId,
        behandlingsmaaned = behandlingsmaaned,
        type = type,
        dryRun = dryRun,
        behandlingId = behandlingId,
    )
