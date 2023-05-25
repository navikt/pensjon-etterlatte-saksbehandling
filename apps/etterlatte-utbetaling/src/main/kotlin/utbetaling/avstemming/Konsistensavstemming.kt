package no.nav.etterlatte.utbetaling.avstemming

import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.utbetaling.grensesnittavstemming.UUIDBase64
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.Foedselsnummer
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.Kjoereplan
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.NavIdent
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.SakId
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.Saktype
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.UtbetalingslinjeId
import java.math.BigDecimal
import java.time.LocalDate

data class Konsistensavstemming(
    val id: UUIDBase64,
    val sakType: Saktype,
    val opprettet: Tidspunkt,
    val avstemmingsdata: String?,
    val loependeFraOgMed: Tidspunkt,
    val opprettetTilOgMed: Tidspunkt,
    val loependeUtbetalinger: List<OppdragForKonsistensavstemming>
)

data class OppdragForKonsistensavstemming(
    val sakId: SakId,
    val sakType: Saktype,
    val fnr: Foedselsnummer,
    val utbetalingslinjer: List<OppdragslinjeForKonsistensavstemming>
)

data class OppdragslinjeForKonsistensavstemming(
    val id: UtbetalingslinjeId,
    val opprettet: Tidspunkt,
    val fraOgMed: LocalDate,
    val tilOgMed: LocalDate?,
    var forrigeUtbetalingslinjeId: UtbetalingslinjeId?,
    val beloep: BigDecimal?,
    val attestanter: List<NavIdent>,
    val kjoereplan: Kjoereplan
)