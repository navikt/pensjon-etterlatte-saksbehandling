package no.nav.etterlatte.utbetaling.iverksetting.utbetaling

import no.nav.etterlatte.domene.vedtak.UtbetalingsperiodeType
import no.nav.etterlatte.domene.vedtak.Vedtak
import no.nav.etterlatte.utbetaling.common.Tidspunkt
import no.nav.etterlatte.utbetaling.common.forsteDagIMaaneden
import no.nav.etterlatte.utbetaling.common.sisteDagIMaaneden
import java.util.*

class UtbetalingMapper(
    val tidligereUtbetalinger: List<Utbetaling>,
    val vedtak: Vedtak,
    val utbetalingId: UUID = UUID.randomUUID(),
    val opprettet: Tidspunkt = Tidspunkt.now(),
) {

    private val utbetalingsperioder = vedtak.pensjonTilUtbetaling!!.sortedBy { it.periode.fom }

    fun opprettUtbetaling(): Utbetaling = utbetaling(utbetalingslinjer())

    private fun utbetalingslinjer() = utbetalingsperioder.map {
        Utbetalingslinje(
            id = UtbetalingslinjeId(it.id),
            opprettet = opprettet,
            periode = Utbetalingsperiode(
                fra = forsteDagIMaaneden(it.periode.fom),
                til = it.periode.tom?.let { dato -> sisteDagIMaaneden(dato) }
            ),
            beloep = it.beloep,
            utbetalingId = utbetalingId,
            sakId = SakId(vedtak.sak.id),
            utbetalingslinjetype = when (it.type) {
                UtbetalingsperiodeType.OPPHOER -> Utbetalingslinjetype.OPPHOER
                UtbetalingsperiodeType.UTBETALING -> Utbetalingslinjetype.UTBETALING
            },
            erstatterId = finnErstatterId(utbetalingslinjeId = it.id)
        )
    }

    private fun utbetaling(utbetalingslinjer: List<Utbetalingslinje>) = Utbetaling(
        id = utbetalingId,
        sakId = SakId(vedtak.sak.id),
        behandlingId = BehandlingId(vedtak.behandling.id.toString()), // TODO: må erstattes til en maks 30 tegn lang nøkkel
        vedtakId = VedtakId(vedtak.vedtakId),
        status = UtbetalingStatus.SENDT,
        opprettet = opprettet,
        endret = opprettet,
        avstemmingsnoekkel = opprettet,
        stoenadsmottaker = Foedselsnummer(vedtak.sak.ident),
        saksbehandler = NavIdent(vedtak.vedtakFattet!!.ansvarligSaksbehandler),
        attestant = NavIdent(vedtak.attestasjon!!.attestant),
        vedtak = vedtak,
        utbetalingslinjer = utbetalingslinjer
    )

    /*
           1     2     3
V1: |-----|-----|----->
        4    5     6
V2:    |--|-----|----->
     */

    private fun finnErstatterId(utbetalingslinjeId: Long): UtbetalingslinjeId? {
        return if (utbetalingsperioder.indexOfFirst { it.id == utbetalingslinjeId } == 0) {
            finnIdSisteUtbetalingslinje()
        } else {
            UtbetalingslinjeId(
                finnForrigeIndeks(utbetalingslinjeId)
            )
        }
    }

    // TODO: gjøre dette på en sikrere/finere måte
    private fun finnForrigeIndeks(utbetalingslinjeId: Long) =
        utbetalingsperioder[utbetalingsperioder.indexOfFirst { it.id == utbetalingslinjeId } - 1].id

    private fun finnIdSisteUtbetalingslinje() = tidligereUtbetalinger.filter {
        it.status in listOf(
            UtbetalingStatus.GODKJENT,
            UtbetalingStatus.GODKJENT_MED_FEIL
        )
    }.maxByOrNull { it.opprettet.instant }?.let {
        it.utbetalingslinjer.last().id
    }
}