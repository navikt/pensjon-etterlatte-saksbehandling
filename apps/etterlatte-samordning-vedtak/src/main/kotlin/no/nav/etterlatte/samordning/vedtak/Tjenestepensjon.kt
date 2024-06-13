package no.nav.etterlatte.samordning.vedtak

import no.nav.etterlatte.libs.common.feilhaandtering.ForespoerselException
import no.nav.etterlatte.libs.common.feilhaandtering.IkkeFunnetException
import no.nav.etterlatte.libs.common.feilhaandtering.IkkeTillattException
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException

data class Tjenestepensjonnummer(
    val value: String,
) {
    init {
        require(value == value.replace(Regex("[^0-9]"), ""))
    }
}

class TjenestepensjonManglendeTilgangException(
    detail: String,
) : IkkeTillattException(
        code = "010-TP-TILGANG",
        detail = detail,
        meta = getMeta(),
    )

class TjenestepensjonUgyldigForesporselException(
    detail: String,
) : UgyldigForespoerselException(
        code = "011-TP-FORESPOERSEL",
        detail = detail,
        meta = getMeta(),
    )

class TjenestepensjonIkkeFunnetException(
    detail: String,
) : IkkeFunnetException(
        code = "012-TP-IKKE-FUNNET",
        detail = detail,
        meta = getMeta(),
    )

class TjenestepensjonInternFeil(
    detail: String,
) : ForespoerselException(
        status = 500,
        code = "TP-INTERNFEIL",
        detail = detail,
        meta = getMeta(),
    )
