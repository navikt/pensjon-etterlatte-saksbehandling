package beregning.regler.avkorting

import io.kotest.matchers.shouldBe
import no.nav.etterlatte.beregning.regler.avkortetYtelseGrunnlag
import no.nav.etterlatte.beregning.regler.avkorting.avkorteYtelse
import no.nav.etterlatte.libs.regler.RegelPeriode
import org.junit.jupiter.api.Test
import java.time.LocalDate

class AvkortetYtelseTest {
    @Test
    fun `avkorteYtelse skal trekke avkortingsbeloep fra bruttoytelse`() {
        val avkortetYtelse = avkorteYtelse.anvend(
            avkortetYtelseGrunnlag(bruttoYtelse = 100000, avkorting = 50000),
            RegelPeriode(LocalDate.of(2023, 1, 1))
        )
        avkortetYtelse.verdi shouldBe 50000
    }
}