package no.nav.etterlatte.libs.common.behandling

import no.nav.etterlatte.libs.common.sak.SakId
import java.time.LocalDate

data class FoersteVirkDto(
    val foersteIverksatteVirkISak: LocalDate,
    val sakId: SakId,
)
