package no.nav.etterlatte.statistikk.domain

import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import java.time.LocalDate
import java.time.YearMonth
import java.util.*

data class StoenadRad(
    val id: Long,
    val fnrSoeker: String,
    val fnrForeldre: List<String>,
    val fnrSoesken: List<String>,
    val anvendtTrygdetid: String,
    val nettoYtelse: String,
    val beregningType: String,
    val anvendtSats: String,
    val behandlingId: UUID,
    val sakId: Long,
    val sakNummer: Long,
    val tekniskTid: Tidspunkt,
    val sakYtelse: String,
    val versjon: String,
    val saksbehandler: String,
    val attestant: String?,
    val vedtakLoependeFom: LocalDate,
    val vedtakLoependeTom: LocalDate?,
    val beregning: Beregning?,
    val avkorting: Avkorting?,
    val vedtakType: VedtakType?,
    val sakUtland: SakUtland?,
    val virkningstidspunkt: YearMonth?,
    val utbetalingsdato: LocalDate?
)