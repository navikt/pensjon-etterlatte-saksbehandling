package no.nav.etterlatte.behandling

import no.nav.etterlatte.behandling.hendelse.LagretHendelse
import no.nav.etterlatte.behandling.revurdering.RevurderingInfoMedBegrunnelse
import no.nav.etterlatte.libs.common.Enhetsnummer
import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.BoddEllerArbeidetUtlandet
import no.nav.etterlatte.libs.common.behandling.KommerBarnetTilgode
import no.nav.etterlatte.libs.common.behandling.Revurderingaarsak
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.behandling.Utlandstilknytning
import no.nav.etterlatte.libs.common.behandling.Virkningstidspunkt
import no.nav.etterlatte.libs.common.gyldigSoeknad.GyldighetsResultat
import no.nav.etterlatte.libs.common.sak.SakId
import java.time.LocalDateTime
import java.util.UUID

/**
 * Brukes av frontend. Se IDetaljertBehandling.ts
 **/
data class DetaljertBehandlingDto(
    val id: UUID,
    val sakId: SakId,
    val sakType: SakType,
    val sakEnhetId: Enhetsnummer,
    val gyldighetsprøving: GyldighetsResultat?,
    val soeknadMottattDato: LocalDateTime?,
    val virkningstidspunkt: Virkningstidspunkt?,
    val utlandstilknytning: Utlandstilknytning?,
    val boddEllerArbeidetUtlandet: BoddEllerArbeidetUtlandet?,
    val status: BehandlingStatus,
    val hendelser: List<LagretHendelse>?,
    val behandlingType: BehandlingType,
    val kommerBarnetTilgode: KommerBarnetTilgode?,
    val revurderingsaarsak: Revurderingaarsak?,
    val revurderinginfo: RevurderingInfoMedBegrunnelse?,
    val begrunnelse: String?,
    val kilde: Vedtaksloesning,
    val sendeBrev: Boolean,
    val viderefoertOpphoer: ViderefoertOpphoer?,
)
