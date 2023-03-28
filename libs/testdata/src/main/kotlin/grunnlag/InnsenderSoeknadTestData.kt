package grunnlag

import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.InnsenderSoeknad
import no.nav.etterlatte.libs.common.innsendtsoeknad.common.PersonType
import no.nav.etterlatte.libs.common.person.Foedselsnummer

fun innsenderSoeknad(fnr: String) = InnsenderSoeknad(
    PersonType.INNSENDER,
    "Innsend",
    "Innsender",
    Foedselsnummer.of(fnr)
)