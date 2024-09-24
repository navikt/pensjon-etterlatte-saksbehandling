package no.nav.etterlatte.grunnlagsendring

import no.nav.etterlatte.libs.common.Enhetsnummer

data class SakMedEnhet(
    val id: Long,
    val enhet: Enhetsnummer,
)
