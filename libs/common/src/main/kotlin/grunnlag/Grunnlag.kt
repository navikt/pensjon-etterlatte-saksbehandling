package no.nav.etterlatte.libs.common.grunnlag

import com.fasterxml.jackson.databind.JsonNode
import no.nav.etterlatte.libs.common.person.PersonRolle
import java.time.YearMonth

data class Grunnlag(
    val saksId: Long,
    val grunnlag: List<Grunnlagsopplysning<JsonNode>>,
    val versjon: Long
)

class Opplysningsgrunnlag(
    val søker: Grunnlagsdata<JsonNode>,
    val familie: List<Grunnlagsdata<JsonNode>>,
    val sak: Grunnlagsdata<JsonNode>,
    private val metadata: Metadata
) {
    companion object {
        fun empty() = Opplysningsgrunnlag(
            søker = emptyMap(),
            familie = listOf(),
            sak = mapOf(),
            metadata = Metadata(0, 0)
        )
    }

    fun hentAvdoed(): Grunnlagsdata<JsonNode> = hentFamiliemedlem(PersonRolle.AVDOED)
    fun hentGjenlevende(): Grunnlagsdata<JsonNode> = hentFamiliemedlem(PersonRolle.GJENLEVENDE)

    private fun hentFamiliemedlem(personRolle: PersonRolle) =
        familie.find { it.hentPersonrolle()?.verdi == personRolle }!!

    fun hentVersjon() = metadata.versjon
}

data class Metadata(val sakId: Long, val versjon: Long)

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