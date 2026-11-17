package no.nav.etterlatte.modules.beregning.avkorting

import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.behandling.BehandlingService
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.oppgave.OppgaveKilde
import no.nav.etterlatte.libs.common.oppgave.OppgaveType
import no.nav.etterlatte.libs.common.periode.Periode
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import no.nav.etterlatte.libs.testdata.behandling.VirkningstidspunktTestData
import no.nav.etterlatte.modules.beregning.BeregningTestKontekst
import no.nav.etterlatte.modules.beregning.avkorting.Avkorting
import no.nav.etterlatte.modules.beregning.avkorting.AvkortingRepository
import no.nav.etterlatte.modules.beregning.avkorting.AvkortingTidligAlderspensjonService
import no.nav.etterlatte.modules.beregning.avkorting.Inntektsavkorting
import no.nav.etterlatte.modules.beregning.avkorting.OverstyrtInnvilgaMaanederAarsak
import no.nav.etterlatte.modules.beregning.regler.aarsoppgjoer
import no.nav.etterlatte.modules.beregning.regler.avkortinggrunnlag
import no.nav.etterlatte.modules.beregning.regler.behandling
import no.nav.etterlatte.oppgave.OppgaveService
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate
import java.time.Month
import java.time.YearMonth
import java.util.UUID

@ExtendWith(BeregningTestKontekst::class)
class AvkortingTidligAlderspensjonServiceTest {
    private val oppgaveService = mockk<OppgaveService>()
    private val behandlingService: BehandlingService = mockk()
    private val avkortingRepository: AvkortingRepository = mockk()

    private val service =
        AvkortingTidligAlderspensjonService(
            behandlingService,
            avkortingRepository,
            oppgaveService = oppgaveService,
        )

    @BeforeEach
    fun beforeEach() {
        clearAllMocks()
    }

    @AfterEach
    fun afterEach() {
        confirmVerified()
        clearAllMocks()
    }

    @Test
    fun `Skal opprette oppgave hvis opphoer ved tidlig alderspensjon foerstegangsbehandling innevaerede aar`() {
        val behandlingId = UUID.randomUUID()
        val brukerTokenInfo = mockk<BrukerTokenInfo>(relaxed = true)
        val fom = VirkningstidspunktTestData.virkningstidsunkt(YearMonth.of(2024, 1))

        val behandling =
            behandling(
                id = behandlingId,
                behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
                status = BehandlingStatus.IVERKSATT,
                virkningstidspunkt = fom,
                sak = SakId(123),
            )

        val grunnlag =
            avkortinggrunnlag(
                id = UUID.randomUUID(),
                periode = Periode(fom = fom.dato, tom = null),
                kilde = Grunnlagsopplysning.Saksbehandler("Saksbehandler01", Tidspunkt.now()),
                overstyrtInnvilgaMaanederAarsak = OverstyrtInnvilgaMaanederAarsak.TAR_UT_PENSJON_TIDLIG,
                innvilgaMaaneder = 3,
            )

        val inntektsavkorting = Inntektsavkorting(grunnlag = grunnlag)
        val aarsoppgjoer = aarsoppgjoer(aar = fom.dato.year, inntektsavkorting = listOf(inntektsavkorting))

        val avkorting = Avkorting(aarsoppgjoer = listOf(aarsoppgjoer))

        coEvery { behandlingService.hentDetaljertBehandling(behandlingId, brukerTokenInfo) } returns behandling
        coEvery { avkortingRepository.hentAvkorting(behandlingId) } returns avkorting

        coEvery {
            oppgaveService.opprettOppgave(
                sakId = behandling.sak,
                type = OppgaveType.GENERELL_OPPGAVE,
                merknad = any(),
                frist = any(),
                kilde = any(),
                referanse = any(),
            )
        } returns mockk()

        runBlocking {
            service.opprettOppgaveHvisTidligAlderspensjon(behandlingId, brukerTokenInfo)
        }

        val slotTidspunkt = slot<Tidspunkt>()
        coVerify(exactly = 1) {
            oppgaveService.opprettOppgave(
                sakId = behandling.sak,
                type = OppgaveType.GENERELL_OPPGAVE,
                merknad = any(),
                frist = capture(slotTidspunkt),
                kilde = OppgaveKilde.BEHANDLING,
                referanse = behandlingId.toString(),
            )
            avkortingRepository.hentAvkorting(behandlingId)
            behandlingService.hentDetaljertBehandling(behandlingId, brukerTokenInfo)
        }

        val oppgaveFrist = slotTidspunkt.captured.toLocalDate()
        oppgaveFrist shouldBe LocalDate.of(2024, Month.FEBRUARY, 1)
    }

