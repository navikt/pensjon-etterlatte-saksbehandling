package no.nav.etterlatte.libs.common.grunnlag.opplysningstyper

import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.PersonType

data class InnsenderSoeknad(
    val type: PersonType,
    val fornavn: String,
    val etternavn: String,
    val foedselsnummer: Folkeregisteridentifikator
)