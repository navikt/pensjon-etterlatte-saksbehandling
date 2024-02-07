package no.nav.etterlatte.libs.common.behandling

import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.event.EventnameHendelseType
import no.nav.etterlatte.libs.common.sak.Sak
import java.time.LocalDateTime
import java.util.UUID

data class StatistikkBehandling(
    val id: UUID,
    val sak: Sak,
    val sistEndret: LocalDateTime,
    val behandlingOpprettet: LocalDateTime,
    val soeknadMottattDato: LocalDateTime?,
    val innsender: String?,
    val soeker: String,
    val gjenlevende: List<String>?,
    val avdoed: List<String>?,
    val soesken: List<String>?,
    val status: BehandlingStatus,
    val behandlingType: BehandlingType,
    val virkningstidspunkt: Virkningstidspunkt?,
    val boddEllerArbeidetUtlandet: BoddEllerArbeidetUtlandet?,
    val revurderingsaarsak: Revurderingaarsak? = null,
    val revurderingInfo: RevurderingInfo?,
    val prosesstype: Prosesstype,
    val enhet: String,
    val kilde: Vedtaksloesning,
    val pesysId: Long?,
)

enum class BehandlingHendelseType : EventnameHendelseType {
    OPPRETTET,
    AVBRUTT,
    ;

    override fun lagEventnameForType(): String = "BEHANDLING:${this.name}"
}

const val BEHANDLING_RIVER_KEY = "behandling"
