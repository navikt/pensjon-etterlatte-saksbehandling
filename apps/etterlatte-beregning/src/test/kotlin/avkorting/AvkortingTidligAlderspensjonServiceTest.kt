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
    }

    @Test
    fun `Skal opprette oppgave hvis opphoer ved tidlig alderspensjon`() {
        val behandlingId = UUID.randomUUID()
        val brukerTokenInfo = mockk<BrukerTokenInfo>(relaxed = true)
        val fom = VirkningstidspunktTestData.virkningstidsunkt(YearMonth.of(2024, 1))

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
        val aarsoppgjoer = aarsoppgjoer(aar = fom.dato.year, inntektsavkorting = listOf(inntektsavkorting))

        val avkorting = Avkorting(aarsoppgjoer = listOf(aarsoppgjoer))

        coEvery { behandlingKlient.hentBehandling(behandlingId, brukerTokenInfo) } returns behandling
        coEvery { avkortingRepository.hentAvkorting(behandlingId) } returns avkorting

        coEvery {
            behandlingKlient.opprettOppgave(
                behandling.sak,
                brukerTokenInfo,
                OppgaveType.GENERELL_OPPGAVE,
                "Opphør av ytelse på grunn av alderspensjon.",
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
                "Opphør av ytelse på grunn av alderspensjon.",
                capture(slotTidspunkt),
                OppgaveKilde.BEHANDLING,
                behandlingId.toString(),
            )
            avkortingRepository.hentAvkorting(behandlingId)
            behandlingKlient.hentBehandling(behandlingId, brukerTokenInfo)
        }

        val oppgaveFrist = slotTidspunkt.captured.toLocalDate()
        oppgaveFrist.year shouldBe fom.dato.year
        oppgaveFrist.month shouldBe fom.dato.month + grunnlag.innvilgaMaaneder.minus(2L)
    }
}
