package no.nav.etterlatte

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.etterlatte.TidshendelseService.TidshendelserJobbType
import no.nav.etterlatte.TidshendelseService.TidshendelserJobbType.AO_BP20
import no.nav.etterlatte.TidshendelseService.TidshendelserJobbType.OMS_DOED_3AAR
import no.nav.etterlatte.funksjonsbrytere.DummyFeatureToggleService
import no.nav.etterlatte.libs.common.behandling.Omregningshendelse
import no.nav.etterlatte.libs.common.behandling.Prosesstype
import no.nav.etterlatte.libs.common.behandling.Revurderingaarsak
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.oppgave.OppgaveType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Month
import java.time.YearMonth
import java.util.UUID

class TidshendelseServiceTest {
    private val featureToggleService = DummyFeatureToggleService()
    private val behandlingService = mockk<BehandlingService>()
    private val tidshendelseService = TidshendelseService(behandlingService, featureToggleService)
    private val opprettetOppgaveId = UUID.randomUUID()
    private val forrigeBehandlingId = UUID.randomUUID()
    private val behandlingId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        featureToggleService.settBryter(TidshendelserFeatureToggle.OpprettOppgaveForVarselbrevAktivitetsplikt, true)

        every { behandlingService.opprettOppgave(any(), any(), any(), any(), any()) } returns opprettetOppgaveId
    }

    @Test
    fun `skal opprette behandling og returnere dens id`() {
        val hendelseId = UUID.randomUUID()
        val sakId = 321L
        val behandlingsmaaned = YearMonth.of(2024, Month.APRIL)
        val frist = behandlingsmaaned.atEndOfMonth()

        val melding = lagMeldingForVurdertLoependeYtelse(hendelseId, sakId, behandlingsmaaned, dryRun = false)

        val omregningshendelse =
            Omregningshendelse(
                sakId = sakId,
                fradato = behandlingsmaaned.plusMonths(1).atDay(1),
                prosesstype = Prosesstype.AUTOMATISK,
                revurderingaarsak = Revurderingaarsak.ALDERSOVERGANG,
                oppgavefrist = frist,
            )

        every { behandlingService.opprettOmregning(omregningshendelse) } returns
            OpprettOmregningResponse(behandlingId, forrigeBehandlingId, sakType = SakType.BARNEPENSJON)

        tidshendelseService.haandterHendelse(TidshendelsePacket(melding)) shouldBe
            TidshendelseResult.OpprettetOmregning(behandlingId, forrigeBehandlingId)

        verify { behandlingService.opprettOmregning(omregningshendelse) }
    }

    @Test
    fun `haandterHendelseMedLoependeYtelse med type oms_doed_4mnd skal opprette oppgave`() {
        val behandlingId = UUID.randomUUID()
        val opprettetOppgave =
            tidshendelseService.haandterHendelse(
                TidshendelsePacket(
                    lagMeldingForVurdertLoependeYtelse(
                        sakId = 2,
                        type = TidshendelserJobbType.OMS_DOED_4MND,
                        behandlingsmaaned = YearMonth.of(2024, 1),
                        behandlingId = behandlingId,
                    ),
                ),
            ) as TidshendelseResult.OpprettetOppgave

        opprettetOppgave.opprettetOppgaveId shouldBe opprettetOppgaveId
        verify {
            behandlingService.opprettOppgave(
                sakId = 2,
                oppgaveType = OppgaveType.AKTIVITETSPLIKT,
                referanse = behandlingId.toString(),
                merknad = "Varselbrev om aktivitetsplikt OMS etter 4 mnd",
                frist = withArg { it shouldNotBe null },
            )
        }
        verify(exactly = 0) {
            behandlingService.opprettOmregning(any())
        }
    }

    @Test
    fun `skal ikke opprette omregning eller oppgave hvis BP20 og yrkesskadefordel`() {
        val sakId = 37465L
        val behandlingsmaaned = YearMonth.of(2024, Month.MARCH)

        val melding = lagMeldingForVurdertLoependeYtelse(sakId, behandlingsmaaned, type = AO_BP20)
        melding["yrkesskadefordel_pre_20240101"] = true

        tidshendelseService.haandterHendelse(TidshendelsePacket(melding))

        verify(exactly = 0) { behandlingService.opprettOmregning(any()) }
        verify(exactly = 0) { behandlingService.opprettOppgave(sakId, any(), any(), any(), any()) }
    }

    @Test
    fun `OMS tre aar siden doedsfall, skal ikke opprette oppgave hvis rett uten tidsbegrensning`() {
        val hendelseId = UUID.randomUUID()
        val sakId = 93L
        val behandlingsmaaned = YearMonth.of(2024, Month.APRIL)

        val melding = lagMeldingForVurdertLoependeYtelse(hendelseId, sakId, behandlingsmaaned, type = OMS_DOED_3AAR)
        melding["oms_rett_uten_tidsbegrensning"] = true

        tidshendelseService.haandterHendelse(TidshendelsePacket(melding))

        verify(exactly = 0) { behandlingService.opprettOppgave(sakId, any(), any(), "Aldersovergang", any()) }
    }

    @Test
    fun `skal ikke kalle tjeneste for aa opprette behandling hvis dry-run`() {
        val hendelseId = UUID.randomUUID()
        val sakId = 37465L
        val behandlingsmaaned = YearMonth.of(2024, Month.MARCH)

        val melding = lagMeldingForVurdertLoependeYtelse(hendelseId, sakId, behandlingsmaaned, dryRun = true)
        tidshendelseService.haandterHendelse(TidshendelsePacket(melding))

        verify(exactly = 0) { behandlingService.opprettOmregning(any()) }
        verify(exactly = 0) { behandlingService.opprettOppgave(any(), any(), any(), any()) }
    }

    @Test
    fun `skal opprette oppgave hvis opprett omregning feiler`() {
        val hendelseId = UUID.randomUUID()
        val sakId = 37465L
        val behandlingsmaaned = YearMonth.of(2024, Month.MARCH)
        every { behandlingService.opprettOmregning(any()) } throws RuntimeException("Feil ved opprettelse av omregning")
        every { behandlingService.opprettOppgave(any(), any(), any(), any(), any()) } returns opprettetOppgaveId

        val melding = lagMeldingForVurdertLoependeYtelse(hendelseId, sakId, behandlingsmaaned)
        tidshendelseService.haandterHendelse(TidshendelsePacket(melding))

        verify { behandlingService.opprettOmregning(any()) }
        verify {
            behandlingService.opprettOppgave(
                sakId = sakId,
                oppgaveType = OppgaveType.REVURDERING,
                referanse = null,
                merknad = "Aldersovergang v/20 år",
                frist = any(),
            )
        }
    }

    @Test
    fun `skal ikke opprette noe hvis feature toggle er av for oms aktivitetsplikt`() {
        val melding =
            lagMeldingForVurdertLoependeYtelse(
                sakId = 37465L,
                behandlingsmaaned = YearMonth.of(2024, Month.MARCH),
                type = TidshendelserJobbType.OMS_DOED_4MND,
            )
        featureToggleService.settBryter(
            TidshendelserFeatureToggle.OpprettOppgaveForVarselbrevAktivitetsplikt,
            false,
        )

        tidshendelseService.haandterHendelse(TidshendelsePacket(melding))

        verify(exactly = 0) { behandlingService.opprettOmregning(any()) }
        verify(exactly = 0) {
            behandlingService.opprettOppgave(any(), any(), any(), any(), any())
        }
    }
}
