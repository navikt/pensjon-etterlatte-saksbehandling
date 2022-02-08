package no.nav.etterlatte.libs.common.behandling.opplysningstyper

import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.Svar

data class Doedsaarsak(
    val doedsaarsakSkyldesYrkesskadeEllerYrkessykdom: Svar,
    val fnr: String
)