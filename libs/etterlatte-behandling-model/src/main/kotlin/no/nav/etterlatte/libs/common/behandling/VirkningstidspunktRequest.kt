package no.nav.etterlatte.behandling

import com.fasterxml.jackson.annotation.JsonProperty
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.toNorskTid
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeParseException

data class VirkningstidspunktRequest(
    @JsonProperty("dato") private val _dato: String,
    val begrunnelse: String?,
    val kravdato: LocalDate? = null,
) {
    val dato: YearMonth = _dato.tilYearMonth()
}

fun String.tilYearMonth(): YearMonth =
    try {
        Tidspunkt.parse(this).toNorskTid().let {
            YearMonth.of(it.year, it.month)
        } ?: throw IllegalArgumentException("Dato $this må være definert")
    } catch (dtpe: DateTimeParseException) {
        // For testdata-generering støter vi på denne.
        YearMonth.parse(this.trim())
    } catch (e: Exception) {
        throw RuntimeException("Kunne ikke lese dato for virkningstidspunkt: $this", e)
    }
