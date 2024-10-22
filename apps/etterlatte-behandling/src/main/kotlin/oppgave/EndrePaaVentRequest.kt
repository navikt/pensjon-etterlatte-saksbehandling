package no.nav.etterlatte.oppgave

import no.nav.etterlatte.libs.common.behandling.PaaVentAarsak

data class EndrePaaVentRequest(
    val aarsak: PaaVentAarsak? = null,
    val merknad: String,
    val paaVent: Boolean,
)
