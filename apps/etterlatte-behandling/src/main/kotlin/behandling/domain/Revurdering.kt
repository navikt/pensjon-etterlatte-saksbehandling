package no.nav.etterlatte.behandling.domain

import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.KommerBarnetTilgode
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.behandling.Prosesstype
import no.nav.etterlatte.libs.common.behandling.RevurderingAarsak
import no.nav.etterlatte.libs.common.behandling.Virkningstidspunkt
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingUtfall
import java.time.LocalDateTime
import java.util.*

sealed class Revurdering(
    override val id: UUID,
    override val sak: Sak,
    override val behandlingOpprettet: LocalDateTime,
    override val sistEndret: LocalDateTime,
    override val status: BehandlingStatus,
    override val persongalleri: Persongalleri,
    override val kommerBarnetTilgode: KommerBarnetTilgode?,
    override val vilkaarUtfall: VilkaarsvurderingUtfall?,
    override val virkningstidspunkt: Virkningstidspunkt?,
    open val revurderingsaarsak: RevurderingAarsak,
    override val prosesstype: Prosesstype
) : Behandling() {
    override val type: BehandlingType = BehandlingType.REVURDERING

    abstract fun kopier(): Revurdering

    companion object {
        fun opprett(
            id: UUID,
            sak: Sak,
            behandlingOpprettet: LocalDateTime,
            sistEndret: LocalDateTime,
            status: BehandlingStatus,
            persongalleri: Persongalleri,
            kommerBarnetTilgode: KommerBarnetTilgode?,
            vilkaarUtfall: VilkaarsvurderingUtfall?,
            virkningstidspunkt: Virkningstidspunkt?,
            revurderingsaarsak: RevurderingAarsak,
            prosesstype: Prosesstype
        ) = when (prosesstype) {
            Prosesstype.MANUELL -> ManuellRevurdering(
                id,
                sak,
                behandlingOpprettet,
                sistEndret,
                status,
                persongalleri,
                kommerBarnetTilgode,
                vilkaarUtfall,
                virkningstidspunkt,
                revurderingsaarsak
            )

            Prosesstype.AUTOMATISK -> AutomatiskRevurdering(
                id,
                sak,
                behandlingOpprettet,
                sistEndret,
                status,
                persongalleri,
                kommerBarnetTilgode,
                vilkaarUtfall,
                virkningstidspunkt,
                revurderingsaarsak
            )
        }
    }
}