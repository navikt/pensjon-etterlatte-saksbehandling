package avkorting

import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.avkorting.AvkortingService
import no.nav.etterlatte.avkorting.OverstyrtInnvilgaMaanederAarsak
import no.nav.etterlatte.avkorting.inntektsjustering.MottattInntektsjusteringService
import no.nav.etterlatte.avkorting.toDto
import no.nav.etterlatte.beregning.regler.avkortinggrunnlag
import no.nav.etterlatte.beregning.regler.bruker
import no.nav.etterlatte.libs.common.beregning.AvkortingFrontendDto
import no.nav.etterlatte.libs.common.beregning.AvkortingGrunnlagLagreDto
import no.nav.etterlatte.libs.common.beregning.MottattInntektsjusteringAvkortigRequest
import no.nav.etterlatte.libs.common.periode.Periode
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.inntektsjustering.MottattInntektsjustering
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.UUID

class MottattInntektsjusteringServiceTest {
    private val avkortingService: AvkortingService =
        mockk {
            coEvery { tilstandssjekk(any(), any()) } just runs
        }
    private val service = MottattInntektsjusteringService(avkortingService)

    @Test
    fun `Mottatt inntektsjustering skal erstatte eksisterende inntekt`() {
        val request =
            MottattInntektsjusteringAvkortigRequest(
                behandlingId = UUID.randomUUID(),
                virkningstidspunkt = YearMonth.of(2025, 1),
                mottattInntektsjustering =
                    MottattInntektsjustering(
                        SakId(123L),
                        UUID.randomUUID(),
                        "123",
                        LocalDateTime.now(),
                        2025,
                        100,
                        100,
                        100,
                        100,
                        null,
                    ),
            )
        val eksisterendeInntekt =
            avkortinggrunnlag(
                periode = Periode(fom = request.virkningstidspunkt, tom = null),
                inntektTom = 300,
            )

        val inntektSomLagres = slot<List<AvkortingGrunnlagLagreDto>>()

        coEvery { avkortingService.hentOpprettEllerReberegnAvkorting(request.behandlingId, bruker) } returns
            AvkortingFrontendDto(
                redigerbareInntekter = listOf(eksisterendeInntekt.toDto()),
                avkortingGrunnlag = emptyList(),
                avkortetYtelse = emptyList(),
            )

        coEvery {
            avkortingService.beregnAvkortingMedNyeGrunnlag(
                any(),
                capture(inntektSomLagres),
                any(),
            )
        } returns mockk()
        every { avkortingService.hentAvkorting(any()) } returns mockk()

        runBlocking {
            service.opprettAvkortingMedBrukeroppgittInntekt(request, bruker)
        }

        coVerify {
            avkortingService.beregnAvkortingMedNyeGrunnlag(request.behandlingId, inntektSomLagres.captured, bruker)
        }
        verify { avkortingService.hentAvkorting(request.behandlingId) }

        with(inntektSomLagres.captured[0]) {
            id shouldBe eksisterendeInntekt.id
            inntektTom shouldBe 300
            fratrekkInnAar shouldBe 0
            inntektUtlandTom shouldBe 100
            fratrekkInnAarUtland shouldBe 0
            spesifikasjon shouldBe "Mottatt inntekt fra bruker gjennom selvbetjening"
            fom shouldBe request.virkningstidspunkt
            overstyrtInnvilgaMaaneder shouldBe null
        }
    }

    @Test
    fun `Mottatt inntektsjustering skal sette overstyre innvilga maaneder hvis tidlig alderspenjon`() {
        val request =
            MottattInntektsjusteringAvkortigRequest(
                behandlingId = UUID.randomUUID(),
                virkningstidspunkt = YearMonth.of(2025, 1),
                mottattInntektsjustering =
                    MottattInntektsjustering(
                        SakId(123L),
                        UUID.randomUUID(),
                        "123",
                        LocalDateTime.now(),
                        2025,
                        100,
                        100,
                        100,
                        100,
                        YearMonth.of(2025, 6),
                    ),
            )
        val eksisterendeInntekt =
            avkortinggrunnlag(
                periode = Periode(fom = request.virkningstidspunkt, tom = null),
                inntektTom = 300,
                innvilgaMaaneder = 12,
            )

        val inntektSomLagres = slot<List<AvkortingGrunnlagLagreDto>>()

        coEvery {
            avkortingService.hentOpprettEllerReberegnAvkorting(
                request.behandlingId,
                bruker,
            )
        } returns
            AvkortingFrontendDto(
                redigerbareInntekter = listOf(eksisterendeInntekt.toDto()),
                avkortingGrunnlag = emptyList(),
                avkortetYtelse = emptyList(),
            )
        coEvery {
            avkortingService.beregnAvkortingMedNyeGrunnlag(
                any(),
                capture(inntektSomLagres),
                any(),
            )
        } returns mockk()
        every { avkortingService.hentAvkorting(any()) } returns mockk()

        runBlocking {
            service.opprettAvkortingMedBrukeroppgittInntekt(request, bruker)
        }

        coVerify {
            avkortingService.beregnAvkortingMedNyeGrunnlag(request.behandlingId, inntektSomLagres.captured, bruker)
        }
        verify { avkortingService.hentAvkorting(request.behandlingId) }

        with(inntektSomLagres.captured[0]) {
            overstyrtInnvilgaMaaneder?.antall shouldBe 5
            overstyrtInnvilgaMaaneder?.begrunnelse shouldBe "Bruker har oppgitt tidlig alderspensjon i inntektsjusteringskjema"
            overstyrtInnvilgaMaaneder?.aarsak shouldBe OverstyrtInnvilgaMaanederAarsak.TAR_UT_PENSJON_TIDLIG.name
        }
    }
}
