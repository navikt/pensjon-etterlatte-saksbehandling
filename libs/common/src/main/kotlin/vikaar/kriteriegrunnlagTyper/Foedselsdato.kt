package no.nav.etterlatte.libs.common.vikaar.kriteriegrunnlagTyper

import no.nav.etterlatte.libs.common.person.Foedselsnummer
import java.time.LocalDate

data class Foedselsdato(
    val foedselsdato: LocalDate?,
    val foedselsnummer: Foedselsnummer
)