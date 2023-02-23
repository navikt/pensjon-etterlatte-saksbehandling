package no.nav.etterlatte.libs.common.grunnlag.opplysningstyper

import no.nav.etterlatte.libs.common.person.Foedselsnummer
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.PersonType

data class SoekerOmstillingSoeknad(
    val type: PersonType,
    val fornavn: String,
    val etternavn: String,
    val foedselsnummer: Foedselsnummer,
    val adresse: String?,
    val statsborgerskap: String,
    val telefonnummer: String?,
    val sivilstatus: String
)