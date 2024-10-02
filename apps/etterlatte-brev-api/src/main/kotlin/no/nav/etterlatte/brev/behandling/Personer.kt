package no.nav.etterlatte.brev.behandling

import no.nav.etterlatte.brev.brevbaker.SoekerOgEventuellVerge
import no.nav.etterlatte.libs.common.person.Verge
import no.nav.pensjon.brevbaker.api.model.Foedselsnummer

data class PersonerISak(
    val innsender: Innsender?,
    val soeker: Soeker,
    val avdoede: List<Avdoed>,
    val verge: Verge?,
) {
    fun soekerOgEventuellVerge(): SoekerOgEventuellVerge = SoekerOgEventuellVerge(soeker, verge)
}

data class Innsender(
    val fnr: Foedselsnummer,
)

data class Soeker(
    val fornavn: String,
    val mellomnavn: String? = null,
    val etternavn: String,
    val fnr: Foedselsnummer,
    val under18: Boolean? = null,
    val foreldreloes: Boolean = false,
    val ufoere: Boolean = false,
)
