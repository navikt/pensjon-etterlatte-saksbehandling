package no.nav.etterlatte.behandling.domain

import no.nav.etterlatte.libs.common.behandling.BehandlingType

sealed class Omregning : Behandling() {
    override val type: BehandlingType = BehandlingType.OMREGNING
}