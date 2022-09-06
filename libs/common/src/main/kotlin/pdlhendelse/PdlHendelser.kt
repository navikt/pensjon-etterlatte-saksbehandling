package no.nav.etterlatte.libs.common.pdlhendelse

import java.time.LocalDate

sealed interface PdlHendelse

data class Doedshendelse(
    val avdoedFnr: String,
    val doedsdato: LocalDate?,
    val endringstype: Endringstype
) : PdlHendelse

data class UtflyttingsHendelse(
    val fnr: String,
    val tilflyttingsLand: String?,
    val tilflyttingsstedIUtlandet: String?,
    val utflyttingsdato: LocalDate?,
    val endringstype: Endringstype
)

enum class Endringstype {
    OPPRETTET, KORRIGERT, ANNULLERT, OPPHOERT
}