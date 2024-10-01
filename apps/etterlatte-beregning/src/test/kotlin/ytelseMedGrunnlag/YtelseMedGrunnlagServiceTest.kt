package no.nav.etterlatte.beregning.regler.ytelseMedGrunnlag

import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.avkorting.AvkortingRepository
import no.nav.etterlatte.avkorting.Inntektsavkorting
import no.nav.etterlatte.beregning.BeregningRepository
import no.nav.etterlatte.beregning.regler.avkortetYtelse
import no.nav.etterlatte.beregning.regler.avkorting
import no.nav.etterlatte.beregning.regler.avkortinggrunnlag
import no.nav.etterlatte.beregning.regler.behandling
import no.nav.etterlatte.beregning.regler.beregning
import no.nav.etterlatte.beregning.regler.beregningsperiode
import no.nav.etterlatte.beregning.regler.bruker
import no.nav.etterlatte.klienter.BehandlingKlient
import no.nav.etterlatte.libs.common.periode.Periode
import no.nav.etterlatte.libs.testdata.behandling.VirkningstidspunktTestData
import no.nav.etterlatte.ytelseMedGrunnlag.YtelseMedGrunnlagService
import org.junit.jupiter.api.Test
import java.time.YearMonth
import java.util.UUID

internal class YtelseMedGrunnlagServiceTest {
    private val avkortingRepository = mockk<AvkortingRepository>()
    private val beregningRepository = mockk<BeregningRepository>()
    private val behandlingKlient = mockk<BehandlingKlient>()
    private val service =
        YtelseMedGrunnlagService(
            beregningRepository,
            avkortingRepository,
            behandlingKlient,
        )

    @Test
    fun `returnerer null hvis avkorting ikke finnes`() {
        every { avkortingRepository.hentAvkorting(any()) } returns null
        every { beregningRepository.hent(any()) } returns null
        runBlocking {
            service.hentYtelseMedGrunnlag(UUID.randomUUID(), bruker) shouldBe null
        }
    }