    @Test
    fun `Skal opprette oppgave hvis opphoer ved tidlig alderspensjon foerstegangsbehandling neste aar`() {
        val behandlingId = UUID.randomUUID()
        val brukerTokenInfo = mockk<BrukerTokenInfo>(relaxed = true)
        val fom = VirkningstidspunktTestData.virkningstidsunkt(YearMonth.of(2025, 1))

        val behandling =
            behandling(
                id = behandlingId,
                behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
                status = BehandlingStatus.IVERKSATT,
                virkningstidspunkt = fom,
                sak = SakId(123),
            )

        val grunnlag =
            avkortinggrunnlag(
                id = UUID.randomUUID(),
                periode = Periode(fom = fom.dato, tom = null),
                kilde = Grunnlagsopplysning.Saksbehandler("Saksbehandler01", Tidspunkt.now()),
                overstyrtInnvilgaMaanederAarsak = OverstyrtInnvilgaMaanederAarsak.TAR_UT_PENSJON_TIDLIG,
                innvilgaMaaneder = 3,
            )

        val inntektsavkorting = Inntektsavkorting(grunnlag = grunnlag)
        val nesteAarsoppgjoer = aarsoppgjoer(aar = fom.dato.year, inntektsavkorting = listOf(inntektsavkorting))

        val foesteAarsoppjoer =
            aarsoppgjoer(
                aar = 2024,
                inntektsavkorting =
                    listOf(
                        Inntektsavkorting(
                            grunnlag =
                                avkortinggrunnlag(
                                    id = UUID.randomUUID(),
                                    periode = Periode(fom = YearMonth.of(2024, 1), tom = null),
                                    kilde = Grunnlagsopplysning.Saksbehandler("Saksbehandler01", Tidspunkt.now()),
                                    overstyrtInnvilgaMaanederAarsak = null,
                                    innvilgaMaaneder = 12,
                                ),
                        ),
                    ),
            )

        val avkorting = Avkorting(aarsoppgjoer = listOf(foesteAarsoppjoer, nesteAarsoppgjoer))

        coEvery { behandlingService.hentDetaljertBehandling(behandlingId, brukerTokenInfo) } returns behandling
        coEvery { avkortingRepository.hentAvkorting(behandlingId) } returns avkorting

        coEvery {
            oppgaveService.opprettOppgave(
                sakId = behandling.sak,
                type = OppgaveType.GENERELL_OPPGAVE,
                merknad = any(),
                frist = any(),
                kilde = any(),
                referanse = any(),
            )
        } returns mockk()

        runBlocking {
            service.opprettOppgaveHvisTidligAlderspensjon(behandlingId, brukerTokenInfo)
        }

        val slotTidspunkt = slot<Tidspunkt>()
        coVerify(exactly = 1) {
            oppgaveService.opprettOppgave(
                sakId = behandling.sak,
                type = OppgaveType.GENERELL_OPPGAVE,
                merknad = any(),
                frist = capture(slotTidspunkt),
                kilde = OppgaveKilde.BEHANDLING,
                referanse = behandlingId.toString(),
            )
            avkortingRepository.hentAvkorting(behandlingId)
            behandlingService.hentDetaljertBehandling(behandlingId, brukerTokenInfo)
        }

        val oppgaveFrist = slotTidspunkt.captured.toLocalDate()
        oppgaveFrist shouldBe LocalDate.of(2025, Month.FEBRUARY, 1)
    }

    @Test
    fun `Skal opprette oppgave hvis opphoer ved tidlig alderspensjon revurdering`() {
        val behandlingId = UUID.randomUUID()
        val brukerTokenInfo = mockk<BrukerTokenInfo>(relaxed = true)
        val fom = VirkningstidspunktTestData.virkningstidsunkt(YearMonth.of(2024, 5))

        val behandling =
            behandling(
                id = behandlingId,
                behandlingType = BehandlingType.REVURDERING,
                status = BehandlingStatus.IVERKSATT,
                virkningstidspunkt = fom,
                sak = SakId(123),
            )

        val grunnlag =
            avkortinggrunnlag(
                id = UUID.randomUUID(),
                periode = Periode(fom = fom.dato, tom = null),
                kilde = Grunnlagsopplysning.Saksbehandler("Saksbehandler01", Tidspunkt.now()),
                overstyrtInnvilgaMaanederAarsak = OverstyrtInnvilgaMaanederAarsak.TAR_UT_PENSJON_TIDLIG,
                innvilgaMaaneder = 3,
            )

        val inntektsavkorting = Inntektsavkorting(grunnlag = grunnlag)
        val aarsoppgjoer =
            aarsoppgjoer(aar = fom.dato.year, fom = fom.dato, inntektsavkorting = listOf(inntektsavkorting))

        val avkorting = Avkorting(aarsoppgjoer = listOf(aarsoppgjoer))

        coEvery { behandlingService.hentDetaljertBehandling(behandlingId, brukerTokenInfo) } returns behandling
        coEvery { avkortingRepository.hentAvkorting(behandlingId) } returns avkorting

        coEvery {
            oppgaveService.opprettOppgave(
                sakId = behandling.sak,
                type = OppgaveType.GENERELL_OPPGAVE,
                merknad = any(),
                frist = any(),
                kilde = any(),
                referanse = any(),
            )
        } returns mockk()

        runBlocking {
            service.opprettOppgaveHvisTidligAlderspensjon(behandlingId, brukerTokenInfo)
        }

        val slotTidspunkt = slot<Tidspunkt>()
        coVerify(exactly = 1) {
            oppgaveService.opprettOppgave(
                sakId = behandling.sak,
                type = OppgaveType.GENERELL_OPPGAVE,
                merknad = any(),
                frist = capture(slotTidspunkt),
                kilde = OppgaveKilde.BEHANDLING,
                referanse = behandlingId.toString(),
            )
            avkortingRepository.hentAvkorting(behandlingId)
            behandlingService.hentDetaljertBehandling(behandlingId, brukerTokenInfo)
        }

        val oppgaveFrist = slotTidspunkt.captured.toLocalDate()
        oppgaveFrist shouldBe LocalDate.of(2024, Month.JUNE, 1)
    }
}
