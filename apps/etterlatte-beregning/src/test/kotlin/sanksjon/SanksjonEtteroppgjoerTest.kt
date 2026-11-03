package no.nav.etterlatte.sanksjon

import io.kotest.matchers.shouldBe
import no.nav.etterlatte.avkorting.Avkorting
import no.nav.etterlatte.beregning.regler.aarsoppgjoer
import no.nav.etterlatte.beregning.regler.bruker
import no.nav.etterlatte.beregning.regler.sanksjon
import no.nav.etterlatte.beregning.regler.ytelseFoerAvkorting
import no.nav.etterlatte.libs.common.beregning.Sanksjon
import no.nav.etterlatte.libs.common.periode.Periode
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.time.YearMonth

class SanksjonEtteroppgjoerTest {
    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun `etteroppgjoer med opphoer i mars skal beholde sanksjon i februar ogsaa ved reberegning`(brukNyeRegler: Boolean) {
        val januar = YearMonth.of(2026, 1)
        val februar = YearMonth.of(2026, 2)
        val mars = YearMonth.of(2026, 3)
        val sanksjoner = listOf(sanksjon(fom = februar, tom = februar))
        val avkorting =
            Avkorting(
                aarsoppgjoer =
                    listOf(
                        aarsoppgjoer(
                            aar = 2026,
                            fom = januar,
                            ytelseFoerAvkorting =
                                listOf(
                                    ytelseFoerAvkorting(beregning = 20000, periode = Periode(januar, februar)),
                                    ytelseFoerAvkorting(beregning = 0, periode = Periode(mars, null)),
                                ),
                        ),
                    ),
            )

        fun forbehandling(sanksjoner: List<Sanksjon>) =
            avkorting.beregnEtteroppgjoer(
                brukerTokenInfo = bruker,
                aar = 2026,
                loennsinntekt = 0,
                afp = 0,
                naeringsinntekt = 0,
                utland = 0,
                sanksjoner = sanksjoner,
                spesifikasjon = "",
                innvilgetPeriodeIEtteroppgjoersAar = Periode(januar, februar),
                opphoerFom = mars,
                brukNyeReglerAvkorting = brukNyeRegler,
            )

        fun ytelseFebruar(avkorting: Avkorting) =
            avkorting.aarsoppgjoer
                .single()
                .avkortetYtelse
                .single { it.periode.fom <= februar && (it.periode.tom ?: februar) >= februar }
                .ytelseEtterAvkorting

        val forbehandling = forbehandling(sanksjoner)
        ytelseFebruar(forbehandling) shouldBe 0
        val revurdering =
            forbehandling.kopierAvkorting(mars).beregnAvkorting(
                virkningstidspunkt = januar,
                beregning = null,
                sanksjoner = sanksjoner,
                opphoerFom = mars,
                brukNyeReglerAvkorting = brukNyeRegler,
            )
        ytelseFebruar(revurdering) shouldBe 0

        ytelseFebruar(forbehandling(emptyList())) shouldBe 20000
    }
}
