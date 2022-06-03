package model

import no.nav.etterlatte.libs.common.objectMapper
import java.io.FileNotFoundException
import java.math.BigDecimal
import java.time.LocalDate

class Grunnbeloep {

    companion object {
        val melding = readFile("/grunnbelop.json")

        val gListe = objectMapper.readValue(melding, grunnbeløpListe::class.java)


        fun readFile(file: String) = Companion::class.java.getResource(file)?.readText()
            ?: throw FileNotFoundException("Fant ikke filen $file")

        fun hentGforPeriode(datoFOM: LocalDate, datoTOM: LocalDate = LocalDate.MAX): List<G> {
            return gListe.grunnbeløp.filter { !it.dato.isAfter(datoTOM) }.partition { it.dato.isAfter(datoFOM) }
                .let { (gEtterFOM, gFoerFOM) ->
                    gEtterFOM + (gFoerFOM.maxByOrNull { it.dato }
                        ?: throw IllegalArgumentException("Fant ingen G for perioden"))
                }.sortedBy { it.dato }
        }
        fun hentGjeldendeG (dato: LocalDate): G {
            return gListe.grunnbeløp.filter { it.dato.isBefore(dato)||it.dato.isEqual(dato) && beregnTom(it)
                ?.isAfter(dato) ?: true }.first()
        }

        //TODO virker denna?
        fun beregnTom(g: G): LocalDate? {
            return gListe.grunnbeløp.sortedBy { it.dato }.zipWithNext().find { it.first.dato == g.dato }?.let {
                it.second.dato
            }
        }
    }
}


data class grunnbeløpListe(
    val grunnbeløp: List<G>
)

data class G(
    val dato: LocalDate,
    val grunnbeløp: Int,
    val grunnbeløpPerMåned: Int,
    val gjennomsnittPerÅr: Int?,
    val omregningsfaktor: BigDecimal?,
    val virkningstidspunktForMinsteinntekt: LocalDate?,

    //fun sort()
)