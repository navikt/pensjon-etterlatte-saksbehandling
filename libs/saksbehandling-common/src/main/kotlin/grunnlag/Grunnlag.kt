package no.nav.etterlatte.libs.common.grunnlag

import com.fasterxml.jackson.databind.JsonNode
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype
import no.nav.etterlatte.libs.common.person.PersonRolle
import no.nav.etterlatte.libs.common.sak.SakId

class Grunnlag(
    val soeker: Grunnlagsdata<JsonNode>,
    val familie: List<Grunnlagsdata<JsonNode>>,
    val sak: Grunnlagsdata<JsonNode>,
    val metadata: Metadata,
) {
    companion object {
        fun empty() =
            Grunnlag(
                soeker = emptyMap(),
                familie = listOf(),
                sak = mapOf(),
                metadata = Metadata(0, 0),
            )
    }

    fun hentInnsender(): Grunnlagsdata<JsonNode> = hentFamiliemedlem(PersonRolle.INNSENDER)

    fun hentPotensiellGjenlevende(): Grunnlagsdata<JsonNode>? = hentFamiliemedlemNullable(PersonRolle.GJENLEVENDE)

    fun hentAvdoede(): List<Grunnlagsdata<JsonNode>> = hentFamiliemedlemmer(PersonRolle.AVDOED)

    @Deprecated(
        "Denne er ikke safe med tanke på endringer i persongalleri / feil i gammelt grunnlag",
        replaceWith =
            ReplaceWith("hentAvdoede().flatMap { it.barn }"),
    )
    fun hentSoesken() = familie.filter { it.hentPersonrolle()?.verdi in listOf(PersonRolle.BARN, PersonRolle.TILKNYTTET_BARN) }

    private fun hentFamiliemedlemNullable(personRolle: PersonRolle): Grunnlagsdata<JsonNode>? {
        val aktuellePersoner = folkMedRolle(personRolle)
        return familie
            .filter { it.hentFoedselsnummer()?.verdi?.value in aktuellePersoner }
            .find { it.hentPersonrolle()?.verdi == personRolle }
    }

    private fun hentFamiliemedlem(personRolle: PersonRolle) =
        hentFamiliemedlemNullable(personRolle)
            ?: throw IllegalStateException("Fant ikke familiemedlem med rolle $personRolle")

    private fun hentFamiliemedlemmer(personRolle: PersonRolle): List<Grunnlagsdata<JsonNode>> {
        val aktuellePersoner = folkMedRolle(personRolle)

        return familie
            .filter { it.hentFoedselsnummer()?.verdi?.value in aktuellePersoner }
            .filter { it.hentPersonrolle()?.verdi == personRolle }
    }

    fun hentVersjon() = metadata.versjon
}

fun Grunnlag.folkMedRolle(rolle: PersonRolle): List<String> {
    val persongalleri = this.sak.hentKonstantOpplysning<Persongalleri>(Opplysningstype.PERSONGALLERI_V1)?.verdi
    return when (rolle) {
        PersonRolle.INNSENDER -> listOfNotNull(persongalleri?.innsender)
        PersonRolle.BARN -> throw IllegalArgumentException("Grunnlag vet ikke hvem som er barn")
        PersonRolle.AVDOED -> persongalleri?.avdoed ?: emptyList()
        PersonRolle.GJENLEVENDE -> persongalleri?.gjenlevende ?: emptyList()
        PersonRolle.TILKNYTTET_BARN -> throw IllegalArgumentException("Grunnlag vet ikke hvem som er barn")
    }
}

data class Metadata(
    val sakId: SakId,
    val versjon: Long,
)
