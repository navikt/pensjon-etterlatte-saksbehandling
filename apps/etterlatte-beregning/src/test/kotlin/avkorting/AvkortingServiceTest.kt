package avkorting

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.avkorting.AvkortetYtelse
import no.nav.etterlatte.avkorting.Avkorting
import no.nav.etterlatte.avkorting.AvkortingGrunnlag
import no.nav.etterlatte.avkorting.AvkortingRepository
import no.nav.etterlatte.avkorting.AvkortingService
import no.nav.etterlatte.avkorting.Avkortingsperiode
import no.nav.etterlatte.avkorting.InntektAvkortingService
import no.nav.etterlatte.beregning.BeregningService
import no.nav.etterlatte.beregning.regler.avkortetYtelse
import no.nav.etterlatte.beregning.regler.avkorting
import no.nav.etterlatte.beregning.regler.avkortinggrunnlag
import no.nav.etterlatte.beregning.regler.avkortingsperiode
import no.nav.etterlatte.beregning.regler.behandling
import no.nav.etterlatte.beregning.regler.beregning
import no.nav.etterlatte.beregning.regler.bruker
import no.nav.etterlatte.klienter.BehandlingKlient
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.beregning.Beregningsperiode
import no.nav.etterlatte.libs.common.periode.Periode
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.YearMonth
import java.util.UUID

internal class AvkortingServiceTest {

    private val behandlingKlient: BehandlingKlient = mockk()
    private val inntektAvkortingService: InntektAvkortingService = mockk()
    private val avkortingRepository: AvkortingRepository = mockk()
    private val beregningService: BeregningService = mockk()
    private val service =
        AvkortingService(behandlingKlient, inntektAvkortingService, avkortingRepository, beregningService)

    @BeforeEach
    fun beforeEach() {
        clearAllMocks()
        coEvery { behandlingKlient.beregn(any(), any(), any()) } returns true
    }

    @AfterEach
    fun afterEach() {
        confirmVerified()
    }

    @Test
    fun `skal hente avkorting`() {
        val behandlingId = UUID.randomUUID()
        val avkorting = avkorting()
        every { avkortingRepository.hentAvkorting(behandlingId) } returns avkorting

        runBlocking {
            service.hentAvkorting(behandlingId, bruker) shouldBe avkorting
        }
        coVerify {
            avkortingRepository.hentAvkorting(behandlingId)
        }
    }

    @Test
    fun `skal returnere null hvis avkorting ikke finnes`() {
        val behandlingId = UUID.randomUUID()
        every { avkortingRepository.hentAvkorting(behandlingId) } returns null
        val behandling = behandling(behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING)
        coEvery { behandlingKlient.hentBehandling(behandlingId, bruker) } returns behandling

        runBlocking {
            service.hentAvkorting(behandlingId, bruker) shouldBe null
        }

        coVerify {
            avkortingRepository.hentAvkorting(behandlingId)
            behandlingKlient.hentBehandling(behandlingId, bruker)
            behandling.behandlingType
        }
    }

