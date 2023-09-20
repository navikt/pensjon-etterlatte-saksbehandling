package no.nav.etterlatte.libs.common.person

import no.nav.etterlatte.libs.common.behandling.SakType

data class HentGeografiskTilknytningRequest(
    val foedselsnummer: Folkeregisteridentifikator,
    val saktype: SakType,
)
