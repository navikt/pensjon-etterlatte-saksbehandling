package no.nav.etterlatte.libs.common.grunnlag

import com.fasterxml.jackson.databind.JsonNode
import no.nav.etterlatte.libs.common.person.PersonRolle

class Grunnlag(
    val soeker: Grunnlagsdata<JsonNode>,
    val familie: List<Grunnlagsdata<JsonNode>>,
    val sak: Grunnlagsdata<JsonNode>,
    val metadata: Metadata
) {
    companion object {
        fun empty() = Grunnlag(
            soeker = emptyMap(),
            familie = listOf(),
            sak = mapOf(),
            metadata = Metadata(0, 0)
        )
    }

    fun hentAvdoed(): Grunnlagsdata<JsonNode> = hentFamiliemedlem(PersonRolle.AVDOED)
    fun hentGjenlevende(): Grunnlagsdata<JsonNode> = hentFamiliemedlem(PersonRolle.GJENLEVENDE)
    fun hentAvdoede(): List<Grunnlagsdata<JsonNode>> = hentFamiliemedlemmer(PersonRolle.AVDOED)

    fun hentSoesken() =
        familie.filter { it.hentPersonrolle()?.verdi == PersonRolle.BARN }

    private fun hentFamiliemedlem(personRolle: PersonRolle) =
        familie.find { it.hentPersonrolle()?.verdi == personRolle }
            ?: throw IllegalStateException("Fant ikke familiemedlem med rolle $personRolle")

    private fun hentFamiliemedlemmer(personRolle: PersonRolle) =
        familie.filter { it.hentPersonrolle()?.verdi == personRolle }

    fun hentVersjon() = metadata.versjon
}

data class Metadata(val sakId: Long, val versjon: Long)