    @Test
    fun `skal returnere ny beregnet avkorting med kopierte grunnlag fra forrige avkorting`() {
        val behandlingId = UUID.randomUUID()
        val sakId = 123L
        val virkningstidspunkt = YearMonth.of(2023, 1)
        val behandling = behandling(
            behandlingType = BehandlingType.REVURDERING,
            sak = sakId,
            virkningstidspunkt = YearMonth.of(2023, 1)
        )

        val forrigeBehandlingId = UUID.randomUUID()
        val forrigeBehandling = behandling(id = forrigeBehandlingId)
        val avkortinggrunnlag = avkortinggrunnlag(aarsinntekt = 100)
        val forrigeAvkorting = avkorting(avkortingGrunnlag = listOf(avkortinggrunnlag))
        val nyAvkortingsperiode = avkortingsperiode()
        val beregning = beregning()
        val nyAvkortetYtelse = avkortetYtelse()
        val lagretAvkorting = mockk<Avkorting>()

        every { avkortingRepository.hentAvkorting(behandlingId) } returns null
        coEvery { behandlingKlient.hentBehandling(any(), any()) } returns behandling
        coEvery { behandlingKlient.hentSisteIverksatteBehandling(any(), any()) } returns forrigeBehandling
        every { avkortingRepository.hentAvkorting(forrigeBehandlingId) } returns forrigeAvkorting
        every { inntektAvkortingService.beregnInntektsavkorting(any(), any()) } returns listOf(nyAvkortingsperiode)
        every { beregningService.hentBeregningNonnull(any()) } returns beregning
        every { inntektAvkortingService.beregnAvkortetYtelse(any(), any(), any()) } returns listOf(nyAvkortetYtelse)
        every { avkortingRepository.lagreAvkorting(any(), any()) } returns lagretAvkorting
        coEvery { behandlingKlient.avkort(any(), any(), any()) } returns true

        runBlocking {
            service.hentAvkorting(behandlingId, bruker)
        }

        coVerify {
            avkortingRepository.hentAvkorting(behandlingId)
            behandlingKlient.hentBehandling(behandlingId, bruker)
            behandlingKlient.hentSisteIverksatteBehandling(sakId, bruker)
            avkortingRepository.hentAvkorting(forrigeBehandlingId)
            inntektAvkortingService.beregnInntektsavkorting(
                virkningstidspunkt,
                withArg {
                    with(it[0]) {
                        id shouldNotBe avkortinggrunnlag.id
                        aarsinntekt shouldBe avkortinggrunnlag.aarsinntekt
                    }
                }
            )
            beregningService.hentBeregningNonnull(behandlingId)
            inntektAvkortingService.beregnAvkortetYtelse(
                virkningstidspunkt,
                beregning.beregningsperioder,
                listOf(nyAvkortingsperiode)
            )
            avkortingRepository.lagreAvkorting(
                behandlingId,
                withArg {
                    with(it.avkortingGrunnlag[0]) {
                        id shouldNotBe avkortinggrunnlag.id
                        aarsinntekt shouldBe avkortinggrunnlag.aarsinntekt
                    }
                    it.avkortingsperioder shouldBe listOf(nyAvkortingsperiode)
                    it.avkortetYtelse shouldBe listOf(nyAvkortetYtelse)
                }
            )
            behandlingKlient.avkort(behandlingId, bruker, true)
        }
    }

    @Test
    fun `nytt avkortingsgrunnlag for ny avkorting og kjoere regler for avkorting og lagre resultat`() {
        val behandlingId = UUID.randomUUID()
        val virkningstidspunkt = YearMonth.of(2023, 1)
        val behandling = behandling(virkningstidspunkt = virkningstidspunkt)
        val nyttGrunnlag = mockk<AvkortingGrunnlag>()
        val nyAvkortingsperiode = mockk<Avkortingsperiode>()
        val nyAvkortetYtelse = mockk<AvkortetYtelse>()
        val beregning = mockk<Beregningsperiode>()
        val nyAvkorting = Avkorting(
            avkortingGrunnlag = listOf(nyttGrunnlag),
            avkortingsperioder = listOf(nyAvkortingsperiode),
            avkortetYtelse = listOf(nyAvkortetYtelse)
        )

        coEvery { behandlingKlient.hentBehandling(any(), any()) } returns behandling
        every { avkortingRepository.hentAvkorting(any()) } returns null
        coEvery { behandlingKlient.avkort(any(), any(), any()) } returns true
        every { inntektAvkortingService.beregnInntektsavkorting(any(), any()) } returns listOf(nyAvkortingsperiode)
        every { beregningService.hentBeregningNonnull(any()) } returns beregning(listOf(beregning))
        every { inntektAvkortingService.beregnAvkortetYtelse(any(), any(), any()) } returns listOf(nyAvkortetYtelse)
        every { avkortingRepository.lagreAvkorting(any(), any()) } returns nyAvkorting
        coEvery { behandlingKlient.avkort(any(), any(), any()) } returns true

        val result = runBlocking {
            service.lagreAvkorting(behandlingId, bruker, nyttGrunnlag)
        }

        result shouldBe nyAvkorting
        coVerify(exactly = 1) {
            behandlingKlient.avkort(behandlingId, bruker, false)
            behandlingKlient.hentBehandling(behandlingId, bruker)
            avkortingRepository.hentAvkorting(behandlingId)
            inntektAvkortingService.beregnInntektsavkorting(virkningstidspunkt, listOf(nyttGrunnlag))
            beregningService.hentBeregningNonnull(behandlingId)
            inntektAvkortingService.beregnAvkortetYtelse(
                virkningstidspunkt,
                listOf(beregning),
                listOf(nyAvkortingsperiode)
            )
            avkortingRepository.lagreAvkorting(behandlingId, nyAvkorting)
            behandlingKlient.avkort(behandlingId, bruker, true)
        }
    }

