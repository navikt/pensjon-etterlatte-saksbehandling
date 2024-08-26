package no.nav.etterlatte.behandling.hendelse

import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import java.util.UUID

data class LagretHendelse(
    val id: Long,
    val hendelse: String,
    val opprettet: Tidspunkt,
    val inntruffet: Tidspunkt?,
    val vedtakId: Long?,
    val behandlingId: UUID,
    val sakId: no.nav.etterlatte.libs.common.sak.SakId,
    val ident: String?,
    val identType: String?,
    val kommentar: String?,
    val valgtBegrunnelse: String?,
)
