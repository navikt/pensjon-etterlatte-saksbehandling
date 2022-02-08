package no.nav.etterlatte.libs.common.behandling.opplysningstyper

import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.Svar

data class Militaertjeneste(
    val harHattMilitaertjeneste: Svar?,
    val aarstallForMilitaerTjeneste: String?,
    val foedselsnummer: String
)