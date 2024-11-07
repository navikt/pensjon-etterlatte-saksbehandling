package no.nav.etterlatte.pdl

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
    override val data: PdlSoekPerson? = null,
    override val errors: List<PdlResponseError>? = null,
) : PdlDataErrorResponse<PdlSoekPerson>

data class PdlSoekPerson(
    val sokPerson: PersonSearchResult? = null,
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
    val navn: List<PdlNavn>,
    val folkeregisteridentifikator: List<PdlFolkeregisteridentifikator>,
    val bostedsadresse: List<PdlBostedsadresse>?,
)
