package no.nav.etterlatte.libs.common.pdl

import no.nav.etterlatte.libs.common.feilhaandtering.IkkeFunnetException

enum class PdlFeilAarsak {
    FANT_IKKE_PERSON,
    INTERNAL_SERVER_ERROR,
}

class FantIkkePersonException(
    override val detail: String = "Fant ikke forespurt person i PDL",
    override val cause: Throwable? = null,
) : IkkeFunnetException(
        code = PdlFeilAarsak.FANT_IKKE_PERSON.name,
        detail = detail,
    )
