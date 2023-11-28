package no.nav.etterlatte.brev.behandling

import no.nav.etterlatte.brev.model.Adresse
import no.nav.etterlatte.brev.model.Mottaker
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.person.VergeAdresse
import no.nav.pensjon.brevbaker.api.model.Foedselsnummer
import java.time.LocalDate

data class PersonerISak(
    val innsender: Innsender?,
    val soeker: Soeker,
    val avdoede: List<Avdoed>,
    val verge: Verge?,
)

data class Innsender(val fnr: Foedselsnummer)

data class Soeker(val fornavn: String, val mellomnavn: String? = null, val etternavn: String, val fnr: Foedselsnummer)

data class Avdoed(val navn: String, val doedsdato: LocalDate)

data class Verge(
    val navn: String,
    val vedVergemaal: Boolean = false,
    val foedselsnummer: Folkeregisteridentifikator? = null,
    val adresse: VergeAdresse? = null,
) {
    fun toMottaker(): Mottaker {
        return Mottaker(
            navn = navn,
            foedselsnummer = foedselsnummer?.let { Foedselsnummer(it.value) },
            orgnummer = null,
            adresse =
                with(adresse!!) {
                    Adresse(adresseType, adresselinje1, adresselinje2, adresselinje3, postnummer, poststed, landkode, land)
                },
        )
    }
}
