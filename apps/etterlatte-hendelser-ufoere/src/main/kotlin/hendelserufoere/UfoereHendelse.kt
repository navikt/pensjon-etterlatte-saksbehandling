package no.nav.etterlatte.hendelserufoere

import java.time.LocalDate

data class UfoereHendelse(
    val personIdent: String,
    val fodselsdato: LocalDate,
    val virkningsdato: LocalDate,
    val vedtaksType: VedtaksType,
)

enum class VedtaksType {
    INNV,
    ENDR,
    OPPH,
}
