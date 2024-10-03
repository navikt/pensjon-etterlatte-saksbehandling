package no.nav.etterlatte.brev.behandling

import no.nav.pensjon.brevbaker.api.model.Foedselsnummer
import java.time.LocalDate

data class Avdoed(
    val fnr: Foedselsnummer,
    val navn: String,
    val doedsdato: LocalDate,
)
