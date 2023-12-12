package no.nav.etterlatte.behandling.domain

import no.nav.etterlatte.behandling.revurdering.RevurderingInfoMedBegrunnelse
import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.BoddEllerArbeidetUtlandet
import no.nav.etterlatte.libs.common.behandling.KommerBarnetTilgode
import no.nav.etterlatte.libs.common.behandling.Prosesstype
import no.nav.etterlatte.libs.common.behandling.Revurderingaarsak
import no.nav.etterlatte.libs.common.behandling.Utlandstilknytning
import no.nav.etterlatte.libs.common.behandling.Virkningstidspunkt
import no.nav.etterlatte.libs.common.sak.Sak
import java.time.LocalDateTime
import java.util.UUID

sealed class Revurdering(
    override val id: UUID,
    override val sak: Sak,
    override val behandlingOpprettet: LocalDateTime,
    override val sistEndret: LocalDateTime,
    override val status: BehandlingStatus,
    override val kommerBarnetTilgode: KommerBarnetTilgode?,
    override val virkningstidspunkt: Virkningstidspunkt?,
    override val boddEllerArbeidetUtlandet: BoddEllerArbeidetUtlandet?,
    open val revurderingsaarsak: Revurderingaarsak?,
    open val revurderingInfo: RevurderingInfoMedBegrunnelse?,
    override val prosesstype: Prosesstype,
    override val kilde: Vedtaksloesning,
    open val begrunnelse: String?,
) : Behandling() {
    override val type: BehandlingType = BehandlingType.REVURDERING

    abstract fun kopier(): Revurdering

    override fun begrunnelse() = begrunnelse

    companion object {
        fun opprett(
            id: UUID,
            sak: Sak,
            behandlingOpprettet: LocalDateTime,
            sistEndret: LocalDateTime,
            status: BehandlingStatus,
            kommerBarnetTilgode: KommerBarnetTilgode?,
            virkningstidspunkt: Virkningstidspunkt?,
            utlandstilknytning: Utlandstilknytning?,
            boddEllerArbeidetUtlandet: BoddEllerArbeidetUtlandet?,
            revurderingsaarsak: Revurderingaarsak,
            prosesstype: Prosesstype,
            kilde: Vedtaksloesning,
            revurderingInfo: RevurderingInfoMedBegrunnelse?,
            begrunnelse: String?,
        ) = when (prosesstype) {
            Prosesstype.MANUELL ->
                ManuellRevurdering(
                    id = id,
                    sak = sak,
                    behandlingOpprettet = behandlingOpprettet,
                    sistEndret = sistEndret,
                    status = status,
                    kommerBarnetTilgode = kommerBarnetTilgode,
                    virkningstidspunkt = virkningstidspunkt,
                    utlandstilknytning = utlandstilknytning,
                    boddEllerArbeidetUtlandet = boddEllerArbeidetUtlandet,
                    revurderingsaarsak = revurderingsaarsak,
                    revurderingInfo = revurderingInfo,
                    kilde = kilde,
                    begrunnelse = begrunnelse,
                )

            Prosesstype.AUTOMATISK ->
                AutomatiskRevurdering(
                    id = id,
                    sak = sak,
                    behandlingOpprettet = behandlingOpprettet,
                    sistEndret = sistEndret,
                    status = status,
                    kommerBarnetTilgode = kommerBarnetTilgode,
                    virkningstidspunkt = virkningstidspunkt,
                    utlandstilknytning = utlandstilknytning,
                    boddEllerArbeidetUtlandet = boddEllerArbeidetUtlandet,
                    revurderingsaarsak = revurderingsaarsak,
                    revurderingInfo = revurderingInfo,
                    kilde = kilde,
                    begrunnelse = begrunnelse,
                )
        }
    }
}
