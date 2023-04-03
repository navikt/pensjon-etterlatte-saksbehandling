package no.nav.etterlatte.libs.common.grunnlag.opplysningstyper

import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.innsendtsoeknad.common.PersonType

data class GjenlevendeForelderSoeknad(
    val type: PersonType,
    val fornavn: String,
    val etternavn: String,
    val foedselsnummer: Folkeregisteridentifikator,
    val adresse: String,
    val statsborgerskap: String,
    val telefonnummer: String

)