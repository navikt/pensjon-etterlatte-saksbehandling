package no.nav.etterlatte.behandling.domain

import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.behandling.BehandlingOpprinnelse
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.BoddEllerArbeidetUtlandet
import no.nav.etterlatte.libs.common.behandling.Prosesstype
import no.nav.etterlatte.libs.common.behandling.Revurderingaarsak
import no.nav.etterlatte.libs.common.behandling.TidligereFamiliepleier
import no.nav.etterlatte.libs.common.behandling.Utlandstilknytning
import no.nav.etterlatte.libs.common.behandling.Virkningstidspunkt
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.UUID

data class OpprettBehandling(
    val type: BehandlingType,
    val sakId: SakId,
    val status: BehandlingStatus,
    val soeknadMottattDato: LocalDateTime? = null,
    val virkningstidspunkt: Virkningstidspunkt? = null,
    val utlandstilknytning: Utlandstilknytning? = null,
    val boddEllerArbeidetUtlandet: BoddEllerArbeidetUtlandet? = null,
    val revurderingsAarsak: Revurderingaarsak? = null,
    val prosesstype: Prosesstype = Prosesstype.MANUELL,
    val vedtaksloesning: Vedtaksloesning,
    val begrunnelse: String? = null,
    val relatertBehandlingId: String? = null,
    val sendeBrev: Boolean,
    val opphoerFraOgMed: YearMonth? = null,
    val tidligereFamiliepleier: TidligereFamiliepleier? = null,
    val opprinnelse: BehandlingOpprinnelse,
) {
    val id: UUID = UUID.randomUUID()
    val opprettet: Tidspunkt = Tidspunkt.now()
}

data class BehandlingOpprettet(
    val timestamp: Tidspunkt,
    val id: UUID,
    val sak: SakId,
)

data class OpphoerFraTidligereBehandling(
    val opphoerFraOgMed: YearMonth,
    val behandlingId: UUID,
)

fun OpprettBehandling.toBehandlingOpprettet() = BehandlingOpprettet(opprettet, id, sakId)
