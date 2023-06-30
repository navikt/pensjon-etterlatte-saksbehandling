package no.nav.etterlatte.beregning

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.beregning.grunnlag.BeregningsGrunnlagService
import no.nav.etterlatte.beregning.regler.bruker
import no.nav.etterlatte.klienter.BehandlingKlientImpl
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.testdata.behandling.VirkningstidspunktTestData
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.YearMonth
import java.util.UUID.randomUUID

internal class BeregningServiceTest {

    private val behandlingKlient = mockk<BehandlingKlientImpl>()
    private val beregningRepository = mockk<BeregningRepository> {
        every { lagreEllerOppdaterBeregning(any()) } returns mockk()
    }
    private val beregnBarnepensjonService = mockk<BeregnBarnepensjonService>()
    private val beregnOmstillingsstoenadService = mockk<BeregnOmstillingsstoenadService>()
    private val beregningsGrunnlagService = mockk<BeregningsGrunnlagService>()
    private val beregningService: BeregningService = BeregningService(
        beregningRepository = beregningRepository,
        behandlingKlient = behandlingKlient,
        beregnBarnepensjonService = beregnBarnepensjonService,
        beregnOmstillingsstoenadService = beregnOmstillingsstoenadService,
        beregningsGrunnlagService = beregningsGrunnlagService
    )

    @Test
    fun `skal beregne barnepensjon og lagre resultatet hvis statussjekk i behandling passerer`() {
        val behandling = mockBehandling(SakType.BARNEPENSJON)

        coEvery { behandlingKlient.hentBehandling(any(), any()) } returns behandling
        coEvery { behandlingKlient.beregn(any(), any(), any()) } returns true
        coEvery { beregnBarnepensjonService.beregn(any(), any()) } returns mockk()

        runBlocking {
            beregningService.opprettBeregning(behandling.id, bruker)

            coVerify(exactly = 1) { behandlingKlient.beregn(any(), any(), false) }
            coVerify(exactly = 1) { behandlingKlient.beregn(any(), any(), true) }
            coVerify(exactly = 1) { beregnBarnepensjonService.beregn(any(), any()) }
            verify(exactly = 1) { beregningRepository.lagreEllerOppdaterBeregning(any()) }
        }
    }

    @Test
    fun `skal ikke beregne barnepensjon eller lagre noe hvis statussjekk i behandling ikke passerer`() {
        val behandling = mockBehandling(SakType.BARNEPENSJON)

        coEvery { behandlingKlient.hentBehandling(any(), any()) } returns behandling
        coEvery { behandlingKlient.beregn(any(), any(), any()) } returns false

        runBlocking {
            assertThrows<IllegalStateException> {
                beregningService.opprettBeregning(behandling.id, bruker)
            }

            coVerify(exactly = 1) { behandlingKlient.beregn(any(), any(), false) }
            coVerify(exactly = 0) { beregnBarnepensjonService.beregn(any(), any()) }
            verify(exactly = 0) { beregningRepository.lagreEllerOppdaterBeregning(any()) }
        }
    }

    @Test
    fun `skal beregne omstillingsstoenad og lagre resultatet hvis statussjekk i behandling passerer`() {
        val behandling = mockBehandling(SakType.OMSTILLINGSSTOENAD)

        coEvery { behandlingKlient.hentBehandling(any(), any()) } returns behandling
        coEvery { behandlingKlient.beregn(any(), any(), any()) } returns true
        coEvery { beregnOmstillingsstoenadService.beregn(any(), any()) } returns mockk()

        runBlocking {
            beregningService.opprettBeregning(behandling.id, bruker)

            coVerify(exactly = 1) { behandlingKlient.beregn(any(), any(), false) }
            coVerify(exactly = 1) { behandlingKlient.beregn(any(), any(), true) }
            coVerify(exactly = 1) { beregnOmstillingsstoenadService.beregn(any(), any()) }
            verify(exactly = 1) { beregningRepository.lagreEllerOppdaterBeregning(any()) }
        }
    }

    @Test
    fun `skal ikke beregne omstillingsstoenad eller lagre noe hvis statussjekk i behandling ikke passerer`() {
        val behandling = mockBehandling(SakType.OMSTILLINGSSTOENAD)

        coEvery { behandlingKlient.hentBehandling(any(), any()) } returns behandling
        coEvery { behandlingKlient.beregn(any(), any(), any()) } returns false

        runBlocking {
            assertThrows<IllegalStateException> {
                beregningService.opprettBeregning(behandling.id, bruker)
            }

            coVerify(exactly = 1) { behandlingKlient.beregn(any(), any(), false) }
            coVerify(exactly = 0) { beregnOmstillingsstoenadService.beregn(any(), any()) }
            verify(exactly = 0) { beregningRepository.lagreEllerOppdaterBeregning(any()) }
        }
    }

    private fun mockBehandling(type: SakType): DetaljertBehandling =
        mockk<DetaljertBehandling>().apply {
            every { id } returns randomUUID()
            every { sak } returns 1
            every { sakType } returns type
            every { behandlingType } returns BehandlingType.FØRSTEGANGSBEHANDLING
            every { virkningstidspunkt } returns VirkningstidspunktTestData.virkningstidsunkt(YearMonth.of(2023, 1))
        }
}