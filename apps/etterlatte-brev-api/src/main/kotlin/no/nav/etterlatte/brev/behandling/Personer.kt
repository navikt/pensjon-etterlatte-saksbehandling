package no.nav.etterlatte.brev.behandling

import no.nav.pensjon.brevbaker.api.model.Foedselsnummer
import java.time.LocalDate

data class Innsender(val navn: String, val fnr: Foedselsnummer)

data class Soeker(val fornavn: String, val mellomnavn: String? = null, val etternavn: String, val fnr: Foedselsnummer)

data class Avdoed(val navn: String, val doedsdato: LocalDate)