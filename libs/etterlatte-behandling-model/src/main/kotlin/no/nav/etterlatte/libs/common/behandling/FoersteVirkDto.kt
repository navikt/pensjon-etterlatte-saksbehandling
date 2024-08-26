package no.nav.etterlatte.libs.common.behandling

import java.time.LocalDate

data class FoersteVirkDto(
    val foersteIverksatteVirkISak: LocalDate,
    val sakId: no.nav.etterlatte.libs.common.sak.SakId,
)
