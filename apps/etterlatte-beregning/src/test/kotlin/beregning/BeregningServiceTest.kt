package no.nav.etterlatte.beregning

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.beregning.regler.bruker
import no.nav.etterlatte.klienter.BehandlingKlientImpl
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.beregning.OverstyrBeregningDTO
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.testdata.behandling.VirkningstidspunktTestData
import no.nav.etterlatte.sanksjon.SanksjonService
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.YearMonth
import java.util.UUID.randomUUID

internal class BeregningServiceTest {
    private val behandlingKlient = mockk<BehandlingKlientImpl>()
    private val beregningRepository = mockk<BeregningRepository>()
    private val beregnBarnepensjonService = mockk<BeregnBarnepensjonService>()
    private val beregnOmstillingsstoenadService = mockk<BeregnOmstillingsstoenadService>()
    private val beregnOverstyrBeregningService = mockk<BeregnOverstyrBeregningService>()
    private val sanksjonService = mockk<SanksjonService>()
    private val beregningService: BeregningService =
        BeregningService(
            beregningRepository = beregningRepository,
            behandlingKlient = behandlingKlient,
            beregnBarnepensjonService = beregnBarnepensjonService,
            beregnOmstillingsstoenadService = beregnOmstillingsstoenadService,
            beregnOverstyrBeregningService = beregnOverstyrBeregningService,
            sanksjonService = sanksjonService,
        )

    @Test
    fun `skal beregne barnepensjon og lagre resultatet hvis statussjekk i behandling passerer`() {
        val behandling = mockBehandling(SakType.BARNEPENSJON)
        val beregning = mockk<Beregning>()

        every { beregningRepository.lagreEllerOppdaterBeregning(any()) } returns beregning
        every { beregningRepository.hentOverstyrBeregning(any()) } returns null
        coEvery { behandlingKlient.hentBehandling(any(), any()) } returns behandling
        coEvery { behandlingKlient.kanBeregnes(any(), any(), any()) } returns true
        coEvery { beregnBarnepensjonService.beregn(any(), any()) } returns mockk()
        every { beregning.behandlingId } returns behandling.id
        every { beregning.copy(any(), any(), any(), any(), any(), any(), any()) } returns beregning

        runBlocking {
            beregningService.opprettBeregning(behandling.id, bruker)

            coVerify(exactly = 1) { behandlingKlient.kanBeregnes(any(), any(), false) }
            coVerify(exactly = 1) { behandlingKlient.kanBeregnes(any(), any(), true) }
            coVerify(exactly = 1) { beregnBarnepensjonService.beregn(any(), any()) }
            verify(exactly = 1) { beregningRepository.lagreEllerOppdaterBeregning(any()) }
        }
    }

    @Test
    fun `skal beregne barnepensjon og lagre resultatet hvis statussjekk i behandling passerer med manuelt beregning`() {
        val behandling = mockBehandling(SakType.BARNEPENSJON)
        val beregning = mockk<Beregning>()
        val overstyrBeregning =
            OverstyrBeregning(
                behandling.sak,
                "Test",
                Tidspunkt.now(),
            )

        every { beregningRepository.lagreEllerOppdaterBeregning(any()) } returns beregning
        every { beregningRepository.hentOverstyrBeregning(any()) } returns overstyrBeregning
        coEvery { behandlingKlient.hentBehandling(any(), any()) } returns behandling
        coEvery { behandlingKlient.kanBeregnes(any(), any(), any()) } returns true
        coEvery { beregnOverstyrBeregningService.beregn(any(), any(), any()) } returns mockk()
        every { beregning.behandlingId } returns behandling.id
        every { beregning.copy(any(), any(), any(), any(), any(), any(), overstyrBeregning) } returns beregning

        runBlocking {
            beregningService.opprettBeregning(behandling.id, bruker)

            coVerify(exactly = 1) { behandlingKlient.kanBeregnes(any(), any(), false) }
            coVerify(exactly = 1) { behandlingKlient.kanBeregnes(any(), any(), true) }
            coVerify(exactly = 1) { beregnOverstyrBeregningService.beregn(any(), any(), any()) }
            verify(exactly = 1) { beregningRepository.lagreEllerOppdaterBeregning(any()) }
        }
    }

