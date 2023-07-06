package no.nav.etterlatte.beregning.regler.avkorting.regler

import io.kotest.matchers.shouldBe
import no.nav.etterlatte.avkorting.regler.avkortetYtelseMedRestanse
import no.nav.etterlatte.beregning.regler.avkortetYtelseGrunnlag
import no.nav.etterlatte.libs.regler.RegelPeriode
import org.junit.jupiter.api.Test
import java.time.LocalDate

class AvkortetYtelseTest {
    @Test
    fun `avkorteYtelse skal trekke avkortingsbeloep fra beregnet ytelse`() {
        val avkortetYtelse = avkortetYtelseMedRestanse.anvend(
            avkortetYtelseGrunnlag(beregning = 100000, avkorting = 50000),
            RegelPeriode(LocalDate.of(2023, 1, 1))
        )
        avkortetYtelse.verdi shouldBe 50000
    }

    @Test
    fun `avkorteYtelse skal bli 0 dersom avkortingsbeloep er stoerre enn beregnet ytelse`() {
        val avkortetYtelse = avkortetYtelseMedRestanse.anvend(
            avkortetYtelseGrunnlag(beregning = 50000, avkorting = 100000),
            RegelPeriode(LocalDate.of(2023, 1, 1))
        )
        avkortetYtelse.verdi shouldBe 0
    }

}