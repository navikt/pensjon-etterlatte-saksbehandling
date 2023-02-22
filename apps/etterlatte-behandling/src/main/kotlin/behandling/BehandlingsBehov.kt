package no.nav.etterlatte.behandling

import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.behandling.SakType

data class BehandlingsBehov(
    val sak: Long,
    val sakType: SakType,
    val persongalleri: Persongalleri,
    val mottattDato: String
)