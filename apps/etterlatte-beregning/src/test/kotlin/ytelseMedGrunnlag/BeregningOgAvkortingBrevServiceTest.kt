package no.nav.etterlatte.beregning.regler.ytelseMedGrunnlag

import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.avkorting.AvkortingService
import no.nav.etterlatte.avkorting.Inntektsavkorting
import no.nav.etterlatte.beregning.BeregningRepository
import no.nav.etterlatte.beregning.grunnlag.BeregningsGrunnlag
import no.nav.etterlatte.beregning.grunnlag.BeregningsGrunnlagService
import no.nav.etterlatte.beregning.regler.avkortetYtelse
import no.nav.etterlatte.beregning.regler.avkorting
import no.nav.etterlatte.beregning.regler.avkortinggrunnlag
import no.nav.etterlatte.beregning.regler.behandling
import no.nav.etterlatte.beregning.regler.beregning
import no.nav.etterlatte.beregning.regler.beregningsperiode
import no.nav.etterlatte.beregning.regler.bruker
import no.nav.etterlatte.klienter.BehandlingKlient
import no.nav.etterlatte.libs.common.beregning.BeregningsMetode
import no.nav.etterlatte.libs.common.beregning.BeregningsMetodeBeregningsgrunnlag
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.periode.Periode
import no.nav.etterlatte.libs.testdata.behandling.VirkningstidspunktTestData
import no.nav.etterlatte.ytelseMedGrunnlag.BeregningOgAvkortingBrevService
import org.junit.jupiter.api.Test
import java.time.YearMonth
import java.util.UUID

internal class BeregningOgAvkortingBrevServiceTest {
    private val avkortingService = mockk<AvkortingService>()
    private val beregningRepository = mockk<BeregningRepository>()
    private val beregningsGrunnlagService = mockk<BeregningsGrunnlagService>()
    private val behandlingKlient = mockk<BehandlingKlient>()
    private val service =
        BeregningOgAvkortingBrevService(
            beregningRepository,
            avkortingService,
            beregningsGrunnlagService,
            behandlingKlient,
        )

    @Test
    fun `returnerer null hvis avkorting ikke finnes`() {
        every { avkortingService.hentAvkorting(any()) } returns null
        every { beregningRepository.hent(any()) } returns null
        runBlocking {
            service.hentBeregningOgAvkorting(UUID.randomUUID(), bruker) shouldBe null
        }
    }

    @Test
    fun `skal hente ytelse oppdelt i perioder med alle grunnlag`() {
        val behandlingsId = UUID.randomUUID()
        val virkningstidspunkt = YearMonth.of(2024, 2)
        coEvery {
            behandlingKlient.hentBehandling(
                behandlingsId,
                bruker,
            )
        } returns behandling(virkningstidspunkt = VirkningstidspunktTestData.virkningstidsunkt(virkningstidspunkt))
        every { beregningRepository.hent(behandlingsId) } returns
            beregning(
                beregninger =
                    listOf(
                        beregningsperiode(
                            datoFOM = YearMonth.of(2024, 2),
                            datoTOM = YearMonth.of(2024, 5),
                            utbetaltBeloep = 20000,
                            trygdetid = 40,
                            grunnbeloep = 120000,
                            grunnbeloepMnd = 10000,
                        ),
                        beregningsperiode(
                            datoFOM = YearMonth.of(2024, 6),
                            datoTOM = null,
                            utbetaltBeloep = 21000,
                            trygdetid = 40,
                            grunnbeloep = 132000,
                            grunnbeloepMnd = 11000,
                        ),
                    ),
            )
        every { avkortingService.hentAvkorting(behandlingsId) } returns
            avkorting(
                inntektsavkorting =
                    listOf(
                        Inntektsavkorting(
                            grunnlag =
                                avkortinggrunnlag(
                                    periode = Periode(fom = YearMonth.of(2024, 2), tom = YearMonth.of(2024, 3)),
                                    inntektTom = 300000,
                                    fratrekkInnAar = 25000,
                                ),
                        ),
                        Inntektsavkorting(
                            grunnlag =
                                avkortinggrunnlag(
                                    periode = Periode(fom = YearMonth.of(2024, 4), tom = null),
                                    inntektTom = 350000,
                                    fratrekkInnAar = 25000,
                                ),
                        ),
                    ),
                avkortetYtelse =
                    listOf(
                        avkortetYtelse(
                            periode = Periode(fom = YearMonth.of(2024, 2), tom = YearMonth.of(2024, 3)),
                            ytelseEtterAvkorting = 15000,
                            ytelseFoerAvkorting = 20000,
                            avkortingsbeloep = 5000,
                        ),
                        avkortetYtelse(
                            periode = Periode(fom = YearMonth.of(2024, 4), tom = YearMonth.of(2024, 5)),
                            ytelseEtterAvkorting = 17000,
                            ytelseFoerAvkorting = 20000,
                            avkortingsbeloep = 3000,
                        ),
                        avkortetYtelse(
                            periode = Periode(fom = YearMonth.of(2024, 6), tom = null),
                            ytelseEtterAvkorting = 17000,
                            ytelseFoerAvkorting = 21000,
                            avkortingsbeloep = 4000,
                        ),
                    ),
            )
        coEvery { beregningsGrunnlagService.hentBeregningsGrunnlag(behandlingsId, bruker) } returns
            BeregningsGrunnlag(
                behandlingId = behandlingsId,
                kilde = Grunnlagsopplysning.Saksbehandler.create("Z123456"),
                beregningsMetode =
                    BeregningsMetodeBeregningsgrunnlag(
                        BeregningsMetode.BEST,
                    ),
            )

        val ytelse =
            runBlocking {
                service.hentBeregningOgAvkorting(behandlingsId, bruker)
            }

        with(ytelse!!.perioder[0]) {
            periode shouldBe Periode(fom = YearMonth.of(2024, 2), tom = YearMonth.of(2024, 3))
            ytelseEtterAvkorting shouldBe 15000
            ytelseFoerAvkorting shouldBe 20000
            avkortingsbeloep shouldBe 5000
            oppgittInntekt shouldBe 300000
            fratrekkInnAar shouldBe 25000
            grunnbelop shouldBe 120000
            grunnbelopMnd shouldBe 10000
            beregningsMetodeFraGrunnlag shouldBe BeregningsMetode.BEST
        }
        with(ytelse.perioder[1]) {
            periode shouldBe Periode(fom = YearMonth.of(2024, 4), tom = YearMonth.of(2024, 5))
            ytelseEtterAvkorting shouldBe 17000
            ytelseFoerAvkorting shouldBe 20000
            avkortingsbeloep shouldBe 3000
            oppgittInntekt shouldBe 350000
            fratrekkInnAar shouldBe 25000
            grunnbelop shouldBe 120000
            grunnbelopMnd shouldBe 10000
            beregningsMetodeFraGrunnlag shouldBe BeregningsMetode.BEST
        }
        with(ytelse.perioder[2]) {
            periode shouldBe Periode(fom = YearMonth.of(2024, 6), tom = null)
            ytelseEtterAvkorting shouldBe 17000
            ytelseFoerAvkorting shouldBe 21000
            avkortingsbeloep shouldBe 4000
            oppgittInntekt shouldBe 350000
            fratrekkInnAar shouldBe 25000
            grunnbelop shouldBe 132000
            grunnbelopMnd shouldBe 11000
            beregningsMetodeFraGrunnlag shouldBe BeregningsMetode.BEST
        }
    }
}
