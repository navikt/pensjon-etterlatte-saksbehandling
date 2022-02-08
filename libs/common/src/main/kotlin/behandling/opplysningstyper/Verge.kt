package no.nav.etterlatte.libs.common.behandling.opplysningstyper

import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.Svar

data class Verge(
    val barnHarVerge: Svar?,
    val fornavn: String?,
    val etternavn: String?,
    val foedselsnummer: String?
)