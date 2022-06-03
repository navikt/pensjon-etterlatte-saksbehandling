package no.nav.etterlatte.utbetaling.iverksetting.utbetaling

import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.utbetaling.iverksetting.oppdrag.OppdragMapper
import no.nav.etterlatte.utbetaling.iverksetting.oppdrag.OppdragSender
import no.nav.etterlatte.utbetaling.iverksetting.oppdrag.vedtakId
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.IverksettResultat.SendtTilOppdrag
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.IverksettResultat.UtbetalingForVedtakEksisterer
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.IverksettResultat.UtbetalingslinjerForVedtakEksisterer
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.OppdaterKvitteringResultat.KvitteringOppdatert
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.OppdaterKvitteringResultat.UgyldigStatus
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.OppdaterKvitteringResultat.UtbetalingFinnesIkke
import no.nav.helse.rapids_rivers.RapidsConnection
import no.trygdeetaten.skjema.oppdrag.Mmel
import no.trygdeetaten.skjema.oppdrag.Oppdrag
import org.slf4j.LoggerFactory
import java.time.Clock


class UtbetalingService(
    val oppdragMapper: OppdragMapper,
    val oppdragSender: OppdragSender,
    val utbetalingDao: UtbetalingDao,
    val rapidsConnection: RapidsConnection,
    val clock: Clock
) {
    fun iverksettUtbetaling(vedtak: Utbetalingsvedtak): IverksettResultat {
        val utbetalingForVedtak = utbetalingDao.hentUtbetaling(vedtak.vedtakId)
        val utbetalingslinjerForVedtak = utbetalingDao.hentUtbetalingslinjer(vedtak.pensjonTilUtbetaling)

        return when {
            utbetalingForVedtak != null ->
                UtbetalingForVedtakEksisterer(utbetalingForVedtak)

            utbetalingslinjerForVedtak.isNotEmpty() ->
                UtbetalingslinjerForVedtakEksisterer(utbetalingslinjerForVedtak)

            else -> {
                val utbetaling = UtbetalingMapper(
                    tidligereUtbetalinger = utbetalingDao.hentUtbetalinger(vedtak.sak.id),
                    vedtak = vedtak,
                ).opprettUtbetaling()

                val foerstegangsbehandling = vedtak.behandling.type == BehandlingType.FORSTEGANGSBEHANDLING
                val oppdrag = oppdragMapper.oppdragFraUtbetaling(utbetaling, foerstegangsbehandling)

                logger.info("Sender oppdrag for sakId=${vedtak.sak.id} med vedtakId=${vedtak.vedtakId} til oppdrag")
                oppdragSender.sendOppdrag(oppdrag)

                SendtTilOppdrag(utbetalingDao.opprettUtbetaling(utbetaling.copy(oppdrag = oppdrag)))
            }
        }
    }

    fun oppdaterKvittering(oppdrag: Oppdrag): OppdaterKvitteringResultat {
        val utbetaling = utbetalingDao.hentUtbetaling(oppdrag.vedtakId())

        return when {
            utbetaling == null -> UtbetalingFinnesIkke(oppdrag.vedtakId())
            utbetaling.status != UtbetalingStatus.SENDT -> UgyldigStatus(utbetaling.status)
            else -> {
                logger.info("Oppdaterer kvittering for oppdrag med vedtakId=${oppdrag.vedtakId()}")
                val oppdatertUtbetaling = utbetalingDao.oppdaterKvittering(oppdrag, Tidspunkt.now(clock))
                KvitteringOppdatert(oppdatertUtbetaling)
            }
        }
    }

    // TODO: sette inn kolonne i database som viser at kvittering er oppdatert manuelt?
    fun settKvitteringManuelt(vedtakId: Long) = utbetalingDao.hentUtbetaling(vedtakId)?.let {
        it.oppdrag?.apply {
            mmel = Mmel().apply {
                systemId = "231-OPPD" // TODO: en annen systemid her for å indikere manuell jobb?
                alvorlighetsgrad = "00"
            }
        }.let { utbetalingDao.oppdaterKvittering(it!!, Tidspunkt.now(clock)) } // TODO
    }

    companion object {
        private val logger = LoggerFactory.getLogger(UtbetalingService::class.java)
    }
}

sealed class IverksettResultat {
    class SendtTilOppdrag(val utbetaling: Utbetaling) : IverksettResultat()
    class UtbetalingslinjerForVedtakEksisterer(val utbetalingslinjer: List<Utbetalingslinje>) : IverksettResultat()
    class UtbetalingForVedtakEksisterer(val utbetaling: Utbetaling) : IverksettResultat()
}

sealed class OppdaterKvitteringResultat {
    class KvitteringOppdatert(val utbetaling: Utbetaling) : OppdaterKvitteringResultat()
    class UtbetalingFinnesIkke(val vedtakId: Long) : OppdaterKvitteringResultat()
    class UgyldigStatus(val status: UtbetalingStatus) : OppdaterKvitteringResultat()
}
