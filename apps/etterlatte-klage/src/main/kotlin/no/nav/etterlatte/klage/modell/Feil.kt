package no.nav.etterlatte.klage.modell

import io.ktor.http.HttpStatusCode

class Feil(
    message: String,
    val frontendFeilmelding: String? = null,
    val httpStatus: HttpStatusCode = HttpStatusCode.InternalServerError,
    throwable: Throwable? = null
) : RuntimeException(message, throwable) {

    constructor(
        message: String,
        throwable: Throwable?,
        httpStatus: HttpStatusCode = HttpStatusCode.InternalServerError
    ) :
        this(message, null, httpStatus, throwable)
}