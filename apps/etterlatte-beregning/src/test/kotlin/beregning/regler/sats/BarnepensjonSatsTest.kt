package beregning.regler.sats

import io.kotest.matchers.collections.shouldBeIn
import io.kotest.matchers.ints.shouldBeExactly
import io.kotest.matchers.shouldBe
import no.nav.etterlatte.beregning.regler.Beregningstall
import no.nav.etterlatte.beregning.regler.Beregningstall.Companion.DESIMALER_DELBEREGNING
import no.nav.etterlatte.beregning.regler.FNR_1
import no.nav.etterlatte.beregning.regler.FNR_2
import no.nav.etterlatte.beregning.regler.FNR_3
import no.nav.etterlatte.beregning.regler.REGEL_PERIODE
import no.nav.etterlatte.beregning.regler.barnepensjon.sats.antallSoeskenIKullet
import no.nav.etterlatte.beregning.regler.barnepensjon.sats.barnepensjonSatsRegel
import no.nav.etterlatte.beregning.regler.barnepensjon.sats.belopForEtterfoelgendeBarn
import no.nav.etterlatte.beregning.regler.barnepensjon.sats.belopForFoersteBarn
import no.nav.etterlatte.beregning.regler.barnepensjon.sats.grunnbeloep
import no.nav.etterlatte.beregning.regler.barnepensjon.sats.historiskeGrunnbeloep
import no.nav.etterlatte.beregning.regler.barnepensjon.sats.prosentsatsEtterfoelgendeBarnKonstant
import no.nav.etterlatte.beregning.regler.barnepensjon.sats.prosentsatsFoersteBarnKonstant
import no.nav.etterlatte.beregning.regler.barnepensjon.sats.soeskenIKullet
import no.nav.etterlatte.beregning.regler.barnepensjonGrunnlag
import no.nav.etterlatte.beregning.regler.toBeregningstall
import no.nav.etterlatte.libs.regler.RegelPeriode
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.YearMonth

internal class BarnepensjonSatsTest {

    @Test
    fun `historiskeGrunnbeloep skal hente alle historiske grunnbeloep`() {
        historiskeGrunnbeloep.size shouldBeExactly 69
    }

    @Test
    fun `grunnbeloep skal returnere korrekt grunnbeloep for mai 2022`() {
        val resultat = grunnbeloep.anvend(barnepensjonGrunnlag(), RegelPeriode(LocalDate.of(2022, 5, 1)))
        with(resultat.verdi) {
            dato shouldBe YearMonth.of(2022, 5)
            grunnbeloep shouldBeExactly 111477
            grunnbeloepPerMaaned shouldBeExactly 9290
        }
    }

    @Test
    fun `grunnbeloep skal returnere korrekt grunnbeloep for april 2022`() {
        val resultat = grunnbeloep.anvend(barnepensjonGrunnlag(), RegelPeriode(LocalDate.of(2022, 4, 1)))
        with(resultat.verdi) {
            dato shouldBe YearMonth.of(2021, 5)
            grunnbeloep shouldBeExactly 106399
            grunnbeloepPerMaaned shouldBeExactly 8867
        }
    }

    @Test
    fun `soeskenIKullet skal returnere liste med soesken`() {
        val resultat = soeskenIKullet.anvend(
            grunnlag = barnepensjonGrunnlag(soeskenKull = listOf(FNR_1, FNR_2)),
            periode = REGEL_PERIODE
        )

        resultat.verdi.size shouldBeExactly 2
        resultat.verdi.forEach { it.value shouldBeIn listOf(FNR_1, FNR_2) }
    }

    @Test
    fun `antallSoeskenIKullet skal returnere antall soesken i kullet`() {
        val resultat = antallSoeskenIKullet.anvend(
            grunnlag = barnepensjonGrunnlag(soeskenKull = listOf(FNR_1, FNR_2)),
            periode = REGEL_PERIODE
        )

        resultat.verdi shouldBeExactly 2
    }

    @Test
    fun `prosentSatsFoersteBarn skal returnere 40 prosent`() {
        val resultat = prosentsatsFoersteBarnKonstant.anvend(
            grunnlag = barnepensjonGrunnlag(),
            periode = REGEL_PERIODE
        )

        resultat.verdi shouldBe Beregningstall(0.40)
    }

    @Test
    fun `prosentSatsEtterfoelgendeBarn skal returnere 25 prosent`() {
        val resultat = prosentsatsEtterfoelgendeBarnKonstant.anvend(
            grunnlag = barnepensjonGrunnlag(),
            periode = REGEL_PERIODE
        )

        resultat.verdi shouldBe Beregningstall(0.25)
    }

    @Test
    fun `belopForFoersteBarn skal returnere 3716,00 kroner`() {
        val resultat = belopForFoersteBarn.anvend(
            grunnlag = barnepensjonGrunnlag(),
            periode = REGEL_PERIODE
        )

        resultat.verdi shouldBe 3716.00.toBeregningstall(DESIMALER_DELBEREGNING)
    }

    @Test
    fun `belopForEtterfoelgendeBarn skal returnere 2322,50 kroner`() {
        val resultat = belopForEtterfoelgendeBarn.anvend(
            grunnlag = barnepensjonGrunnlag(),
            periode = REGEL_PERIODE
        )

        resultat.verdi shouldBe 2322.50.toBeregningstall(DESIMALER_DELBEREGNING)
    }

    @Test
    fun `barnepensjonSatsRegel skal returnere 3716,00 kroner ved 0 soesken`() {
        val resultat = barnepensjonSatsRegel.anvend(
            grunnlag = barnepensjonGrunnlag(),
            periode = REGEL_PERIODE
        )

        resultat.verdi shouldBe 3716.00.toBeregningstall(DESIMALER_DELBEREGNING)
    }

    @Test
    fun `barnepensjonSatsRegel skal returnere 3019,25 kroner ved 1 soesken`() {
        val resultat = barnepensjonSatsRegel.anvend(
            grunnlag = barnepensjonGrunnlag(soeskenKull = listOf(FNR_1)),
            periode = REGEL_PERIODE
        )

        resultat.verdi shouldBe 3019.25.toBeregningstall(DESIMALER_DELBEREGNING)
    }

    @Test
    fun `barnepensjonSatsRegel skal returnere 2787,00 kroner ved 2 soesken`() {
        val resultat = barnepensjonSatsRegel.anvend(
            grunnlag = barnepensjonGrunnlag(soeskenKull = listOf(FNR_1, FNR_2)),
            periode = REGEL_PERIODE
        )

        resultat.verdi shouldBe 2787.00.toBeregningstall(DESIMALER_DELBEREGNING)
    }

    @Test
    fun `barnepensjonSatsRegel skal returnere 2670,887 kroner ved 3 soesken`() {
        val resultat = barnepensjonSatsRegel.anvend(
            grunnlag = barnepensjonGrunnlag(soeskenKull = listOf(FNR_1, FNR_2, FNR_3)),
            periode = REGEL_PERIODE
        )

        resultat.verdi shouldBe 2670.875.toBeregningstall(DESIMALER_DELBEREGNING)
    }
}