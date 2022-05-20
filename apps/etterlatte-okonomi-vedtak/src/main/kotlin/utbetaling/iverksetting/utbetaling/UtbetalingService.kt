package no.nav.etterlatte.utbetaling.iverksetting.utbetaling

import no.nav.etterlatte.domene.vedtak.Vedtak
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.utbetaling.common.Tidspunkt
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
    fun iverksettUtbetaling(vedtak: Vedtak): Utbetaling {

        val tidligereUtbetalingerForSak = utbetalingDao.hentUtbetalinger(vedtak.sak.id)
        // hva gjøres dersom sakId ikke finnes i databasen, men vedtaket er IKKE et førstegangsvedtak?

        // må uansett her håndtere én av tre situasjoner:
        // 1. Førstegangsinnvilgelse
        // 2. Rent opphør
        // 3. Endringer (flere linjer og muligens opphør)

        if (tidligereUtbetalingerForSak.isEmpty()) {
            forstegangsinnvilgelse(vedtak)
        }

        /* Håndtere case:
        / 1. Førstegangsinnvilgelse (1 utbetalingslinje)
        / 2. Førstegangsinnvilgelse (2 eller flere utbetalingslinjer)
        / 3. Opphør av sak (1 utbetalingslinje)
        / 4. Opphør av sak (2 eller flere utbetalingslinjer)
        / 5. Førstegangsinnvilgelse og opphør i samme vedtak (minst 2 utbetalingslinjer)


         For revurdering / opphør: må finne utbetalingslinjer som gjelder per nå.
         Eksempel på utbetalingslinjer:
             1       2       3       4
         |-------|-------|-------|------------------------->
               5    6       7               8
              |--|-------|-------|------------------------->
                9     10          11
              |--|-----------|----------->
             9
         |-------|
           10  11
         |----|--|

         5: ENDR?
         6,7,8: NY?


         Historikk-messig, også behov for å se hvilke utbetalinger fom faktisk har skjedd?
         F.eks en revurdering tilbake i tid:
                   8                    9
         |------------------|------------------------>
         - I tilfelle med kun utbetalingslinjer 1,2,3 og 4, så nå en revurdering med 8 og 9 ->
            - er 8 UEND eller NY?
            - er 9 UEND eller NY?


         Hva skal settes som erstatterId i Utbetalingslinje?
         - 2 for linje 5? eller kanskje 2, 3 og 4?
         - 4 for linje 6 og linje 7?
         Må iallefall ha i modellen/databasen at en linje erstattes av en annen for å ha et konsept
         om hvilke utbetalingslinjer som gjelder nå og ikke.

         Behov for å finne gjeldene utbetaling for et gitt tidsrom:

         1. Henter ut eksisterende utbetalingslinjer for en sak
         2. Må finne tidligste nye utbetalingslinje, og finne ut hvilken linje denne erstatter (sorter perioder)
            - trenger referansen til eksisterende utbetalingslinje som blir erstattet av den tidligste nye utbetalingslinjen
         3. Hvis det er opphør: send opphørsmelding
         4. Hvis det er en endring: send de nye meldingene inn som "ENDR" eller "NY"
            - Eventuelt kun den første av de nye meldingene som sendes inn som "ENDR"?

         */


        val utbetaling: Utbetaling = toUtbetaling(vedtak, tidligereUtbetalingerForSak)
        val opprettetTidspunkt = Tidspunkt.now(clock)
        val oppdrag = oppdragMapper.oppdragFraVedtak(vedtak, opprettetTidspunkt)

        logger.info("Sender oppdrag for sakId=${vedtak.sak.id} med vedtakId=${vedtak.vedtakId} til oppdrag")
        oppdragSender.sendOppdrag(oppdrag)
        return utbetalingDao.opprettUtbetaling(utbetaling.copy(oppdrag = oppdrag))
    }


    fun forstegangsinnvilgelse(vedtak: Vedtak) {
        val opprettetTidspunkt = Tidspunkt.now(clock)
        val nyUtbetaling: Utbetaling = toNyUtbetaling(vedtak)
        val oppdrag = oppdragMapper.oppdragFraUtbetaling(nyUtbetaling, true)

        logger.info("Sender oppdrag for sakId=${vedtak.sak.id} med vedtakId=${vedtak.vedtakId} til oppdrag")
        oppdragSender.sendOppdrag(oppdrag)

        // return utbetalingDao.opprettUtbetaling(nyUtbetaling.copy(oppdrag = oppdrag))
    }

    private fun toNyUtbetaling(vedtak: Vedtak): Utbetaling {
        //return Utbetaling()
    }


    private fun toUtbetaling(vedtak: Vedtak, utbetalinger: List<Utbetaling>): Utbetaling {
        val utbetalingslinjer = utbetalinger.flatMap { it.utbetalingslinjer }


        val opprettetTidspunkt = Tidspunkt.now(clock)
        // opprett utbetaling her
        return utbetalinger.last() // TODO

    }

    fun utbetalingEksisterer(vedtak: Vedtak) =
        utbetalingDao.hentUtbetaling(vedtak.vedtakId) != null

    fun oppdaterKvittering(oppdrag: Oppdrag): Utbetaling {
        logger.info("Oppdaterer kvittering for oppdrag med id=${oppdrag.vedtakId()}")
        return utbetalingDao.oppdaterKvittering(oppdrag, Tidspunkt.now(clock))
    }

    // TODO: sette inn kolonne i database som viser at kvittering er oppdatert manuelt?
    fun settKvitteringManuelt(vedtakId: String) = utbetalingDao.hentUtbetaling(vedtakId)?.let {
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