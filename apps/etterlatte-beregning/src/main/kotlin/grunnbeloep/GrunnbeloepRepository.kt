package no.nav.etterlatte.grunnbeloep

import com.fasterxml.jackson.annotation.JsonProperty
import no.nav.etterlatte.libs.common.objectMapper
import java.io.FileNotFoundException
import java.math.BigDecimal
import java.time.YearMonth

object GrunnbeloepRepository {
    val historiskeGrunnbeloep: List<Grunnbeloep> =
        objectMapper.readValue(readFile("/grunnbelop.json"), GrunnbeloepListe::class.java).grunnbeloep

    private fun readFile(file: String) = GrunnbeloepRepository::class.java.getResource("/grunnbelop.json")?.readText()
        ?: throw FileNotFoundException("Fant ikke filen $file")

    fun hentGjeldendeGrunnbeloep(dato: YearMonth): Grunnbeloep {
        return historiskeGrunnbeloep.first {
            it.dato.isBefore(dato) || it.dato == dato && beregnTom(it)?.isAfter(dato) ?: true
        }
    }

    private fun beregnTom(grunnbeloep: Grunnbeloep): YearMonth? {
        return historiskeGrunnbeloep.sortedBy { it.dato }.zipWithNext()
            .find { it.first.dato == grunnbeloep.dato }?.second?.dato?.minusMonths(1)
    }
}

data class GrunnbeloepListe(
    @JsonProperty("grunnbeløp")
    val grunnbeloep: List<Grunnbeloep>
)

data class Grunnbeloep(
    val dato: YearMonth,
    @JsonProperty("grunnbeløp")
    val grunnbeloep: Int,
    @JsonProperty("grunnbeløpPerMåned")
    val grunnbeloepPerMaaned: Int,
    val omregningsfaktor: BigDecimal?
)