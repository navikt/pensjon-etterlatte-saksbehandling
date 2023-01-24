package beregning.regler.trygdetidsfaktor

import beregning.regler.REGEL_PERIODE
import beregning.regler.barnepensjonGrunnlag
import io.kotest.matchers.shouldBe
import no.nav.etterlatte.beregning.regler.trygdetidsfaktor.maksTrygdetid
import no.nav.etterlatte.beregning.regler.trygdetidsfaktor.trygdetidRegel
import no.nav.etterlatte.beregning.regler.trygdetidsfaktor.trygdetidsFaktor
import org.junit.jupiter.api.Test
import java.math.BigDecimal

internal class TrygdetidsfaktorTest {

    @Test
    fun `trygdetidRegel skal returnere 40 aars trygdetid`() {
        val resultat = trygdetidRegel.anvend(barnepensjonGrunnlag(trygdeTid = 40.0.toBigDecimal()), REGEL_PERIODE)

        resultat.verdi shouldBe 40.0.toBigDecimal()
    }

    @Test
    fun `maksTrygdetid skal returnere 40 aars trygdetid`() {
        val resultat = maksTrygdetid.anvend(barnepensjonGrunnlag(), REGEL_PERIODE)

        resultat.verdi shouldBe BigDecimal(40)
    }

    @Test
    fun `trygdetidsFaktor skal returnere 1 naar trygdetid er 40 aar`() {
        val resultat = trygdetidsFaktor.anvend(barnepensjonGrunnlag(), REGEL_PERIODE)

        resultat.verdi shouldBe BigDecimal(1)
    }

    @Test
    fun `trygdetidsFaktor skal returnere 0,5 naar trygdetid er 20 aar`() {
        val resultat = trygdetidsFaktor.anvend(barnepensjonGrunnlag(trygdeTid = 20.0.toBigDecimal()), REGEL_PERIODE)

        resultat.verdi shouldBe BigDecimal(0.5)
    }
}