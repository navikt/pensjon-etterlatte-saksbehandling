package no.nav.etterlatte.libs.common.pdl

import no.nav.etterlatte.libs.common.feilhaandtering.ForespoerselException
import no.nav.etterlatte.libs.common.feilhaandtering.IkkeFunnetException

enum class PdlFeilAarsak {
    FANT_IKKE_PERSON,
    INGEN_IDENT_FAMILIERELASJON,
    INTERNAL_SERVER_ERROR,
}

class FantIkkePersonException(override val detail: String = "Fant ikke forespurt person i PDL") : IkkeFunnetException(
    code = PdlFeilAarsak.FANT_IKKE_PERSON.name,
    detail = detail,
)

class AkseptererIkkePersonerUtenIdentException(override val detail: String = "En person i familierelasjon mangler ident") :
    ForespoerselException(
        status = 400,
        code = PdlFeilAarsak.INGEN_IDENT_FAMILIERELASJON.name,
        detail = detail,
    )

class PdlInternalServerError : Exception()
