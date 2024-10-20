package no.nav.etterlatte

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.etterlatte.behandling.randomSakId
import no.nav.etterlatte.behandling.sakId2
import no.nav.etterlatte.libs.common.behandling.Revurderingaarsak
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.oppgave.OppgaveType
import no.nav.etterlatte.libs.common.revurdering.AutomatiskRevurderingRequest
import no.nav.etterlatte.libs.common.revurdering.AutomatiskRevurderingResponse
import no.nav.etterlatte.libs.tidshendelser.JobbType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Month
import java.time.YearMonth
import java.util.UUID

class TidshendelseServiceTest {
    private val behandlingService = mockk<BehandlingService>()
    private val tidshendelseService = TidshendelseService(behandlingService)
    private val opprettetOppgaveId = UUID.randomUUID()
    private val forrigeBehandlingId = UUID.randomUUID()
    private val behandlingId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        every { behandlingService.opprettOppgave(any(), any(), any(), any(), any()) } returns opprettetOppgaveId
    }

    @Test
    fun `skal opprette behandling og returnere dens id`() {
        val hendelseId = UUID.randomUUID()
        val sakId = randomSakId()
        val behandlingsmaaned = YearMonth.of(2024, Month.APRIL)
        val frist = behandlingsmaaned.atEndOfMonth()

        val melding = lagMeldingForVurdertLoependeYtelse(hendelseId, sakId, behandlingsmaaned, dryRun = false)

        val revurderingRequest =
            AutomatiskRevurderingRequest(
                sakId = sakId,
                fraDato = behandlingsmaaned.plusMonths(1).atDay(1),
                revurderingAarsak = Revurderingaarsak.ALDERSOVERGANG,
                oppgavefrist = frist,
            )

        every { behandlingService.opprettAutomatiskRevurdering(revurderingRequest) } returns
            AutomatiskRevurderingResponse(behandlingId, forrigeBehandlingId, sakType = SakType.BARNEPENSJON)

        tidshendelseService.haandterHendelse(TidshendelsePacket(melding)) shouldBe
            TidshendelseResult.OpprettetOmregning(behandlingId, forrigeBehandlingId)

        verify { behandlingService.opprettAutomatiskRevurdering(revurderingRequest) }
    }

    @Test
    fun `haandterHendelseMedLoependeYtelse med type oms_doed_4mnd skal opprette oppgave`() {
        val behandlingId = UUID.randomUUID()
        val opprettetOppgave =
            tidshendelseService.haandterHendelse(
                TidshendelsePacket(
                    lagMeldingForVurdertLoependeYtelse(
                        sakId = sakId2,
                        type = JobbType.OMS_DOED_4MND,
                        behandlingsmaaned = YearMonth.of(2024, 1),
                        behandlingId = behandlingId,
                    ),
                ),
            ) as TidshendelseResult.OpprettetOppgave

        opprettetOppgave.opprettetOppgaveId shouldBe opprettetOppgaveId
        verify {
            behandlingService.opprettOppgave(
                sakId = sakId2,
                oppgaveType = OppgaveType.AKTIVITETSPLIKT,
                referanse = behandlingId.toString(),
                merknad = any(),
                frist = withArg { it shouldNotBe null },
            )
        }
        verify(exactly = 0) {
            behandlingService.opprettAutomatiskRevurdering(any())
        }
    }

    @Test
    fun `skal ikke opprette omregning eller oppgave hvis BP20 og yrkesskadefordel`() {
        val sakId = randomSakId()
        val behandlingsmaaned = YearMonth.of(2024, Month.MARCH)

        val melding = lagMeldingForVurdertLoependeYtelse(sakId, behandlingsmaaned, type = JobbType.AO_BP20)
        melding["yrkesskadefordel_pre_20240101"] = true

        tidshendelseService.haandterHendelse(TidshendelsePacket(melding))

        verify(exactly = 0) { behandlingService.opprettAutomatiskRevurdering(any()) }
        verify(exactly = 0) { behandlingService.opprettOppgave(sakId, any(), any(), any(), any()) }
    }

    @Test
    fun `OMS tre aar siden doedsfall, skal ikke opprette oppgave hvis rett uten tidsbegrensning`() {
        val hendelseId = UUID.randomUUID()
        val sakId = randomSakId()
        val behandlingsmaaned = YearMonth.of(2024, Month.APRIL)

        val melding = lagMeldingForVurdertLoependeYtelse(hendelseId, sakId, behandlingsmaaned, type = JobbType.OMS_DOED_3AAR)
        melding["oms_rett_uten_tidsbegrensning"] = true

        tidshendelseService.haandterHendelse(TidshendelsePacket(melding))

        verify(exactly = 0) { behandlingService.opprettOppgave(sakId, any(), any(), "Aldersovergang", any()) }
    }

    @Test
    fun `skal ikke kalle tjeneste for aa opprette behandling hvis dry-run`() {
        val hendelseId = UUID.randomUUID()
        val sakId = randomSakId()
        val behandlingsmaaned = YearMonth.of(2024, Month.MARCH)

        val melding = lagMeldingForVurdertLoependeYtelse(hendelseId, sakId, behandlingsmaaned, dryRun = true)
        tidshendelseService.haandterHendelse(TidshendelsePacket(melding))

        verify(exactly = 0) { behandlingService.opprettAutomatiskRevurdering(any()) }
        verify(exactly = 0) { behandlingService.opprettOppgave(any(), any(), any(), any()) }
    }

    @Test
    fun `skal opprette oppgave hvis opprett omregning feiler`() {
        val hendelseId = UUID.randomUUID()
        val sakId = randomSakId()
        val behandlingsmaaned = YearMonth.of(2024, Month.MARCH)
        every { behandlingService.opprettAutomatiskRevurdering(any()) } throws RuntimeException("Feil ved opprettelse av omregning")
        every { behandlingService.opprettOppgave(any(), any(), any(), any(), any()) } returns opprettetOppgaveId

        val melding = lagMeldingForVurdertLoependeYtelse(hendelseId, sakId, behandlingsmaaned)
        tidshendelseService.haandterHendelse(TidshendelsePacket(melding))

        verify { behandlingService.opprettAutomatiskRevurdering(any()) }
        verify {
            behandlingService.opprettOppgave(
                sakId = sakId,
                oppgaveType = OppgaveType.REVURDERING,
                referanse = null,
                merknad = any(),
                frist = any(),
            )
        }
    }
}
