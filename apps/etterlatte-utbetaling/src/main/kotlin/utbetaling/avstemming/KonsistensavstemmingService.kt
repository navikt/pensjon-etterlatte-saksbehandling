package no.nav.etterlatte.utbetaling.avstemming

import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.norskTidssone
import no.nav.etterlatte.libs.common.tidspunkt.toTidspunkt
import no.nav.etterlatte.utbetaling.grensesnittavstemming.AvstemmingDao
import no.nav.etterlatte.utbetaling.grensesnittavstemming.UUIDBase64
import no.nav.etterlatte.utbetaling.grensesnittavstemming.avstemmingsdata.AvstemmingsdataSender
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.Foedselsnummer
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.NavIdent
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.Saktype
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.UtbetalingDao
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.Utbetalingslinje
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.UtbetalingslinjeId
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.Utbetalingslinjetype
import org.slf4j.LoggerFactory
import utbetaling.avstemming.avstemmingsdata.KonsistensavstemmingDataMapper
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime

class KonsistensavstemmingService(
    private val utbetalingDao: UtbetalingDao,
    private val avstemmingDao: AvstemmingDao,
    private val avstemmingsdataSender: AvstemmingsdataSender
) {

    private val logger = LoggerFactory.getLogger(this::class.java)

    fun startKonsistensavstemming(
        dag: LocalDate,
        saktype: Saktype
    ): List<String> {
        logger.info("Starter konsistensavstemming for ${saktype.name} paa dato $dag")
        val konsistensavstemming = lagKonsistensavstemming(dag, saktype)
        val mappetKonsistensavstemming =
            KonsistensavstemmingDataMapper(konsistensavstemming).opprettAvstemmingsmelding()
        val sendtAvstemmingsdata = mappetKonsistensavstemming.mapIndexed { meldingNr, melding ->
            val xmlMelding = avstemmingsdataSender.sendKonsistensavstemming(melding)
            logger.info(
                "Har sendt konsistensavstemmingsmelding ${meldingNr + 1} av ${mappetKonsistensavstemming.size}" +
                    " for konsistensavstemming ${konsistensavstemming.id.value}"
            )
            xmlMelding
        }

        avstemmingDao.opprettKonsistensavstemming(
            konsistensavstemming.copy(
                avstemmingsdata = sendtAvstemmingsdata.joinToString("\n")
            )
        )

        logger.info("Konsistensavstemming for ${saktype.name} paa dato $dag utfoert")
        return sendtAvstemmingsdata
    }

    fun lagKonsistensavstemming(dag: LocalDate, saktype: Saktype): Konsistensavstemming {
        val loependeYtelseFom = dag.atStartOfDay().toTidspunkt(norskTidssone)
        val registrertFoerTom = dag.minusDays(1).atTime(LocalTime.MAX).toTidspunkt(norskTidssone)

        val relevanteUtbetalinger = utbetalingDao.hentUtbetalingerForKonsistensavstemming(
            aktivFraOgMed = loependeYtelseFom,
            opprettetFramTilOgMed = registrertFoerTom,
            saktype = saktype
        )

        /**
         * 1. Ta bort de utbetalingene som er opprettet etter konsistensavstemmingstidspunktet
         * 2. Grupper utbetalinger per sak
         * 3. Samle en liste av utbetalinger til en samlet utbetaling med mange utbetalingslinjer (ligner på struktur vi
         *      sender i konsistensavstemmingen)
         * 4. Filtrerer bort alle oppdragslinjer som ikke er "aktive" i OS på konsistensavstemmingsdatoen
         * 5. Mapper de sakene med oppdragslinjer igjen om til OppdragForKonsistensavstemming, og luker bort de sakene
         *      uten noen oppdragslinjer.
         *
         * Bada bing bada boom
         *
         * Inspirasjon og takk rettes mot su-se-bakover's implementasjon av tilsvarende
         */
        val loependeUtbetalinger: List<OppdragForKonsistensavstemming> = relevanteUtbetalinger
            .filter { it.opprettet <= registrertFoerTom.instant } // 1
            .groupBy { it.sakId } // 2
            .mapValues { entry -> // 3
                entry.value.map { utbetaling ->
                    UtbetalingslinjerPerSak(
                        sakId = 0,
                        saktype = saktype,
                        fnr = utbetaling.stoenadsmottaker.value,
                        utbetalingslinjer = utbetaling.utbetalingslinjer,
                        utbetalingslinjerTilAttestanter = utbetaling.utbetalingslinjer.map { it.id }
                            .associateWith { listOf(utbetaling.attestant) }
                    )
                }.reduce { acc, next ->
                    acc.copy(
                        utbetalingslinjer = acc.utbetalingslinjer + next.utbetalingslinjer,
                        utbetalingslinjerTilAttestanter = acc.utbetalingslinjerTilAttestanter +
                            next.utbetalingslinjerTilAttestanter
                    )
                }
            } // 3
            .mapValues { entry -> // 4
                val gjeldendeForKonsistensavstemming = gjeldendeLinjerForEnDato(entry.value.utbetalingslinjer, dag)
                entry.value.copy(
                    utbetalingslinjer = gjeldendeForKonsistensavstemming
                )
            } // 4
            .mapNotNull { (sakid, utbetalingslinjerPerSak) -> // 5
                when (utbetalingslinjerPerSak.utbetalingslinjer.size) {
                    0 -> null
                    else -> OppdragForKonsistensavstemming(
                        sakId = sakid,
                        sakType = saktype,
                        fnr = Foedselsnummer(utbetalingslinjerPerSak.fnr),
                        utbetalingslinjer = utbetalingslinjerPerSak.utbetalingslinjer.map {
                            OppdragslinjeForKonsistensavstemming(
                                id = it.id,
                                opprettet = it.opprettet,
                                fraOgMed = it.periode.fra,
                                tilOgMed = it.periode.til,
                                forrigeUtbetalingslinjeId = it.erstatterId,
                                beloep = it.beloep,
                                attestanter = utbetalingslinjerPerSak
                                    .utbetalingslinjerTilAttestanter[it.id]
                                    ?: emptyList()
                            )
                        }
                    )
                }
            } // 5

        return Konsistensavstemming(
            id = UUIDBase64(),
            sakType = saktype,
            opprettet = Tidspunkt(instant = Instant.now()),
            avstemmingsdata = null,
            loependeFraOgMed = loependeYtelseFom,
            opprettetTilOgMed = registrertFoerTom,
            loependeUtbetalinger = loependeUtbetalinger
        )
    }
}

