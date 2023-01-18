package no.nav.etterlatte.beregning.regler.sats

import com.fasterxml.jackson.annotation.JsonProperty
import no.nav.etterlatte.libs.common.objectMapper
import java.io.FileNotFoundException
import java.math.BigDecimal
import java.time.YearMonth

object Grunnbeloep {
    val alleGrunnbeloep: List<G> =
        objectMapper.readValue(readFile("/grunnbelop.json"), GrunnbeloepListe::class.java).grunnbeloep

    private fun readFile(file: String) = Grunnbeloep::class.java.getResource(file)?.readText()
        ?: throw FileNotFoundException("Fant ikke filen $file")
}

data class GrunnbeloepListe(
    @JsonProperty("grunnbeløp")
    val grunnbeloep: List<G>
)

data class G(
    val dato: YearMonth,
    @JsonProperty("grunnbeløp")
    val grunnbeloep: Int,
    @JsonProperty("grunnbeløpPerMåned")
    val grunnbeloepPerMaaned: Int,
    val omregningsfaktor: BigDecimal?
)