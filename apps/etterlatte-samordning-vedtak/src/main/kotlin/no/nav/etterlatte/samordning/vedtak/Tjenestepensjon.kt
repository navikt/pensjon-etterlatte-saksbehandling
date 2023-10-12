package no.nav.etterlatte.samordning.vedtak

import java.time.LocalDate

data class SamhandlerPersonDto(
    val fnr: String,
    val forhold: List<TjenestepensjonForhold> = emptyList(),
)

class TjenestepensjonForhold(
    val tpNr: String,
    val kilde: String,
    val ytelser: List<SamhandlerYtelseDto> = emptyList(),
)

data class SamhandlerYtelseDto(
    val ytelseType: String,
    val datoInnmeldtYtelseFom: LocalDate? = null,
    val datoYtelseIverksattFom: LocalDate? = null,
    val datoYtelseIverksattTom: LocalDate? = null,
)

data class Tjenestepensjonnummer(val value: String) {
    init {
        require(value == value.replace(Regex("[^0-9]"), ""))
    }
}

class TjenestepensjonManglendeTilgangException(override val message: String, cause: Throwable? = null) : Exception(message, cause)

class TjenestepensjonUgyldigForesporselException(override val message: String, cause: Throwable?) : Exception(message, cause)

class TjenestepensjonIkkeFunnetException(override val message: String, cause: Throwable?) : Exception(message, cause)
