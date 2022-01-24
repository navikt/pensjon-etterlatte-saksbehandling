package no.nav.etterlatte.behandling

import no.nav.etterlatte.libs.common.soeknad.SoeknadType

data class Sak(val ident: String, val sakType: SoeknadType, val sakId:Long)