package no.nav.etterlatte.brev.behandling

import no.nav.pensjon.brev.api.model.Foedselsnummer
import java.time.LocalDate

data class Innsender(val navn: String, val fnr: Foedselsnummer)

data class Soeker(val navn: String, val fnr: Foedselsnummer)

data class Avdoed(val navn: String, val doedsdato: LocalDate)