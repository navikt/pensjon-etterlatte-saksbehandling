package no.nav.etterlatte.behandling.domain

import no.nav.etterlatte.behandling.manueltopphoer.ManueltOpphoerAarsak
import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.behandling.Prosesstype
import no.nav.etterlatte.libs.common.behandling.RevurderingAarsak
import no.nav.etterlatte.libs.common.behandling.Virkningstidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import java.time.LocalDateTime
import java.util.*

data class OpprettBehandling(
    val type: BehandlingType,
    val sakId: Long,
    val status: BehandlingStatus,
    val persongalleri: Persongalleri,
    val soeknadMottattDato: LocalDateTime? = null,
    val virkningstidspunkt: Virkningstidspunkt? = null,
    val revurderingsAarsak: RevurderingAarsak? = null,
    val opphoerAarsaker: List<ManueltOpphoerAarsak>? = null,
    val fritekstAarsak: String? = null,
    val prosesstype: Prosesstype = Prosesstype.MANUELL,
    val kilde: Vedtaksloesning,
    val merknad: String? = null
) {
    val id: UUID = UUID.randomUUID()
    val opprettet: Tidspunkt = Tidspunkt.now()
}

data class BehandlingOpprettet(
    val timestamp: Tidspunkt,
    val id: UUID,
    val sak: Long
)

fun OpprettBehandling.toBehandlingOpprettet() = BehandlingOpprettet(opprettet, id, sakId)