    @Test
    fun `inntekt for neste aar er true hvis avkorting er oppgitt for neste aar`() {
        val behandlingsId = UUID.randomUUID()
        val virkningstidspunkt = YearMonth.of(YearMonth.now().year, 11)

        val beregning =
            mutableListOf(
                beregningsperiode(
                    datoFOM = YearMonth.of(YearMonth.now().year, 11),
                    datoTOM = YearMonth.of(YearMonth.now().year, 12),
                    utbetaltBeloep = 20000,
                    trygdetid = 40,
                    grunnbeloep = 120000,
                    grunnbeloepMnd = 10000,
                ),
                beregningsperiode(
                    datoFOM = YearMonth.of(YearMonth.now().year + 1, 1),
                    datoTOM = null,
                    utbetaltBeloep = 21000,
                    trygdetid = 40,
                    grunnbeloep = 132000,
                    grunnbeloepMnd = 11000,
                ),
            )

        val inntektsavkorting =
            mutableListOf(
                Inntektsavkorting(
                    grunnlag =
                        avkortinggrunnlag(
                            periode =
                                Periode(
                                    fom = YearMonth.of(YearMonth.now().year, 11),
                                    tom = YearMonth.of(YearMonth.now().year, 12),
                                ),
                            aarsinntekt = 300000,
                            fratrekkInnAar = 25000,
                        ),
                ),
                Inntektsavkorting(
                    grunnlag =
                        avkortinggrunnlag(
                            periode = Periode(fom = YearMonth.of(YearMonth.now().year + 1, 1), tom = null),
                            aarsinntekt = 350000,
                            fratrekkInnAar = 25000,
                        ),
                ),
            )

        val avkortetYtelseAar =
            mutableListOf(
                avkortetYtelse(
                    periode =
                        Periode(
                            fom = YearMonth.of(YearMonth.now().year, 11),
                            tom = YearMonth.of(YearMonth.now().year, 12),
                        ),
                    ytelseEtterAvkorting = 15000,
                    ytelseFoerAvkorting = 20000,
                    avkortingsbeloep = 5000,
                ),
                avkortetYtelse(
                    periode =
                        Periode(
                            fom = YearMonth.of(YearMonth.now().year + 1, 1),
                            tom = null,
                        ),
                    ytelseEtterAvkorting = 17000,
                    ytelseFoerAvkorting = 20000,
                    avkortingsbeloep = 3000,
                ),
            )

        // positiv test
        coEvery {
            behandlingKlient.hentBehandling(behandlingsId, bruker)
        } returns behandling(virkningstidspunkt = VirkningstidspunktTestData.virkningstidsunkt(virkningstidspunkt))

        every { beregningRepository.hent(behandlingsId) } returns
            beregning(
                beregninger = beregning,
            )

        every { avkortingRepository.hentAvkorting(behandlingsId) } returns
            avkorting(
                inntektsavkorting = inntektsavkorting,
                avkortetYtelseAar = avkortetYtelseAar,
            )

        val positivTest =
            runBlocking {
                service.hentYtelseMedGrunnlag(behandlingsId, bruker)
            }

        positivTest!!.inntektForNesteAar shouldBe true

        // negativ test
        beregning.removeLast()
        inntektsavkorting.removeLast()
        avkortetYtelseAar.removeLast()

        coEvery {
            behandlingKlient.hentBehandling(behandlingsId, bruker)
        } returns behandling(virkningstidspunkt = VirkningstidspunktTestData.virkningstidsunkt(virkningstidspunkt))

        every { beregningRepository.hent(behandlingsId) } returns
            beregning(
                beregninger = beregning,
            )

        every { avkortingRepository.hentAvkorting(behandlingsId) } returns
            avkorting(
                inntektsavkorting = inntektsavkorting,
                avkortetYtelseAar = avkortetYtelseAar,
            )

        val negativTest =
            runBlocking {
                service.hentYtelseMedGrunnlag(behandlingsId, bruker)
            }

        negativTest!!.inntektForNesteAar shouldBe false
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
        every { avkortingRepository.hentAvkorting(behandlingsId) } returns
            avkorting(
                inntektsavkorting =
                    listOf(
                        Inntektsavkorting(
                            grunnlag =
                                avkortinggrunnlag(
                                    periode = Periode(fom = YearMonth.of(2024, 2), tom = YearMonth.of(2024, 3)),
                                    aarsinntekt = 300000,
                                    fratrekkInnAar = 25000,
                                ),
                        ),
                        Inntektsavkorting(
                            grunnlag =
                                avkortinggrunnlag(
                                    periode = Periode(fom = YearMonth.of(2024, 4), tom = null),
                                    aarsinntekt = 350000,
                                    fratrekkInnAar = 25000,
                                ),
                        ),
                    ),
                avkortetYtelseAar =
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

        val ytelse =
            runBlocking {
                service.hentYtelseMedGrunnlag(behandlingsId, bruker)
            }

        with(ytelse!!.perioder[0]) {
            periode shouldBe Periode(fom = YearMonth.of(2024, 2), tom = YearMonth.of(2024, 3))
            ytelseEtterAvkorting shouldBe 15000
            ytelseFoerAvkorting shouldBe 20000
            avkortingsbeloep shouldBe 5000
            aarsinntekt shouldBe 300000
            fratrekkInnAar shouldBe 25000
            grunnbelop shouldBe 120000
            grunnbelopMnd shouldBe 10000
        }
        with(ytelse.perioder[1]) {
            periode shouldBe Periode(fom = YearMonth.of(2024, 4), tom = YearMonth.of(2024, 5))
            ytelseEtterAvkorting shouldBe 17000
            ytelseFoerAvkorting shouldBe 20000
            avkortingsbeloep shouldBe 3000
            aarsinntekt shouldBe 350000
            fratrekkInnAar shouldBe 25000
            grunnbelop shouldBe 120000
            grunnbelopMnd shouldBe 10000
        }
        with(ytelse.perioder[2]) {
            periode shouldBe Periode(fom = YearMonth.of(2024, 6), tom = null)
            ytelseEtterAvkorting shouldBe 17000
            ytelseFoerAvkorting shouldBe 21000
            avkortingsbeloep shouldBe 4000
            aarsinntekt shouldBe 350000
            fratrekkInnAar shouldBe 25000
            grunnbelop shouldBe 132000
            grunnbelopMnd shouldBe 11000
        }
    }
}
