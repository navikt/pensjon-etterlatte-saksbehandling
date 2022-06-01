package no.nav.etterlatte.utbetaling.iverksetting.utbetaling

import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.utbetaling.iverksetting.oppdrag.OppdragMapper
import no.nav.etterlatte.utbetaling.iverksetting.oppdrag.OppdragSender
import no.nav.etterlatte.utbetaling.iverksetting.oppdrag.vedtakId
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
    fun iverksettUtbetaling(vedtak: Utbetalingsvedtak): Utbetaling {
        val utbetaling = UtbetalingMapper(
            tidligereUtbetalinger = utbetalingDao.hentUtbetalinger(vedtak.sak.id),
            vedtak = vedtak,
        ).opprettUtbetaling()

        val foerstegangsbehandling = vedtak.behandling.type == BehandlingType.FORSTEGANGSBEHANDLING
        val oppdrag = oppdragMapper.oppdragFraUtbetaling(utbetaling, foerstegangsbehandling)

        logger.info("Sender oppdrag for sakId=${vedtak.sak.id} med vedtakId=${vedtak.vedtakId} til oppdrag")
        oppdragSender.sendOppdrag(oppdrag)
        return utbetalingDao.opprettUtbetaling(utbetaling.copy(oppdrag = oppdrag))
    }

    fun utbetalingEksisterer(vedtak: Utbetalingsvedtak) = utbetalingDao.hentUtbetaling(vedtak.vedtakId) != null

    fun eksisterendeUtbetalingslinjer(vedtak: Utbetalingsvedtak) =
        utbetalingDao.hentUtbetalingslinjer(vedtak.pensjonTilUtbetaling)

    fun eksisterendeData(vedtak: Utbetalingsvedtak): EksisterendeData {
        return if (utbetalingEksisterer(vedtak)) {
            EksisterendeData(Datatype.EKSISTERENDE_VEDTAKID)
        } else {
            val eksisterendeUtbetalingslinjer = eksisterendeUtbetalingslinjer(vedtak)
            if (eksisterendeUtbetalingslinjer.isNotEmpty()) {
                EksisterendeData(Datatype.EKSISTERENDE_UTBETALINGSLINJEID,
                    eksisterendeUtbetalingslinjer.map { it.id.value })
            } else {
                EksisterendeData(Datatype.INGEN_EKSISTERENDE_DATA)
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


    private fun utbetalingEvent(oppdrag: Oppdrag, status: UtbetalingStatus) = mapOf(
        "@event_name" to "utbetaling_oppdatert",
        "@vedtakId" to oppdrag.vedtakId(),
        "@status" to status.name,
        "@beskrivelse" to oppdrag.kvitteringBeskrivelse()
    ).toJson()

    private fun Oppdrag.kvitteringBeskrivelse() = when (this.mmel.kodeMelding) {
        "00" -> "Utbetaling OK"
        else -> "${this.mmel.kodeMelding} ${this.mmel.beskrMelding}"
    }

    companion object {
        private val logger = LoggerFactory.getLogger(UtbetalingService::class.java)
    }
}

data class EksisterendeData(
    val eksisterendeDatatype: Datatype, val data: List<Long>? = null
)

enum class Datatype() {
    EKSISTERENDE_UTBETALINGSLINJEID, EKSISTERENDE_VEDTAKID, INGEN_EKSISTERENDE_DATA
}
