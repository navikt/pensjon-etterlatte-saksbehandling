package trygdetid

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.etterlatte.trygdetid.TrygdetidBeregningService
import no.nav.etterlatte.trygdetid.TrygdetidPeriode
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.time.Period

class TrygdetidBeregningServiceTest {

    @Test
    fun `skal gi 22 aar total trygdetid`() {
        val trygdetidGrunnlag = listOf(
            trygdetidGrunnlag(beregnetTrygdetidGrunnlag = beregnetTrygdetidGrunnlag(Period.ofYears(10))),
            trygdetidGrunnlag(beregnetTrygdetidGrunnlag = beregnetTrygdetidGrunnlag(Period.ofYears(10))),
            trygdetidGrunnlag(beregnetTrygdetidGrunnlag = beregnetTrygdetidGrunnlag(Period.ofYears(2)))
        )

        val beregnetTrygdetid = TrygdetidBeregningService.beregnTrygdetid(trygdetidGrunnlag)
        beregnetTrygdetid shouldNotBe null
        with(beregnetTrygdetid!!) {
            regelResultat shouldNotBe null
            tidspunkt shouldNotBe null
            verdi shouldBe 22
        }
    }

    @Test
    fun `skal gi maksimalt 40 aar total trygdetid`() {
        val trygdetidGrunnlag = listOf(
            trygdetidGrunnlag(beregnetTrygdetidGrunnlag = beregnetTrygdetidGrunnlag(Period.ofYears(30))),
            trygdetidGrunnlag(beregnetTrygdetidGrunnlag = beregnetTrygdetidGrunnlag(Period.ofYears(30)))
        )

        val beregnetTrygdetid = TrygdetidBeregningService.beregnTrygdetid(trygdetidGrunnlag)

        beregnetTrygdetid!!.verdi shouldBe 40
    }

    @Test
    fun `skal avrunde til naermeste aar med total trygdetid`() {
        val trygdetidGrunnlag = listOf(
            trygdetidGrunnlag(beregnetTrygdetidGrunnlag = beregnetTrygdetidGrunnlag(Period.ofMonths(2)))
        )

        val beregnetTrygdetid = TrygdetidBeregningService.beregnTrygdetid(trygdetidGrunnlag)

        beregnetTrygdetid!!.verdi shouldBe 0
    }

    @Test
    fun `trygdetidgrunnlag skal gi tre aar, en maaned og tre dager trygdetid`() {
        val trygdetidGrunnlag = trygdetidGrunnlag(
            periode = TrygdetidPeriode(fra = LocalDate.of(2020, 1, 1), til = LocalDate.of(2023, 2, 3))
        )

        val beregnetTrygdetid = TrygdetidBeregningService.beregnTrygdetidGrunnlag(trygdetidGrunnlag)

        beregnetTrygdetid shouldNotBe null
        with(beregnetTrygdetid!!) {
            regelResultat shouldNotBe null
            tidspunkt shouldNotBe null
            verdi shouldBe Period.of(3, 1, 3)
        }
    }

    @Test
    fun `trygdetidgrunnlag skal gi en dag trygdetid`() {
        val trygdetidGrunnlag = trygdetidGrunnlag(
            periode = TrygdetidPeriode(fra = LocalDate.of(2023, 1, 1), til = LocalDate.of(2023, 1, 1))
        )

        val beregnetTrygdetid = TrygdetidBeregningService.beregnTrygdetidGrunnlag(trygdetidGrunnlag)

        beregnetTrygdetid shouldNotBe null
        with(beregnetTrygdetid!!) {
            regelResultat shouldNotBe null
            tidspunkt shouldNotBe null
            verdi shouldBe Period.ofDays(1)
        }
    }

    @Test
    fun `skal ikke vaere mulig aa opprette negativ periode i trygdetidgrunnlag`() {
        assertThrows<IllegalArgumentException> {
            trygdetidGrunnlag(
                periode = TrygdetidPeriode(fra = LocalDate.of(2023, 1, 1), til = LocalDate.of(2022, 1, 1))
            )
        }
    }
}