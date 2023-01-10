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
    hendelser: HendelseDao,
    behandlingDao: BehandlingDao
) {
    if (hendelse.kreverSaksbehandler()) {
        requireNotNull(saksbehandler)
    }
    if (hendelse.erUnderkjent()) {
        requireNotNull(kommentar)
        requireNotNull(begrunnelse)
    }

    hendelser.vedtakHendelse(
        lagretBehandling.id,
        lagretBehandling.sak,
        vedtakId,
        hendelse,
        inntruffet,
        saksbehandler,
        kommentar,
        begrunnelse
    )

    if (hendelse.erIverksatt()) {
        behandlingDao.lagreStatus(lagretBehandling.id, BehandlingStatus.IVERKSATT, LocalDateTime.now())
    }
}

private fun HendelseType.kreverSaksbehandler() =
    this in listOf(HendelseType.FATTET, HendelseType.ATTESTERT, HendelseType.UNDERKJENT)

private fun HendelseType.erUnderkjent() = this == HendelseType.UNDERKJENT
private fun HendelseType.erIverksatt() = this == HendelseType.IVERKSATT