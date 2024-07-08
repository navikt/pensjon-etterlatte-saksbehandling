package no.nav.etterlatte.klage.modell

class Feil(
    message: String,
    throwable: Throwable? = null,
) : RuntimeException(message, throwable)