/**
 * Tar kun med de gjeldende linjene for en dato:
 * En linje er gjeldende hvis den
 *      1. Er opprettet før / på datoen
 *      2. Ikke har en utløpsdato før datoen
 *      3. Ikke har blitt erstattet med en linje fra med før datoen (gjelder erstatteres erstattere også)
 *      4. Ikke er en opphørslinje (disse telles ikke som aktive)
 */
fun gjeldendeLinjerForEnDato(utbetalingslinjer: List<Utbetalingslinje>, dato: LocalDate): List<Utbetalingslinje> {
    val trivieltGjeldende = utbetalingslinjer
        .filter {
            it.opprettet.instant <= ZonedDateTime.of(dato, LocalTime.MIN, norskTidssone).toInstant()
        } // 1
        .filter { (it.periode.til ?: dato) >= dato } // 2

    val erstattetAvMap = trivieltGjeldende.associateBy { it.erstatterId }

    return trivieltGjeldende.filter { utbetalingslinje -> // 3
        var tidligsteStartForEnErstatter: LocalDate? = null
        var naavaerendeLinje = erstattetAvMap[utbetalingslinje.id]

        while (naavaerendeLinje != null) {
            tidligsteStartForEnErstatter =
                listOfNotNull(tidligsteStartForEnErstatter, naavaerendeLinje.periode.fra).min()
            naavaerendeLinje = erstattetAvMap[naavaerendeLinje.id]
        }

        (tidligsteStartForEnErstatter ?: dato) >= dato
    }.filter { it.type != Utbetalingslinjetype.OPPHOER } // 4
}

data class UtbetalingslinjerPerSak(
    val sakId: Long,
    val saktype: Saktype,
    val fnr: String,
    val utbetalingslinjer: List<Utbetalingslinje>,
    val utbetalingslinjerTilAttestanter: Map<UtbetalingslinjeId, List<NavIdent>>
)