package no.nav.etterlatte.behandling.hendelse

import no.nav.etterlatte.behandling.Behandling
import no.nav.etterlatte.behandling.BehandlingDao
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import java.time.LocalDateTime

fun registrerVedtakHendelseFelles(
    vedtakId: Long,
    hendelse: HendelseType,
    inntruffet: Tidspunkt,
    saksbehandler: String?,
    kommentar: String?,
    begrunnelse: String?,
    lagretBehandling: Behandling,
    behandlinger: BehandlingDao,
    hendelser: HendelseDao
): Behandling {
    if (hendelse.kreverSaksbehandler()) {
        requireNotNull(saksbehandler)
    }
    if (hendelse.erUnderkjent()) {
        requireNotNull(kommentar)
        requireNotNull(begrunnelse)
    }

    return behandlinger.lagreStatus(
        behandling = lagretBehandling.id,
        status = hendelse.tilBehandlingStatus(lagretBehandling),
        sistEndret = LocalDateTime.now()
    ).also {
        hendelser.vedtakHendelse(
            it,
            vedtakId,
            hendelse,
            inntruffet,
            saksbehandler,
            kommentar,
            begrunnelse
        )
    }
}

private fun Behandling.behandlingKanIkkeSettesUnderBehandling() = status == BehandlingStatus.FATTET_VEDTAK ||
    status == BehandlingStatus.RETURNERT ||
    status == BehandlingStatus.ATTESTERT

private fun HendelseType.kreverSaksbehandler() =
    this in listOf(HendelseType.FATTET, HendelseType.ATTESTERT, HendelseType.UNDERKJENT)

private fun HendelseType.erUnderkjent() = this == HendelseType.UNDERKJENT

private fun HendelseType.tilBehandlingStatus(behandling: Behandling) = when (this) {
    HendelseType.FATTET -> BehandlingStatus.FATTET_VEDTAK
    HendelseType.ATTESTERT -> BehandlingStatus.ATTESTERT
    HendelseType.UNDERKJENT -> BehandlingStatus.RETURNERT
    HendelseType.VILKAARSVURDERT, HendelseType.BEREGNET -> {
        if (behandling.behandlingKanIkkeSettesUnderBehandling()) {
            behandling.status
        } else {
            BehandlingStatus.VILKAARSVURDERT
        }
    }

    HendelseType.IVERKSATT -> BehandlingStatus.IVERKSATT
}