    @Test
    fun `skal ikke beregne barnepensjon eller lagre noe hvis statussjekk i behandling ikke passerer`() {
        val behandling = mockBehandling(SakType.BARNEPENSJON)

        every { beregningRepository.lagreEllerOppdaterBeregning(any()) } returns mockk()
        coEvery { behandlingKlient.hentBehandling(any(), any()) } returns behandling
        coEvery { behandlingKlient.kanBeregnes(any(), any(), any()) } returns false

        runBlocking {
            assertThrows<IllegalStateException> {
                beregningService.opprettBeregning(behandling.id, bruker)
            }

            coVerify(exactly = 1) { behandlingKlient.kanBeregnes(any(), any(), false) }
            coVerify(exactly = 0) { beregnBarnepensjonService.beregn(any(), any()) }
            verify(exactly = 0) { beregningRepository.lagreEllerOppdaterBeregning(any()) }
        }
    }

    @Test
    fun `skal beregne omstillingsstoenad og lagre resultatet hvis statussjekk i behandling passerer`() {
        val behandling = mockBehandling(SakType.OMSTILLINGSSTOENAD)
        val beregning = mockk<Beregning>()

        every { beregningRepository.lagreEllerOppdaterBeregning(any()) } returns beregning
        every { beregningRepository.hentOverstyrBeregning(any()) } returns null
        coEvery { behandlingKlient.hentBehandling(any(), any()) } returns behandling
        coEvery { behandlingKlient.kanBeregnes(any(), any(), any()) } returns true
        coEvery { beregnOmstillingsstoenadService.beregn(any(), any()) } returns mockk()
        every { beregning.behandlingId } returns behandling.id
        every { beregning.copy(any(), any(), any(), any(), any(), any(), any()) } returns beregning

        runBlocking {
            beregningService.opprettBeregning(behandling.id, bruker)

            coVerify(exactly = 1) { behandlingKlient.kanBeregnes(any(), any(), false) }
            coVerify(exactly = 1) { behandlingKlient.kanBeregnes(any(), any(), true) }
            coVerify(exactly = 1) { beregnOmstillingsstoenadService.beregn(any(), any()) }
            verify(exactly = 1) { beregningRepository.lagreEllerOppdaterBeregning(any()) }
        }
    }

    @Test
    fun `skal beregne omstillingsstoenad og lagre resultatet hvis statussjekk i behandling passerermed manuelt beregning`() {
        val behandling = mockBehandling(SakType.OMSTILLINGSSTOENAD)
        val beregning = mockk<Beregning>()
        val overstyrBeregning =
            OverstyrBeregning(
                behandling.sak,
                "Test",
                Tidspunkt.now(),
            )

        every { beregningRepository.lagreEllerOppdaterBeregning(any()) } returns beregning
        every { beregningRepository.hentOverstyrBeregning(any()) } returns overstyrBeregning
        coEvery { behandlingKlient.hentBehandling(any(), any()) } returns behandling
        coEvery { behandlingKlient.kanBeregnes(any(), any(), any()) } returns true
        coEvery { beregnOverstyrBeregningService.beregn(any(), any(), any()) } returns mockk()
        every { beregning.behandlingId } returns behandling.id
        every { beregning.copy(any(), any(), any(), any(), any(), any(), overstyrBeregning) } returns beregning

        runBlocking {
            beregningService.opprettBeregning(behandling.id, bruker)

            coVerify(exactly = 1) { behandlingKlient.kanBeregnes(any(), any(), false) }
            coVerify(exactly = 1) { behandlingKlient.kanBeregnes(any(), any(), true) }
            coVerify(exactly = 1) { beregnOverstyrBeregningService.beregn(any(), any(), any()) }
            verify(exactly = 1) { beregningRepository.lagreEllerOppdaterBeregning(any()) }
        }
    }

    @Test
    fun `skal ikke beregne omstillingsstoenad eller lagre noe hvis statussjekk i behandling ikke passerer`() {
        val behandling = mockBehandling(SakType.OMSTILLINGSSTOENAD)

        coEvery { behandlingKlient.hentBehandling(any(), any()) } returns behandling
        coEvery { behandlingKlient.kanBeregnes(any(), any(), any()) } returns false

        runBlocking {
            assertThrows<IllegalStateException> {
                beregningService.opprettBeregning(behandling.id, bruker)
            }

            coVerify(exactly = 1) { behandlingKlient.kanBeregnes(any(), any(), false) }
            coVerify(exactly = 0) { beregnOmstillingsstoenadService.beregn(any(), any()) }
            verify(exactly = 0) { beregningRepository.lagreEllerOppdaterBeregning(any()) }
        }
    }

