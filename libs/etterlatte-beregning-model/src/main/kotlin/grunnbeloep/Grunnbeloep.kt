package no.nav.etterlatte.grunnbeloep

import com.fasterxml.jackson.annotation.JsonProperty
import java.math.BigDecimal
import java.time.YearMonth

data class Grunnbeloep(
    val dato: YearMonth,
    @JsonProperty("grunnbeløp")
    val grunnbeloep: Int,
    @JsonProperty("grunnbeløpPerMåned")
    val grunnbeloepPerMaaned: Int,
    val omregningsfaktor: BigDecimal?,
)
