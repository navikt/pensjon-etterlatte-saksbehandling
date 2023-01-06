package no.nav.etterlatte.behandling.hendelse

import no.nav.etterlatte.behandling.Behandling
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt

fun registrerVedtakHendelseFelles(
    vedtakId: Long,
    hendelse: HendelseType,
    inntruffet: Tidspunkt,
    saksbehandler: String?,
    kommentar: String?,
    begrunnelse: String?,
    lagretBehandling: Behandling,
    hendelser: HendelseDao
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
}

private fun HendelseType.kreverSaksbehandler() =
    this in listOf(HendelseType.FATTET, HendelseType.ATTESTERT, HendelseType.UNDERKJENT)

private fun HendelseType.erUnderkjent() = this == HendelseType.UNDERKJENT