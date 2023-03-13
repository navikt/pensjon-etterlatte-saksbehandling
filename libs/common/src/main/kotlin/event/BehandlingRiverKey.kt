package no.nav.etterlatte.libs.common.event

interface IBehandlingRiverKey {
    val behandlingObjectKey get() = "behandling"
    val sakIdKey get() = "sakId"
    val persongalleriKey get() = "persongalleri"
}

object BehandlingRiverKey : IBehandlingRiverKey