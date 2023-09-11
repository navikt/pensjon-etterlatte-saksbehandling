package no.nav.etterlatte.behandling

import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.behandling.SakType

data class BehandlingsBehov(
    val sakId: Long,
    val persongalleri: Persongalleri,
    val mottattDato: String
)

data class NyBehandlingRequest(
    val sakType: SakType,
    val persongalleri: Persongalleri,
    val mottattDato: String,
    val spraak: String
)