    @Test
    fun `skal ikke hente overstyr beregning der den ikke finnes`() {
        val behandling = mockBehandling(SakType.OMSTILLINGSSTOENAD)

        coEvery { behandlingKlient.hentBehandling(any(), any()) } returns behandling
        every { beregningRepository.hentOverstyrBeregning(any()) } returns null

        runBlocking {
            val overstyrBeregning = beregningService.hentOverstyrBeregningPaaBehandlingId(behandling.id, bruker)

            overstyrBeregning shouldBe null

            verify(exactly = 1) { beregningRepository.hentOverstyrBeregning(any()) }
        }
    }

    @Test
    fun `skal hente overstyr beregning der den finnes`() {
        val behandling = mockBehandling(SakType.OMSTILLINGSSTOENAD)

        coEvery { behandlingKlient.hentBehandling(any(), any()) } returns behandling
        every { beregningRepository.hentOverstyrBeregning(any()) } returns
            OverstyrBeregning(
                behandling.sak,
                "Test",
                Tidspunkt.now(),
            )

        runBlocking {
            val overstyrBeregning = beregningService.hentOverstyrBeregningPaaBehandlingId(behandling.id, bruker)

            overstyrBeregning shouldNotBe null

            overstyrBeregning?.sakId shouldBe behandling.sak
            overstyrBeregning?.beskrivelse shouldBe "Test"

            verify(exactly = 1) { beregningRepository.hentOverstyrBeregning(any()) }
        }
    }

    @Test
    fun `skal opprette overstyrt beregning der den ikke finnes`() {
        val behandling = mockBehandling(SakType.OMSTILLINGSSTOENAD)

        coEvery { behandlingKlient.hentBehandling(any(), any()) } returns behandling
        every { beregningRepository.hentOverstyrBeregning(any()) } returns null
        every { beregningRepository.opprettOverstyrBeregning(any()) } returnsArgument 0

        runBlocking {
            val overstyrBeregning =
                beregningService.opprettOverstyrBeregning(behandling.id, OverstyrBeregningDTO("Test"), bruker)

            overstyrBeregning shouldNotBe null

            overstyrBeregning?.sakId shouldBe behandling.sak
            overstyrBeregning?.beskrivelse shouldBe "Test"

            verify(exactly = 1) {
                beregningRepository.opprettOverstyrBeregning(any())
                beregningRepository.hentOverstyrBeregning(any())
            }
        }
    }

    @Test
    fun `skal kunne slette overstyrt beregning der den finnes`() {
        val behandling = mockBehandling(SakType.OMSTILLINGSSTOENAD)

        coEvery { behandlingKlient.hentBehandling(any(), any()) } returns behandling
        every { beregningRepository.hentOverstyrBeregning(any()) } returns null
        every { beregningRepository.opprettOverstyrBeregning(any()) } returnsArgument 0
        every { beregningRepository.deaktiverOverstyrtBeregning(any()) } just runs

        runBlocking {
            val overstyrBeregning =
                beregningService.opprettOverstyrBeregning(behandling.id, OverstyrBeregningDTO("Test"), bruker)

            overstyrBeregning shouldNotBe null

            overstyrBeregning?.sakId shouldBe behandling.sak
            overstyrBeregning?.beskrivelse shouldBe "Test"

            verify(exactly = 1) {
                beregningRepository.opprettOverstyrBeregning(any())
                beregningRepository.hentOverstyrBeregning(any())
            }

            beregningService.deaktiverOverstyrtberegning(behandling.id, bruker)
            val tomOverstyrt = beregningService.hentOverstyrBeregning(behandling)
            tomOverstyrt shouldBe null
        }
    }

    @Test
    fun `skal ikke overskrive overstyr beregning der den finnes`() {
        val behandling = mockBehandling(SakType.OMSTILLINGSSTOENAD)

        coEvery { behandlingKlient.hentBehandling(any(), any()) } returns behandling
        every { beregningRepository.hentOverstyrBeregning(any()) } returns
            OverstyrBeregning(
                behandling.sak,
                "Test",
                Tidspunkt.now(),
            )

        runBlocking {
            val overstyrBeregning =
                beregningService.opprettOverstyrBeregning(behandling.id, OverstyrBeregningDTO("Test 2"), bruker)

            overstyrBeregning shouldNotBe null

            overstyrBeregning?.sakId shouldBe behandling.sak
            overstyrBeregning?.beskrivelse shouldBe "Test"

            verify(exactly = 1) { beregningRepository.hentOverstyrBeregning(any()) }
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
