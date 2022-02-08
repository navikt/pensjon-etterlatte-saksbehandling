package no.nav.etterlatte.libs.common.behandling.opplysningstyper

import java.time.LocalDate

data class Foedselsdato(
    val foedselsdato: LocalDate,
    val foedselsnummer: String
)