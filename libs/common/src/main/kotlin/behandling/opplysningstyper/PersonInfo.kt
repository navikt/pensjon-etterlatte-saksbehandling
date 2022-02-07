package no.nav.etterlatte.libs.common.behandling.opplysningstyper

import no.nav.etterlatte.libs.common.person.Foedselsnummer
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.Person
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.PersonType

data class PersonInfo(
    override val fornavn: String,
    override val etternavn: String,
    override val foedselsnummer: Foedselsnummer,
    override val type: PersonType,
) : Person