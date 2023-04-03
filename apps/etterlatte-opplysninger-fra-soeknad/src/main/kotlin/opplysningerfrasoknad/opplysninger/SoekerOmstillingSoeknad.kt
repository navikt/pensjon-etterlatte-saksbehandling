package no.nav.etterlatte.opplysningerfrasoknad.opplysninger

import no.nav.etterlatte.libs.common.innsendtsoeknad.common.PersonType
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator

data class SoekerOmstillingSoeknad(
    val type: PersonType,
    val fornavn: String,
    val etternavn: String,
    val foedselsnummer: Folkeregisteridentifikator,
    val adresse: String?,
    val statsborgerskap: String,
    val telefonnummer: String?,
    val sivilstatus: String
)