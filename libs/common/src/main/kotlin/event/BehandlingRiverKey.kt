package no.nav.etterlatte.libs.common.event

interface IBehandlingRiverKey {
    val behandlingObjectKey get() = "behandling"
}

object BehandlingRiverKey : IBehandlingRiverKey