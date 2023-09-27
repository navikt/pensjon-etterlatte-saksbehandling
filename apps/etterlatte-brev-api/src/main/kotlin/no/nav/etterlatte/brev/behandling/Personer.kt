package no.nav.etterlatte.brev.behandling

import no.nav.pensjon.brevbaker.api.model.Foedselsnummer
import java.time.LocalDate

data class PersonerISak(
    val innsender: Innsender?,
    val soeker: Soeker,
    val avdoed: Avdoed,
    val verge: Verge?,
)

data class Innsender(val fnr: Foedselsnummer)

data class Verge(val navn: String)

data class Soeker(val fornavn: String, val mellomnavn: String? = null, val etternavn: String, val fnr: Foedselsnummer)

data class Avdoed(val navn: String, val doedsdato: LocalDate)
