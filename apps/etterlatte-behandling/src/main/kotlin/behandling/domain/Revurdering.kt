package no.nav.etterlatte.behandling.domain

import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.KommerBarnetTilgode
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.behandling.Prosesstype
import no.nav.etterlatte.libs.common.behandling.RevurderingAarsak
import no.nav.etterlatte.libs.common.behandling.Virkningstidspunkt
import no.nav.etterlatte.libs.common.sak.Sak
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
    override val virkningstidspunkt: Virkningstidspunkt?,
    open val revurderingsaarsak: RevurderingAarsak?,
    override val prosesstype: Prosesstype,
    override val kilde: Vedtaksloesning
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
            virkningstidspunkt: Virkningstidspunkt?,
            revurderingsaarsak: RevurderingAarsak,
            prosesstype: Prosesstype,
            kilde: Vedtaksloesning
        ) = when (prosesstype) {
            Prosesstype.MANUELL -> ManuellRevurdering(
                id,
                sak,
                behandlingOpprettet,
                sistEndret,
                status,
                persongalleri,
                kommerBarnetTilgode,
                virkningstidspunkt,
                revurderingsaarsak,
                kilde
            )

            Prosesstype.AUTOMATISK -> AutomatiskRevurdering(
                id,
                sak,
                behandlingOpprettet,
                sistEndret,
                status,
                persongalleri,
                kommerBarnetTilgode,
                virkningstidspunkt,
                revurderingsaarsak,
                kilde
            )
        }
    }
}