package grunnbeloep

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import no.nav.etterlatte.grunnbeloep.GrunnbeloepRepository
import org.junit.jupiter.api.Test
import java.time.YearMonth

internal class GrunnbeloepRepositoryTest {
    @Test
    fun `skal hente alle historiske grunnbeloep`() {
        GrunnbeloepRepository.historiskeGrunnbeloep shouldHaveSize 70
    }

    @Test
    fun `skal sjekke at verdier er riktig for et gitt grunnbeloep`() {
        val grunnbeloep = GrunnbeloepRepository.historiskeGrunnbeloep.first { it.dato == YearMonth.of(2022, 5) }
        grunnbeloep.grunnbeloep shouldBe 111477
        grunnbeloep.grunnbeloepPerMaaned shouldBe 9290
    }

    @Test
    fun `skal hente grunnbeloep for en gitt periode - start maaned`() {
        val grunnbeloep = GrunnbeloepRepository.hentGjeldendeGrunnbeloep(YearMonth.of(2021, 5))
        grunnbeloep.grunnbeloep shouldBe 106399
        grunnbeloep.grunnbeloepPerMaaned shouldBe 8867
    }

    @Test
    fun `skal hente grunnbeloep for en gitt periode - siste maaned`() {
        val grunnbeloep = GrunnbeloepRepository.hentGjeldendeGrunnbeloep(YearMonth.of(2022, 4))
        grunnbeloep.grunnbeloep shouldBe 106399
        grunnbeloep.grunnbeloepPerMaaned shouldBe 8867
    }
}
