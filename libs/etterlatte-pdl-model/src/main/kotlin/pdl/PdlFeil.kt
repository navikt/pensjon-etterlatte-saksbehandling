package pdl

data class PdlFeil(val aarsak: PdlFeilAarsak, val detaljer: String?)

enum class PdlFeilAarsak {
    FANT_IKKE_PERSON, INGEN_IDENT_FAMILIERELASJON
}