    @Test
    fun `endret avkortingsgrunnlag for eksisterende avkorting og kjoere regler for avkorting og lagre resultat`() {
        val behandlingId = UUID.randomUUID()
        val virkningstidspunkt = YearMonth.of(2023, 1)
        val behandling = behandling(virkningstidspunkt = virkningstidspunkt)
        val grunnlagId = UUID.randomUUID()
        val eksisterendeGrunnlag = avkortinggrunnlag(id = grunnlagId)
        val eksisterendeAvkortingsperiode = mockk<Avkortingsperiode>()
        val eksisterendeAvkortetYtelse = mockk<AvkortetYtelse>()

        val avkorting = Avkorting(
            avkortingGrunnlag = listOf(eksisterendeGrunnlag),
            avkortingsperioder = listOf(eksisterendeAvkortingsperiode),
            avkortetYtelse = listOf(eksisterendeAvkortetYtelse)
        )
        val endretGrunnlag = eksisterendeGrunnlag.copy(aarsinntekt = 100)
        val nyAvkortingsperiode = mockk<Avkortingsperiode>()
        val nyAvkortetYtelse = mockk<AvkortetYtelse>()
        val beregning = mockk<Beregningsperiode>()

        coEvery { behandlingKlient.hentBehandling(any(), any()) } returns behandling
        every { avkortingRepository.hentAvkorting(any()) } returns avkorting
        coEvery { behandlingKlient.avkort(any(), any(), any()) } returns true
        every { inntektAvkortingService.beregnInntektsavkorting(any(), any()) } returns listOf(nyAvkortingsperiode)
        every { beregningService.hentBeregningNonnull(any()) } returns beregning(listOf(beregning))
        every { inntektAvkortingService.beregnAvkortetYtelse(any(), any(), any()) } returns listOf(nyAvkortetYtelse)
        every { avkortingRepository.lagreAvkorting(any(), any()) } returns avkorting
        coEvery { behandlingKlient.avkort(any(), any(), any()) } returns true

        val result = runBlocking {
            service.lagreAvkorting(behandlingId, bruker, endretGrunnlag)
        }

        result shouldBe avkorting
        coVerify(exactly = 1) {
            behandlingKlient.avkort(behandlingId, bruker, false)
            behandlingKlient.hentBehandling(behandlingId, bruker)
            avkortingRepository.hentAvkorting(behandlingId)
            inntektAvkortingService.beregnInntektsavkorting(virkningstidspunkt, listOf(endretGrunnlag))
            beregningService.hentBeregningNonnull(behandlingId)
            inntektAvkortingService.beregnAvkortetYtelse(
                virkningstidspunkt,
                listOf(beregning),
                listOf(nyAvkortingsperiode)
            )
            avkortingRepository.lagreAvkorting(
                behandlingId,
                withArg {
                    it.avkortingGrunnlag shouldBe listOf(endretGrunnlag)
                    it.avkortingsperioder shouldBe listOf(nyAvkortingsperiode)
                    it.avkortetYtelse shouldBe listOf(nyAvkortetYtelse)
                }
            )
            behandlingKlient.avkort(behandlingId, bruker, true)
        }
    }

