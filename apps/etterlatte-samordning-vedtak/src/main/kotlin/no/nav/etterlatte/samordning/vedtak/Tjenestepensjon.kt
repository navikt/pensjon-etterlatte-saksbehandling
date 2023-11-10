package no.nav.etterlatte.samordning.vedtak

import no.nav.etterlatte.libs.common.feilhaandtering.IkkeFunnetException
import no.nav.etterlatte.libs.common.feilhaandtering.IkkeTillattException
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
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

class TjenestepensjonManglendeTilgangException(detail: String) : IkkeTillattException(
    code = "010-TP-TILGANG",
    detail = detail,
    meta = getMeta(),
)

class TjenestepensjonUgyldigForesporselException(detail: String) : UgyldigForespoerselException(
    code = "011-TP-FORESPOERSEL",
    detail = detail,
    meta = getMeta(),
)

class TjenestepensjonIkkeFunnetException(detail: String) : IkkeFunnetException(
    code = "012-TP-IKKE-FUNNET",
    detail = detail,
    meta = getMeta(),
)
