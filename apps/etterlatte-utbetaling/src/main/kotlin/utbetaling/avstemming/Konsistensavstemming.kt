package no.nav.etterlatte.utbetaling.avstemming

import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.utbetaling.common.UUID30
import no.nav.etterlatte.utbetaling.grensesnittavstemming.UUIDBase64
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.Foedselsnummer
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.NavIdent
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.SakId
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.Saktype
import java.time.LocalDate

data class Konsistensavstemming(
    val id: UUIDBase64,
    val sakType: Saktype,
    val opprettet: Tidspunkt,
    val avstemmingXmlRequest: String?,
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
    val id: UUID30,
    val opprettet: Tidspunkt,
    val fraOgMed: LocalDate,
    val tilOgMed: LocalDate,
    var forrigeUtbetalingslinjeId: UUID30?,
    val beloep: Int,
    val attestanter: List<NavIdent>
)