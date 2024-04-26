package no.nav.etterlatte.sak

import no.nav.etterlatte.libs.common.behandling.UtlandstilknytningType

data class UtlandstilknytningRequest(
    val utlandstilknytningType: UtlandstilknytningType,
    val begrunnelse: String,
)
