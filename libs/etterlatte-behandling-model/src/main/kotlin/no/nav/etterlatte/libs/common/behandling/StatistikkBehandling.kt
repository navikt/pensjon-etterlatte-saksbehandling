package no.nav.etterlatte.libs.common.behandling

import no.nav.etterlatte.common.Enhet
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
    val utlandstilknytning: Utlandstilknytning?,
    val revurderingsaarsak: Revurderingaarsak? = null,
    val revurderingInfo: RevurderingInfo?,
    val prosesstype: Prosesstype,
    val enhet: Enhet,
    val kilde: Vedtaksloesning,
    val relatertBehandlingId: String?,
    val pesysId: Long?,
)

enum class BehandlingHendelseType : EventnameHendelseType {
    OPPRETTET,
    PAA_VENT,
    AV_VENT,
    AVBRUTT,
    ;

    override fun lagEventnameForType(): String = "BEHANDLING:${this.name}"
}

const val STATISTIKKBEHANDLING_RIVER_KEY = "behandling"
const val BEHANDLING_ID_PAA_VENT_RIVER_KEY = "behandling_vent"
const val PAA_VENT_AARSAK_KEY = "paaVentAarsak"

enum class PaaVentAarsak {
    OPPLYSNING_FRA_BRUKER,
    OPPLYSNING_FRA_ANDRE,
    KRAVGRUNNLAG_SPERRET,
    ANNET,
}
