package no.nav.etterlatte.common

import no.nav.etterlatte.behandling.domain.Behandling
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.Virkningstidspunkt

fun List<Behandling>.tidligsteIverksatteVirkningstidspunkt(): Virkningstidspunkt? =
    this.filter { it.status in listOf(BehandlingStatus.IVERKSATT, BehandlingStatus.SAMORDNET) }
        .mapNotNull { it.virkningstidspunkt }
        .minByOrNull { it.dato }
