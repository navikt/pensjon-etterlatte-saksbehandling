package no.nav.etterlatte.libs.common.behandling

import java.time.LocalDateTime
import java.util.UUID

data class BehandlingForStatistikk(
    val id: UUID,
    val sak: Long,
    val sakType: SakType,
    val behandlingOpprettet: LocalDateTime, // kun statistikk
    val soeknadMottattDato: LocalDateTime?, // kun statistikk
    val innsender: String?, // kun statistikk
    val soeker: String, // statistikk og vedtak
    val gjenlevende: List<String>?, // kun statistikk
    val avdoed: List<String>?, // kun statistikk
    val soesken: List<String>?, // kun statistikk
    val status: BehandlingStatus, // kun statistikk
    val behandlingType: BehandlingType,
    val virkningstidspunkt: Virkningstidspunkt?,
    val boddEllerArbeidetUtlandet: BoddEllerArbeidetUtlandet?,
    val revurderingsaarsak: RevurderingAarsak? = null,
    val revurderingInfo: RevurderingInfo?,
    val prosesstype: Prosesstype, // statistikk og trygdetid
    val enhet: String, // kun statistikk
)
