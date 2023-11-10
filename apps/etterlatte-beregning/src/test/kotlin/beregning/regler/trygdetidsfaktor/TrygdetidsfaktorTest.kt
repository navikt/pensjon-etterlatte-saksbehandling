package beregning.regler.trygdetidsfaktor

import io.kotest.matchers.shouldBe
import no.nav.etterlatte.beregning.regler.AnvendtTrygdetid
import no.nav.etterlatte.beregning.regler.REGEL_PERIODE
import no.nav.etterlatte.beregning.regler.barnepensjon.trygdetidsfaktor.maksTrygdetid
import no.nav.etterlatte.beregning.regler.barnepensjon.trygdetidsfaktor.trygdetidBruktRegel
import no.nav.etterlatte.beregning.regler.barnepensjon.trygdetidsfaktor.trygdetidsFaktor
import no.nav.etterlatte.beregning.regler.barnepensjonGrunnlag
import no.nav.etterlatte.beregning.regler.samletTrygdetid
import no.nav.etterlatte.beregning.regler.toBeregningstall
import no.nav.etterlatte.libs.common.IntBroek
import no.nav.etterlatte.libs.common.beregning.BeregningsMetode
import no.nav.etterlatte.regler.Beregningstall
import org.junit.jupiter.api.Test

internal class TrygdetidsfaktorTest {
    private val trygdetid =
        samletTrygdetid(
            BeregningsMetode.NASJONAL,
            samletTrygdetidNorge = Beregningstall(40.0),
            samletTrygdetidTeoretisk = Beregningstall(30.0),
            broek = IntBroek(1, 2),
        )

    @Test
    fun `trygdetidRegel skal returnere 40 aars trygdetid for nasjonal`() {
        val resultat =
            trygdetidBruktRegel.anvend(
                barnepensjonGrunnlag().copy(
                    avdoedesTrygdetid = trygdetid,
                ),
                REGEL_PERIODE,
            )

        resultat.verdi shouldBe AnvendtTrygdetid(BeregningsMetode.NASJONAL, Beregningstall(40.0))
    }

    @Test
    fun `trygdetidRegel skal returnere 15 aars trygdetid for prorata`() {
        val resultat =
            trygdetidBruktRegel.anvend(
                barnepensjonGrunnlag().copy(
                    avdoedesTrygdetid = trygdetid.copy(verdi = trygdetid.verdi.copy(beregningsMetode = BeregningsMetode.PRORATA)),
                ),
                REGEL_PERIODE,
            )

        resultat.verdi.trygdetid.setScale(1) shouldBe Beregningstall(15.0).setScale(1)
    }

    @Test
    fun `trygdetidRegel skal returnere 40 aars trygdetid for best`() {
        val resultat =
            trygdetidBruktRegel.anvend(
                barnepensjonGrunnlag().copy(
                    avdoedesTrygdetid = trygdetid.copy(verdi = trygdetid.verdi.copy(beregningsMetode = BeregningsMetode.BEST)),
                ),
                REGEL_PERIODE,
            )

        resultat.verdi shouldBe AnvendtTrygdetid(BeregningsMetode.NASJONAL, Beregningstall(40.0))
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
