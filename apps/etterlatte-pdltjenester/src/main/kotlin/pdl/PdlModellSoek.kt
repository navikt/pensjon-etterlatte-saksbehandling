package no.nav.etterlatte.pdl

import no.nav.etterlatte.libs.common.behandling.Navn
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator

data class PdlGraphSoekRequest(
    val query: String,
    val variables: PdlSoekVariables,
)

data class PdlSoekVariables(
    val paging: Paging,
    val criteria: List<Criteria>,
)

data class Paging(
    val pageNumber: String? = "1",
    val resultsPerPage: String? = "20",
)

data class Criteria(
    val fieldName: String,
    val searchRule: Map<String, String>,
) {
    constructor(fieldName: String, searchRule: SearchRule, searchValue: String) : this(
        fieldName,
        mapOf(searchRule.keyName to searchValue),
    )

    constructor(fieldName: String, searchMethod: String, searchValue: String) : this(
        fieldName,
        mapOf(searchMethod to searchValue),
    )
}

enum class SearchRule(
    val keyName: String,
) {
    EQUALS("equals"),
    CONTAINS("contains"),
    FUZZY("fuzzy"),
    FROM("from"),
    AFTER("after"),
}

data class PdlPersonSoekResponse(
    val data: PdlSoekPerson? = null,
    val errors: List<PdlResponseError>? = null,
)

data class PdlSoekPerson(
    val sokPerson: PersonSearchResult,
)

data class PersonSearchResult(
    val hits: List<PersonSearchHit>? = null,
    val pageNumber: Int,
    val totalHits: Int,
    val totalPages: Int,
)

data class PersonSearchHit(
    val person: SoekPersonTreff,
)

data class SoekPersonTreff(
    val navn: Navn,
    val foedselsnummer: Folkeregisteridentifikator,
    val bostedsadresse: PdlBostedsadresse,
)
