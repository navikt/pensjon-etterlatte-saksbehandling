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
import no.nav.etterlatte.avkorting.Inntektsavkorting
import no.nav.etterlatte.avkorting.MottattInntektsjusteringService
import no.nav.etterlatte.avkorting.OverstyrtInnvilgaMaanederAarsak
import no.nav.etterlatte.beregning.regler.avkorting
import no.nav.etterlatte.beregning.regler.avkortinggrunnlag
import no.nav.etterlatte.beregning.regler.bruker
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
                aarsinntekt = 300,
            )

        val inntektSomLagres = slot<AvkortingGrunnlagLagreDto>()

        coEvery { avkortingService.hentOpprettEllerReberegnAvkorting(request.behandlingId, bruker) } returns
            avkorting(
                aar = 2025,
                inntektsavkorting = listOf(Inntektsavkorting(grunnlag = eksisterendeInntekt)),
            ).toFrontend(request.virkningstidspunkt)

        coEvery {
            avkortingService.beregnAvkortingMedNyttGrunnlag(
                any(),
                any(),
                capture(inntektSomLagres),
            )
        } returns mockk()
        every { avkortingService.hentAvkorting(any()) } returns mockk()

        runBlocking {
            service.opprettAvkortingMedBrukeroppgittInntekt(request, bruker)
        }

        coVerify {
            avkortingService.beregnAvkortingMedNyttGrunnlag(request.behandlingId, bruker, inntektSomLagres.captured)
        }
        verify { avkortingService.hentAvkorting(request.behandlingId) }

        with(inntektSomLagres.captured) {
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
                aarsinntekt = 300,
                innvilgaMaaneder = 12,
            )

        val inntektSomLagres = slot<AvkortingGrunnlagLagreDto>()

        coEvery {
            avkortingService.hentOpprettEllerReberegnAvkorting(
                request.behandlingId,
                bruker,
            )
        } returns
            avkorting(
                aar = 2025,
                inntektsavkorting =
                    listOf(
                        Inntektsavkorting(
                            grunnlag = eksisterendeInntekt,
                        ),
                    ),
            ).toFrontend(request.virkningstidspunkt)
        coEvery {
            avkortingService.beregnAvkortingMedNyttGrunnlag(
                any(),
                any(),
                capture(inntektSomLagres),
            )
        } returns mockk()
        every { avkortingService.hentAvkorting(any()) } returns mockk()

        runBlocking {
            service.opprettAvkortingMedBrukeroppgittInntekt(request, bruker)
        }

        coVerify {
            avkortingService.beregnAvkortingMedNyttGrunnlag(request.behandlingId, bruker, inntektSomLagres.captured)
        }
        verify { avkortingService.hentAvkorting(request.behandlingId) }

        with(inntektSomLagres.captured) {
            overstyrtInnvilgaMaaneder?.antall shouldBe 5
            overstyrtInnvilgaMaaneder?.begrunnelse shouldBe "Bruker har oppgitt tidlig alderspensjon i inntektsjusteringskjema"
            overstyrtInnvilgaMaaneder?.aarsak shouldBe OverstyrtInnvilgaMaanederAarsak.TAR_UT_PENSJON_TIDLIG.name
        }
    }
}
