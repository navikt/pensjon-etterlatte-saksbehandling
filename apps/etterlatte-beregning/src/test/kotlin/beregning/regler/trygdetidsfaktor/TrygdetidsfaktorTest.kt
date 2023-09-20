package beregning.regler.trygdetidsfaktor

import io.kotest.matchers.shouldBe
import no.nav.etterlatte.beregning.regler.REGEL_PERIODE
import no.nav.etterlatte.beregning.regler.barnepensjon.trygdetidsfaktor.maksTrygdetid
import no.nav.etterlatte.beregning.regler.barnepensjon.trygdetidsfaktor.trygdetidRegel
import no.nav.etterlatte.beregning.regler.barnepensjon.trygdetidsfaktor.trygdetidsFaktor
import no.nav.etterlatte.beregning.regler.barnepensjonGrunnlag
import no.nav.etterlatte.beregning.regler.toBeregningstall
import no.nav.etterlatte.regler.Beregningstall
import org.junit.jupiter.api.Test

internal class TrygdetidsfaktorTest {
    @Test
    fun `trygdetidRegel skal returnere 40 aars trygdetid`() {
        val resultat = trygdetidRegel.anvend(barnepensjonGrunnlag(trygdeTid = Beregningstall(40.0)), REGEL_PERIODE)

        resultat.verdi shouldBe Beregningstall(40.0)
    }

    @Test
    fun `maksTrygdetid skal returnere 40 aars trygdetid`() {
        val resultat = maksTrygdetid.anvend(barnepensjonGrunnlag(), REGEL_PERIODE)

        resultat.verdi shouldBe Beregningstall(40)
    }

    @Test
    fun `trygdetidsFaktor skal returnere 1 naar trygdetid er 40 aar`() {
        val resultat = trygdetidsFaktor.anvend(barnepensjonGrunnlag(), REGEL_PERIODE)

        resultat.verdi shouldBe 1.toBeregningstall()
    }

    @Test
    fun `trygdetidsFaktor skal returnere 0,5 naar trygdetid er 20 aar`() {
        val resultat = trygdetidsFaktor.anvend(barnepensjonGrunnlag(trygdeTid = Beregningstall(20.0)), REGEL_PERIODE)

        resultat.verdi shouldBe 0.5.toBeregningstall()
    }
}
