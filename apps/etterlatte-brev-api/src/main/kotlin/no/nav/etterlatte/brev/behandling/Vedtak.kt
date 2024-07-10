package no.nav.etterlatte.brev.behandling

import no.nav.etterlatte.libs.common.behandling.Klage
import no.nav.etterlatte.libs.common.behandling.RevurderingInfo
import no.nav.etterlatte.libs.common.tilbakekreving.Tilbakekreving
import no.nav.etterlatte.libs.common.vedtak.VedtakStatus
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import java.time.LocalDate
import java.time.YearMonth

fun VedtakType?.somTittel() = this?.name?.lowercase()?.replace("_", " ")

data class ForenkletVedtak(
    val id: Long,
    val status: VedtakStatus,
    val type: VedtakType,
    val sakenhet: String,
    val saksbehandlerIdent: String,
    val attestantIdent: String?,
    val vedtaksdato: LocalDate?,
    val virkningstidspunkt: YearMonth? = null,
    val revurderingInfo: RevurderingInfo? = null,
    val tilbakekreving: Tilbakekreving? = null,
    val klage: Klage? = null,
)
