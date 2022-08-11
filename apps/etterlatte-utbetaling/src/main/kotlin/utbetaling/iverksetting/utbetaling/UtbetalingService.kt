package no.nav.etterlatte.utbetaling.iverksetting.utbetaling

import no.nav.etterlatte.libs.common.behandling.BehandlingType
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
        val dupliserteUtbetalingslinjer =
            utbetalingDao.hentDupliserteUtbetalingslinjer(vedtak.pensjonTilUtbetaling, vedtak.vedtakId)

        return when {
            utbetalingForVedtak.utbetalingEksisterer() ->
                UtbetalingForVedtakEksisterer(utbetalingForVedtak!!)

            dupliserteUtbetalingslinjer.isNotEmpty() ->
                UtbetalingslinjerForVedtakEksisterer(dupliserteUtbetalingslinjer)

            else -> {
                val utbetaling = UtbetalingMapper(
                    tidligereUtbetalinger = utbetalingDao.hentUtbetalinger(vedtak.sak.id),
                    vedtak = vedtak,
                ).opprettUtbetaling()

                oppdragMapper.oppdragFraUtbetaling(
                    utbetaling = utbetaling,
                    foerstegangsbehandling = vedtak.behandling.type == BehandlingType.FØRSTEGANGSBEHANDLING
                )
                    .also {
                        utbetalingDao.opprettUtbetaling(utbetaling.copy(oppdrag = it))
                        oppdragSender.sendOppdrag(it)
                    }.let {
                        utbetalingDao.nyUtbetalingshendelse(
                            utbetaling.vedtakId.value,
                            utbetaling.sendtUtbetalingshendelse(clock)
                        ).let { SendtTilOppdrag(it) }
                    }

            }
        }
    }

    fun oppdaterKvittering(oppdrag: Oppdrag): OppdaterKvitteringResultat {
        val utbetaling = utbetalingDao.hentUtbetaling(oppdrag.vedtakId())

        return when {
            utbetaling == null -> UtbetalingFinnesIkke(oppdrag.vedtakId())
            utbetaling.ugyldigStatus() -> {
                UgyldigStatus(
                    utbetaling.status()
                )
            }
            else -> {
                KvitteringOppdatert(
                    utbetalingDao.oppdaterKvittering(
                        oppdrag,
                        Tidspunkt.now(clock),
                        utbetaling.id
                    )
                ).also {
                    logger.info("Kvittering for oppdrag med vedtakId=${oppdrag.vedtakId()} oppdatert")
                }
            }
        }
    }

    fun settKvitteringManuelt(vedtakId: Long) = utbetalingDao.hentUtbetaling(vedtakId)?.let { utbetaling ->
        utbetaling.oppdrag?.apply {
            mmel = Mmel().apply {
                systemId = "231-OPPD"
                alvorlighetsgrad = "00"
            }
        }?.let { oppdrag -> utbetalingDao.oppdaterKvittering(oppdrag, Tidspunkt.now(clock), utbetaling.id) }
    }

    fun Utbetaling.sendtUtbetalingshendelse(clock: Clock) = Utbetalingshendelse(
        utbetalingId = this.id,
        status = UtbetalingStatus.SENDT,
        tidspunkt = Tidspunkt.now(clock)
    )

    fun Utbetaling.ugyldigStatus() =
        this.status() != UtbetalingStatus.SENDT && this.status() != UtbetalingStatus.MOTTATT


    fun Utbetaling?.utbetalingEksisterer() =
        this != null && this.status() != UtbetalingStatus.MOTTATT

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
