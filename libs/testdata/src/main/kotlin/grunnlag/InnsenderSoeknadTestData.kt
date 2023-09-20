package grunnlag

import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.InnsenderSoeknad
import no.nav.etterlatte.libs.common.innsendtsoeknad.common.PersonType
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator

fun innsenderSoeknad(fnr: String) =
    InnsenderSoeknad(
        PersonType.INNSENDER,
        "Innsend",
        "Innsender",
        Folkeregisteridentifikator.of(fnr),
    )
