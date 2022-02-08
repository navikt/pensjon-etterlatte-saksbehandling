package no.nav.etterlatte.libs.common.behandling.opplysningstyper

import no.nav.etterlatte.libs.common.person.Foedselsnummer
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.Person
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.PersonType

data class PersonInfo(
    val fornavn: String,
    val etternavn: String,
    val foedselsnummer: Foedselsnummer,
    val type: PersonType,
)