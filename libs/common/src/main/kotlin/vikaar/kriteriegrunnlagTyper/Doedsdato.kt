package no.nav.etterlatte.libs.common.vikaar.kriteriegrunnlagTyper

import no.nav.etterlatte.libs.common.person.Foedselsnummer
import java.time.LocalDate

data class Doedsdato(
    val doedsdato: LocalDate?,
    val foedselsnummer: Foedselsnummer
)