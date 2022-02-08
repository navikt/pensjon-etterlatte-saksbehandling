package no.nav.etterlatte.libs.common.behandling.opplysningstyper

import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.Svar

data class Utenlandsadresse(
    val adresseIUtlandet: Svar?,
    val land: String?,
    val adresse: String?,
    val foedselsnummer: String?
)