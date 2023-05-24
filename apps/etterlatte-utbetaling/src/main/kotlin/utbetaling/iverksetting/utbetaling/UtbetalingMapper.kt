package no.nav.etterlatte.utbetaling.iverksetting.utbetaling

import no.nav.etterlatte.libs.common.behandling.RevurderingAarsak
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.utbetaling.common.forsteDagIMaaneden
import no.nav.etterlatte.utbetaling.common.sisteDagIMaaneden
import no.nav.etterlatte.utbetaling.common.toUUID30
import java.util.*

class UtbetalingMapper(
    val tidligereUtbetalinger: List<Utbetaling>,
    val vedtak: Utbetalingsvedtak,
    val utbetalingId: UUID = UUID.randomUUID(),
    val opprettet: Tidspunkt = Tidspunkt.now()
) {

    private val utbetalingsperioder = vedtak.pensjonTilUtbetaling.sortedBy { it.periode.fom }

    fun opprettUtbetaling(): Utbetaling {
        if (tidligereUtbetalinger.isEmpty() && utbetalingsperioder.size == 1 &&
            utbetalingsperioder.first().type == UtbetalingsperiodeType.OPPHOER
        ) {
            throw IngenEksisterendeUtbetalingException()
        }
        return utbetaling()
    }

    private fun utbetaling() = Utbetaling(
        id = utbetalingId,
        sakId = SakId(vedtak.sak.id),
        sakType = vedtak.sak.sakType,
        behandlingId = BehandlingId(
            vedtak.behandling.id,
            vedtak.behandling.id.toUUID30()
        ),
        vedtakId = VedtakId(vedtak.vedtakId),
        opprettet = opprettet,
        endret = opprettet,
        avstemmingsnoekkel = opprettet,
        stoenadsmottaker = Foedselsnummer(vedtak.sak.ident),
        saksbehandler = NavIdent(vedtak.vedtakFattet.ansvarligSaksbehandler),
        saksbehandlerEnhet = vedtak.vedtakFattet.ansvarligEnhet,
        attestant = NavIdent(vedtak.attestasjon.attestant),
        attestantEnhet = vedtak.attestasjon.attesterendeEnhet,
        vedtak = vedtak,
        utbetalingslinjer = utbetalingslinjer(vedtak.sak.sakType),
        utbetalingshendelser = listOf(
            Utbetalingshendelse(
                utbetalingId = utbetalingId,
                tidspunkt = opprettet,
                status = UtbetalingStatus.MOTTATT
            )
        )
    )

    private fun utbetalingslinjer(saktype: Saktype) = utbetalingsperioder.map {
        Utbetalingslinje(
            id = UtbetalingslinjeId(it.id),
            opprettet = opprettet,
            periode = PeriodeForUtbetaling(
                fra = forsteDagIMaaneden(it.periode.fom),
                til = it.periode.tom?.let { dato -> sisteDagIMaaneden(dato) }
            ),
            beloep = it.beloep,
            utbetalingId = utbetalingId,
            sakId = SakId(vedtak.sak.id),
            type = when (it.type) {
                UtbetalingsperiodeType.OPPHOER -> Utbetalingslinjetype.OPPHOER
                UtbetalingsperiodeType.UTBETALING -> Utbetalingslinjetype.UTBETALING
            },
            erstatterId = finnErstatterId(utbetalingslinjeId = it.id),
            klassifikasjonskode = when (saktype) {
                Saktype.BARNEPENSJON -> OppdragKlassifikasjonskode.BARNEPENSJON_OPTP
                Saktype.OMSTILLINGSSTOENAD -> TODO("Kanskje det er samme som over men vi kræsjer dette i stedet")
            },
            kjoereplan = when (vedtak.behandling.revurderingsaarsak) {
                RevurderingAarsak.REGULERING -> Kjoereplan.NESTE_PLANLAGTE_UTBETALING
                else -> Kjoereplan.MED_EN_GANG
            }
        )
    }

    private fun finnErstatterId(utbetalingslinjeId: Long): UtbetalingslinjeId? {
        return if (indeksForUtbetalingslinje(utbetalingslinjeId) == 0) {
            utbetalingslinjeIdForForrigeUtbetalingslinje()
        } else {
            utbetalingslinjeIdForForrigeUtbetalingslinje(utbetalingslinjeId)
        }
    }

    private fun utbetalingslinjeIdForForrigeUtbetalingslinje(utbetalingslinjeId: Long) =
        UtbetalingslinjeId(utbetalingsperioder[indeksForUtbetalingslinje(utbetalingslinjeId) - 1].id)

    private fun utbetalingslinjeIdForForrigeUtbetalingslinje() = tidligereUtbetalinger.filter {
        it.status() in listOf(
            UtbetalingStatus.GODKJENT,
            UtbetalingStatus.GODKJENT_MED_FEIL
        )
    }.maxByOrNull { it.opprettet }?.utbetalingslinjer?.last()?.id

    private fun indeksForUtbetalingslinje(utbetalingslinjeId: Long) =
        utbetalingsperioder.indexOfFirst { it.id == utbetalingslinjeId }
}

class IngenEksisterendeUtbetalingException : RuntimeException()