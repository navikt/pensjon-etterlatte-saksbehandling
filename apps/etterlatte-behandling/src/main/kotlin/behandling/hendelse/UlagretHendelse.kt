package no.nav.etterlatte.behandling.hendelse

import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import java.util.UUID

data class UlagretHendelse(
    val hendelse: String,
    val inntruffet: Tidspunkt?,
    val vedtakId: Long?,
    val behandlingId: UUID,
    val sakId: SakId,
    val ident: String?,
    val identType: String?,
    val kommentar: String?,
    val valgtBegrunnelse: String?,
)
