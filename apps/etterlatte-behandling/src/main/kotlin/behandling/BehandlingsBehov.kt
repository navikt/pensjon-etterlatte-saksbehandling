package no.nav.etterlatte.behandling

import no.nav.etterlatte.libs.common.behandling.Persongalleri

data class BehandlingsBehov(
    val sak: Long,
    val persongalleri: Persongalleri,
    val mottattDato: String
)