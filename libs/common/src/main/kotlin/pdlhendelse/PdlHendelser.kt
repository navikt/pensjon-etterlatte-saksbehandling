package no.nav.etterlatte.libs.common.pdlhendelse

import java.time.LocalDate


sealed interface PdlHendelse

data class Doedshendelse(
    val avdoedFnr: String,
    val doedsdato: LocalDate?
) : PdlHendelse
