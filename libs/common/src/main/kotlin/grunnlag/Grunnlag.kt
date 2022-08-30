package no.nav.etterlatte.libs.common.grunnlag

import com.fasterxml.jackson.databind.JsonNode
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstyper
import java.time.YearMonth

data class Grunnlag(
    val saksId: Long,
    val grunnlag: List<Grunnlagsopplysning<JsonNode>>,
    val versjon: Long
)

data class Opplysningsgrunnlag(
    val grunnlagsdata: Grunnlagsdata<out JsonNode>,
    val metadata: Metadata
)

data class Metadata(val sakId: Long, val versjon: Long)

data class Grunnlagsdata<T>(
    val søker: Map<Opplysningstyper, Opplysning<T>>
)

data class PeriodisertOpplysning<T>(
    val kilde: Grunnlagsopplysning.Kilde,
    val verdi: T,
    val fom: YearMonth,
    val tom: YearMonth?
)

sealed class Opplysning<T> {
    data class Periodisert<T>(
        val perioder: List<PeriodisertOpplysning<T>>
    ) : Opplysning<T>()

    data class Konstant<T>(
        val kilde: Grunnlagsopplysning.Kilde,
        val verdi: T
    ) : Opplysning<T>()
}