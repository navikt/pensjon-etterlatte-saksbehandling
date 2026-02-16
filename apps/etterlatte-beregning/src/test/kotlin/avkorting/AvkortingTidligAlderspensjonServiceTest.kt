package avkorting

import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.avkorting.Avkorting
import no.nav.etterlatte.avkorting.AvkortingRepository
import no.nav.etterlatte.avkorting.AvkortingTidligAlderspensjonService
import no.nav.etterlatte.avkorting.Inntektsavkorting
import no.nav.etterlatte.avkorting.OverstyrtInnvilgaMaanederAarsak
import no.nav.etterlatte.beregning.regler.aarsoppgjoer
import no.nav.etterlatte.beregning.regler.avkortinggrunnlag
import no.nav.etterlatte.beregning.regler.behandling
import no.nav.etterlatte.klienter.BehandlingKlient
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
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.Month
import java.time.YearMonth
import java.util.UUID

class AvkortingTidligAlderspensjonServiceTest {
    private val behandlingKlient: BehandlingKlient = mockk()
    private val avkortingRepository: AvkortingRepository = mockk()

    private val service =
        AvkortingTidligAlderspensjonService(
            behandlingKlient,
            avkortingRepository,
        )

    @BeforeEach
    fun beforeEach() {
        clearAllMocks()
        coEvery { behandlingKlient.avkort(any(), any(), any()) } returns true
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

        coEvery { behandlingKlient.hentBehandling(behandlingId, brukerTokenInfo) } returns behandling
        coEvery { avkortingRepository.hentAvkorting(behandlingId) } returns avkorting

        coEvery {
            behandlingKlient.opprettOppgave(
                behandling.sak,
                brukerTokenInfo,
                OppgaveType.GENERELL_OPPGAVE,
                any(),
                any(),
                any(),
                any(),
            )
        } returns Unit

        runBlocking {
            service.opprettOppgaveHvisTidligAlderspensjon(behandlingId, brukerTokenInfo)
        }

        val slotTidspunkt = slot<Tidspunkt>()
        coVerify(exactly = 1) {
            behandlingKlient.opprettOppgave(
                behandling.sak,
                brukerTokenInfo,
                OppgaveType.GENERELL_OPPGAVE,
                any(),
                capture(slotTidspunkt),
                OppgaveKilde.BEHANDLING,
                behandlingId.toString(),
            )
            avkortingRepository.hentAvkorting(behandlingId)
            behandlingKlient.hentBehandling(behandlingId, brukerTokenInfo)
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

        coEvery { behandlingKlient.hentBehandling(behandlingId, brukerTokenInfo) } returns behandling
        coEvery { avkortingRepository.hentAvkorting(behandlingId) } returns avkorting

        coEvery {
            behandlingKlient.opprettOppgave(
                behandling.sak,
                brukerTokenInfo,
                OppgaveType.GENERELL_OPPGAVE,
                any(),
                any(),
                any(),
                any(),
            )
        } returns Unit

        runBlocking {
            service.opprettOppgaveHvisTidligAlderspensjon(behandlingId, brukerTokenInfo)
        }

        val slotTidspunkt = slot<Tidspunkt>()
        coVerify(exactly = 1) {
            behandlingKlient.opprettOppgave(
                behandling.sak,
                brukerTokenInfo,
                OppgaveType.GENERELL_OPPGAVE,
                any(),
                capture(slotTidspunkt),
                OppgaveKilde.BEHANDLING,
                behandlingId.toString(),
            )
            avkortingRepository.hentAvkorting(behandlingId)
            behandlingKlient.hentBehandling(behandlingId, brukerTokenInfo)
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

        coEvery { behandlingKlient.hentBehandling(behandlingId, brukerTokenInfo) } returns behandling
        coEvery { avkortingRepository.hentAvkorting(behandlingId) } returns avkorting

        coEvery {
            behandlingKlient.opprettOppgave(
                behandling.sak,
                brukerTokenInfo,
                OppgaveType.GENERELL_OPPGAVE,
                any(),
                any(),
                any(),
                any(),
            )
        } returns Unit

        runBlocking {
            service.opprettOppgaveHvisTidligAlderspensjon(behandlingId, brukerTokenInfo)
        }

        val slotTidspunkt = slot<Tidspunkt>()
        coVerify(exactly = 1) {
            behandlingKlient.opprettOppgave(
                behandling.sak,
                brukerTokenInfo,
                OppgaveType.GENERELL_OPPGAVE,
                any(),
                capture(slotTidspunkt),
                OppgaveKilde.BEHANDLING,
                behandlingId.toString(),
            )
            avkortingRepository.hentAvkorting(behandlingId)
            behandlingKlient.hentBehandling(behandlingId, brukerTokenInfo)
        }

        val oppgaveFrist = slotTidspunkt.captured.toLocalDate()
        oppgaveFrist shouldBe LocalDate.of(2024, Month.JUNE, 1)
    }
}
