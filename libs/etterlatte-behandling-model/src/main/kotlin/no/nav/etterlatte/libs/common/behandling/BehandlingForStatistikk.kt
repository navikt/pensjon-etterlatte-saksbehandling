package no.nav.etterlatte.libs.common.behandling

import java.time.LocalDateTime
import java.util.UUID

data class BehandlingForStatistikk(
    val id: UUID,
    val sakType: SakType,
    val behandlingOpprettet: LocalDateTime,
    val soeknadMottattDato: LocalDateTime?,
    val innsender: String?,
    val soeker: String,
    val gjenlevende: List<String>?,
    val avdoed: List<String>?,
    val soesken: List<String>?,
    val status: BehandlingStatus,
    val revurderingsaarsak: RevurderingAarsak? = null,
    val prosesstype: Prosesstype,
    val enhet: String,
)
