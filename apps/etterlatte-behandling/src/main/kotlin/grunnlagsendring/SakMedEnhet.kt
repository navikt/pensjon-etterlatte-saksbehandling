package no.nav.etterlatte.grunnlagsendring

import no.nav.etterlatte.libs.common.Enhetsnummer
import no.nav.etterlatte.libs.common.sak.SakId

data class SakMedEnhet(
    val id: SakId,
    val enhet: Enhetsnummer,
)
