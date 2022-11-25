package no.nav.etterlatte.model

import no.nav.etterlatte.libs.common.objectMapper
import java.io.FileNotFoundException
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

class Grunnbeloep {

    companion object {
        private val melding = readFile("/grunnbelop.json")

        private val gListe: GrunnbeløpListe = objectMapper.readValue(melding, GrunnbeløpListe::class.java)

        private fun readFile(file: String) = Companion::class.java.getResource(file)?.readText()
            ?: throw FileNotFoundException("Fant ikke filen $file")

        fun hentGforPeriode(datoFOM: YearMonth, datoTOM: YearMonth = YearMonth.from(LocalDate.MAX)): List<G> {
            return gListe.grunnbeløp.filter { !it.dato.isAfter(datoTOM) }.partition { it.dato.isAfter(datoFOM) }
                .let { (gEtterFOM, gFoerFOM) ->
                    gEtterFOM + (
                        gFoerFOM.maxByOrNull { it.dato }
                            ?: throw IllegalArgumentException("Fant ingen G for perioden")
                        )
                }.sortedBy { it.dato }
        }
        fun hentGjeldendeG(dato: YearMonth): G {
            return gListe.grunnbeløp.first {
                it.dato.isBefore(dato) || it.dato == dato && beregnTom(it)?.isAfter(dato) ?: true
            }
        }

        fun beregnTom(g: G): YearMonth? {
            return gListe.grunnbeløp.sortedBy { it.dato }.zipWithNext()
                .find { it.first.dato == g.dato }?.second?.dato?.minusMonths(1)
        }
    }
}

data class GrunnbeløpListe(
    val grunnbeløp: List<G>
)

data class G(
    val dato: YearMonth,
    val grunnbeløp: Int,
    val grunnbeløpPerMåned: Int,
    val omregningsfaktor: BigDecimal?
)