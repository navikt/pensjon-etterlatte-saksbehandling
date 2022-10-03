package no.nav.etterlatte.libs.common.grunnlag

import com.fasterxml.jackson.databind.JsonNode
import no.nav.etterlatte.libs.common.person.PersonRolle

data class Grunnlag(
    val saksId: Long,
    val grunnlag: List<Grunnlagsopplysning<JsonNode>>,
    val versjon: Long
)

class Opplysningsgrunnlag(
    val søker: Grunnlagsdata<JsonNode>,
    val familie: List<Grunnlagsdata<JsonNode>>,
    val sak: Grunnlagsdata<JsonNode>,
    val metadata: Metadata
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