    @Test
    fun `nytt avkortingsgrunnlag for eksisterende avkorting og kjoere regler for avkorting og lagre resultat`() {
        val behandlingId = UUID.randomUUID()
        val virkningstidspunkt = YearMonth.of(2023, 1)
        val behandling = behandling(virkningstidspunkt = virkningstidspunkt)
        val eksisterendeGrunnlag = avkortinggrunnlag(
            periode = Periode(fom = YearMonth.of(2023, 1), tom = null)
        )
        val eksisterendeAvkortingsperiode = mockk<Avkortingsperiode>()
        val eksisterendeAvkortetYtelse = mockk<AvkortetYtelse>()
        val avkorting = Avkorting(
            avkortingGrunnlag = listOf(eksisterendeGrunnlag),
            avkortingsperioder = listOf(eksisterendeAvkortingsperiode),
            avkortetYtelse = listOf(eksisterendeAvkortetYtelse)
        )
        val nyttGrunnlag = avkortinggrunnlag(
            periode = Periode(fom = YearMonth.of(2023, 4), tom = null)
        )
        val nyeAvkortingsperioder = mockk<Avkortingsperiode>()
        val beregninger = mockk<Beregningsperiode>()
        val nyeAvkortetYtelser = mockk<AvkortetYtelse>()

        coEvery { behandlingKlient.hentBehandling(any(), any()) } returns behandling
        every { avkortingRepository.hentAvkorting(any()) } returns avkorting
        coEvery { behandlingKlient.avkort(any(), any(), any()) } returns true
        every { inntektAvkortingService.beregnInntektsavkorting(any(), any()) } returns listOf(nyeAvkortingsperioder)
        every { beregningService.hentBeregningNonnull(any()) } returns beregning(listOf(beregninger))
        every { inntektAvkortingService.beregnAvkortetYtelse(any(), any(), any()) } returns listOf(nyeAvkortetYtelser)
        every { avkortingRepository.lagreAvkorting(any(), any()) } returns avkorting
        coEvery { behandlingKlient.avkort(any(), any(), any()) } returns true

        val result = runBlocking {
            service.lagreAvkorting(behandlingId, bruker, nyttGrunnlag)
        }

        result shouldBe avkorting

        coVerify(exactly = 1) {
            behandlingKlient.avkort(behandlingId, bruker, false)
            behandlingKlient.hentBehandling(behandlingId, bruker)
            avkortingRepository.hentAvkorting(behandlingId)
            inntektAvkortingService.beregnInntektsavkorting(
                virkningstidspunkt,
                withArg {
                    it[0].periode.tom shouldBe YearMonth.of(2023, 3)
                    it[1].periode.tom shouldBe null
                }
            )
            beregningService.hentBeregningNonnull(behandlingId)
            inntektAvkortingService.beregnAvkortetYtelse(
                virkningstidspunkt,
                listOf(beregninger),
                listOf(nyeAvkortingsperioder)
            )
            avkortingRepository.lagreAvkorting(
                behandlingId,
                withArg {
                    it.avkortingGrunnlag[0].periode.tom shouldBe YearMonth.of(2023, 3)
                    it.avkortingGrunnlag[1].periode.tom shouldBe null
                    it.avkortingsperioder shouldBe listOf(nyeAvkortingsperioder)
                    it.avkortetYtelse shouldBe listOf(nyeAvkortetYtelser)
                }
            )
            behandlingKlient.avkort(behandlingId, bruker, true)
        }
    }

    @Test
    fun `skal feile ved lagring av avkortinggrunnlag hvis behandling er i feil tilstand`() {
        val behandlingId = UUID.randomUUID()
        coEvery { behandlingKlient.avkort(behandlingId, bruker, false) } returns false

        runBlocking {
            assertThrows<Exception> {
                service.lagreAvkorting(behandlingId, bruker, avkortinggrunnlag())
            }
        }

        coVerify {
            behandlingKlient.avkort(behandlingId, bruker, false)
        }
    }
}