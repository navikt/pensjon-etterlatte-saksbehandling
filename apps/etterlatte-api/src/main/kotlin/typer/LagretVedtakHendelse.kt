package no.nav.etterlatte.typer

import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import java.util.*

data class LagretVedtakHendelse(
    val id: Long,
    val hendelse: String,
    val opprettet: Tidspunkt,
    val inntruffet: Tidspunkt?,
    val vedtakId: Long?,
    val behandlingId: UUID,
    val sakId: Long,
    val ident: String?,
    val identType: String?,
    val kommentar: String?,
    val valgtBegrunnelse: String?,
)

data class LagretVedtakHendelser(
    val hendelser: List<LagretVedtakHendelse>
)