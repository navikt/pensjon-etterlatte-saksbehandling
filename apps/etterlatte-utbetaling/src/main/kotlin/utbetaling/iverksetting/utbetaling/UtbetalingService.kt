package no.nav.etterlatte.utbetaling.iverksetting.utbetaling

import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.utbetaling.iverksetting.UtbetalingEvent
import no.nav.etterlatte.utbetaling.iverksetting.UtbetalingResponse
import no.nav.etterlatte.utbetaling.iverksetting.oppdrag.OppdragMapper
import no.nav.etterlatte.utbetaling.iverksetting.oppdrag.OppdragSender
import no.nav.etterlatte.utbetaling.iverksetting.oppdrag.vedtakId
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.IverksettResultat.SendtTilOppdrag
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.IverksettResultat.UtbetalingForVedtakEksisterer
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.IverksettResultat.UtbetalingslinjerForVedtakEksisterer
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
        val utbetalingslinjerForVedtak =  utbetalingDao.hentUtbetalingslinjer(vedtak.pensjonTilUtbetaling)

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

    fun oppdaterKvittering(oppdrag: Oppdrag): Utbetaling {
        logger.info("Oppdaterer kvittering for oppdrag med vedtakId=${oppdrag.vedtakId()}")
        return utbetalingDao.oppdaterKvittering(oppdrag, Tidspunkt.now(clock))
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

    fun oppdaterStatusOgPubliserKvittering(oppdrag: Oppdrag, status: UtbetalingStatus) =
        utbetalingDao.oppdaterStatus(oppdrag.vedtakId(), status, Tidspunkt.now(clock))
            .also { rapidsConnection.publish("key", utbetalingEvent(oppdrag, status)) }


    private fun utbetalingEvent(oppdrag: Oppdrag, status: UtbetalingStatus) =
        UtbetalingEvent(
            utbetalingResponse = UtbetalingResponse(
                status = status,
                vedtakId = oppdrag.vedtakId(),
                feilmelding = oppdrag.kvitteringFeilmelding()
            )
        ).toJson()

    private fun Oppdrag.kvitteringFeilmelding() = when (this.mmel.kodeMelding) {
        "00" -> null
        else -> "${this.mmel.kodeMelding} ${this.mmel.beskrMelding}"
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
