package no.nav.etterlatte.utbetaling.avstemming

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.toNorskTidspunkt
import no.nav.etterlatte.utbetaling.grensesnittavstemming.AvstemmingDao
import no.nav.etterlatte.utbetaling.grensesnittavstemming.avstemmingsdata.AvstemmingsdataSender
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.SakId
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.Saktype
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.UtbetalingDao
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.UtbetalingslinjeId
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.Utbetalingslinjetype
import no.nav.etterlatte.utbetaling.utbetaling
import no.nav.etterlatte.utbetaling.utbetalingslinje
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit

internal class KonsistensavstemmingServiceKtTest {

    private val avstemmingDao: AvstemmingDao = mockk()
    private val utbetalingDao: UtbetalingDao = mockk()
    private val avstemmingsdataSender: AvstemmingsdataSender = mockk()

    @Test
    fun `skal starte konsistensavstemming for Barnepensjon`() {
        val utbetaling = utbetaling()
        every {
            utbetalingDao.hentUtbetalingerForKonsistensavstemming(
                any(),
                any(),
                Saktype.BARNEPENSJON
            )
        } returns listOf(utbetaling)
        every { avstemmingDao.opprettKonsistensavstemming(any()) } returns 1

        every { avstemmingsdataSender.sendKonsistensavstemming(any()) } returns "<xml>data</xml>"

        val service = KonsistensavstemmingService(
            utbetalingDao = utbetalingDao,
            avstemmingDao = avstemmingDao,
            avstemmingsdataSender = avstemmingsdataSender
        )

        val dag = LocalDate.of(2022, 10, 10)
        val saktype = Saktype.BARNEPENSJON
        val resultat = service.startKonsistensavstemming(dag, saktype)

        assertTrue(resultat.all { it == "<xml>data</xml>" })
        verify { utbetalingDao.hentUtbetalingerForKonsistensavstemming(any(), any(), Saktype.BARNEPENSJON) }
        verify { avstemmingsdataSender.sendKonsistensavstemming(any()) }
    }

    @Test
    fun `konsistensavstemmingErKjoertIDag skal returnere false dersom det ikke finnes noen konsistensavstemminger`() {
        every {
            avstemmingDao.hentSisteKonsistensavsvemming(Saktype.BARNEPENSJON)
        } returns null

        val idag = LocalDate.now()
        val service = KonsistensavstemmingService(
            utbetalingDao = utbetalingDao,
            avstemmingDao = avstemmingDao,
            avstemmingsdataSender = avstemmingsdataSender
        )

        val resultat = service.konsistensavstemmingErKjoertIDag(Saktype.BARNEPENSJON, idag)
        assertFalse(resultat)
    }

    @Test
    fun `konsistensavstemmingErKjoertIDag skal returnere false dersom konsistensavstemming ikke er kjoert i dag`() {
        val idag = LocalDate.now()
        val igaar = Tidspunkt.now().minus(1, ChronoUnit.DAYS)
        every {
            avstemmingDao.hentSisteKonsistensavsvemming(Saktype.BARNEPENSJON)
        } returns mockk() {
            every { opprettet } returns igaar
        }

        val service = KonsistensavstemmingService(
            utbetalingDao = utbetalingDao,
            avstemmingDao = avstemmingDao,
            avstemmingsdataSender = avstemmingsdataSender
        )

        val resultat = service.konsistensavstemmingErKjoertIDag(Saktype.BARNEPENSJON, idag)
        assertFalse(resultat)
    }

    @Test
    fun `konsistensavstemmingErKjoertIDag skal returnere true dersom konsistensavstemming er kjoert i dag`() {
        val idagDato = LocalDate.now()
        val idagTidspunkt = Tidspunkt.now()
        every {
            avstemmingDao.hentSisteKonsistensavsvemming(Saktype.BARNEPENSJON)
        } returns mockk() {
            every { opprettet } returns idagTidspunkt
        }

        val service = KonsistensavstemmingService(
            utbetalingDao = utbetalingDao,
            avstemmingDao = avstemmingDao,
            avstemmingsdataSender = avstemmingsdataSender
        )

        val resultat = service.konsistensavstemmingErKjoertIDag(Saktype.BARNEPENSJON, idagDato)
        assertTrue(resultat)
    }

    @Test
    fun `lagKonsistensavstemming haandterer tilfellet uten utbetalingslinjer`() {
        val idag = LocalDate.now()
        every {
            utbetalingDao.hentUtbetalingerForKonsistensavstemming(
                idag.atStartOfDay().toNorskTidspunkt(),
                idag.minusDays(1).atTime(
                    LocalTime.MAX
                ).toNorskTidspunkt(),
                Saktype.BARNEPENSJON
            )
        } returns emptyList()

        val service = KonsistensavstemmingService(
            utbetalingDao = utbetalingDao,
            avstemmingDao = avstemmingDao,
            avstemmingsdataSender = avstemmingsdataSender
        )

        val konsistensavstemming = service.lagKonsistensavstemming(LocalDate.now(), Saktype.BARNEPENSJON)
        assertEquals(emptyList<OppdragForKonsistensavstemming>(), konsistensavstemming.loependeUtbetalinger)
    }

    @Test
    fun `lagKonsistensavstemming haandterer tilfellet uten loepende utbetalinger`() {
        val idag = LocalDate.now()
        every {
            utbetalingDao.hentUtbetalingerForKonsistensavstemming(
                idag.atStartOfDay().toNorskTidspunkt(),
                idag.minusDays(1).atTime(
                    LocalTime.MAX
                ).toNorskTidspunkt(),
                Saktype.BARNEPENSJON
            )
        } returns listOf(
            utbetaling(
                periodeFra = idag.minusDays(200),
                periodeTil = idag.minusDays(50)
            ),
            utbetaling(
                utbetalingslinjer = listOf(
                    utbetalingslinje(periodeFra = idag.minusDays(300)),
                    utbetalingslinje(type = Utbetalingslinjetype.OPPHOER, periodeFra = idag.minusDays(2))
                )
            )
        )

        val service = KonsistensavstemmingService(
            utbetalingDao = utbetalingDao,
            avstemmingDao = avstemmingDao,
            avstemmingsdataSender = avstemmingsdataSender
        )

        val konsistensavstemming = service.lagKonsistensavstemming(LocalDate.now(), Saktype.BARNEPENSJON)
        assertEquals(emptyList<OppdragForKonsistensavstemming>(), konsistensavstemming.loependeUtbetalinger)
    }

    /**
     * Utbetalingslinjene er sendt inn paa ulike tidspunkter.
     * 1:   |-------->
     * 2:       |---->
     * 3: |---------->
     */
    @Test
    fun `lagKonsistensavstemming tar med linjer som er relevante for en gitt dato`() {
        val linje1 = utbetalingslinje(
            utbetalingslinjeId = 1,
            periodeFra = LocalDate.of(1998, 1, 1),
            opprettet = LocalDate.of(1998, 1, 1).minusDays(16).toTidspunkt()
        )
        val linje2 = utbetalingslinje(
            utbetalingslinjeId = 2,
            erstatter = 1,
            periodeFra = LocalDate.of(1998, 6, 1),
            opprettet = LocalDate.of(1998, 2, 1).minusDays(16).toTidspunkt()

        )
        val linje3 = utbetalingslinje(
            utbetalingslinjeId = 3,
            erstatter = 2,
            periodeFra = LocalDate.of(1997, 11, 1),
            opprettet = LocalDate.of(1998, 9, 1).toTidspunkt()
        )

        val opprettet = LocalDateTime.of(1997, 12, 1, 0, 0).toNorskTidspunkt()

        val utbetaling1 = utbetaling(opprettet = opprettet, utbetalingslinjer = listOf(linje1))
        val utbetaling2 = utbetaling(opprettet = opprettet, utbetalingslinjer = listOf(linje2))
        val utbetaling3 = utbetaling(opprettet = opprettet, utbetalingslinjer = listOf(linje3))
        val utbetalingsliste = listOf(utbetaling1, utbetaling2, utbetaling3)

        every { utbetalingDao.hentUtbetalingerForKonsistensavstemming(any(), any(), any()) } returns utbetalingsliste

        val service = KonsistensavstemmingService(
            utbetalingDao = utbetalingDao,
            avstemmingDao = avstemmingDao,
            avstemmingsdataSender = avstemmingsdataSender
        )

        /* Case 1: Naa: linje 3 er eneste gjeldende */
        val linjerCase1 = service.lagKonsistensavstemming(
            dag = LocalDate.now(),
            saktype = Saktype.BARNEPENSJON
        ).loependeUtbetalinger.map { it.utbetalingslinjer }.flatten().map { it.id }
        assertEquals(listOf(linje3.id), linjerCase1)

        /* Case 2: Foer linje 3 er aktiv -> linje 1 og 2 er aktive */
        val linjerCase2 = service.lagKonsistensavstemming(
            dag = LocalDate.of(1998, 2, 25),
            saktype = Saktype.BARNEPENSJON
        ).loependeUtbetalinger.map { it.utbetalingslinjer }.flatten().map { it.id }
        assertEquals(listOf(linje1.id, linje2.id), linjerCase2)

        /* Case 3: Etter linje 2 har tatt over for linje 1, men foer linje 3 er aktiv -> Linje 2 er aktiv */
        val linjerCase3 = service.lagKonsistensavstemming(
            dag = LocalDate.of(1998, 7, 25),
            saktype = Saktype.BARNEPENSJON
        ).loependeUtbetalinger.map { it.utbetalingslinjer }.flatten().map { it.id }
        assertEquals(listOf(linje2.id), linjerCase3)

        /* Case 4: Foer linje 2 og 3 er aktive opprettet -> linje 1 er aktiv */
        val linjerCase4 = service.lagKonsistensavstemming(
            dag = LocalDate.of(1998, 1, 2),
            saktype = Saktype.BARNEPENSJON
        ).loependeUtbetalinger.map { it.utbetalingslinjer }.flatten().map { it.id }
        assertEquals(listOf(linje1.id), linjerCase4)
    }

    /**
     * Utbetalingslinjene er sendt inn paa ulike tidspunkter.
     * 1:   |-------->
     * 2:       |---->
     * 3:         | (OPPHOER)
     */
    @Test
    fun `lagKonsistensavstemming haandterer opphoer korrekt`() {
        val linje1 = utbetalingslinje(
            utbetalingslinjeId = 1,
            periodeFra = LocalDate.of(1998, 1, 1),
            opprettet = LocalDate.of(1998, 1, 1).minusDays(16).toTidspunkt()
        )
        val linje2 = utbetalingslinje(
            utbetalingslinjeId = 2,
            erstatter = 1,
            periodeFra = LocalDate.of(1998, 6, 1),
            opprettet = LocalDate.of(1998, 2, 1).minusDays(16).toTidspunkt()

        )
        val linje3 = utbetalingslinje(
            utbetalingslinjeId = 3,
            erstatter = 2,
            periodeFra = LocalDate.of(2000, 11, 1),
            opprettet = LocalDate.of(1998, 9, 1).toTidspunkt(),
            type = Utbetalingslinjetype.OPPHOER
        )

        val opprettet = LocalDateTime.of(1997, 12, 1, 0, 0).toNorskTidspunkt()

        val utbetaling1 = utbetaling(opprettet = opprettet, utbetalingslinjer = listOf(linje1))
        val utbetaling2 = utbetaling(opprettet = opprettet, utbetalingslinjer = listOf(linje2))
        val utbetaling3 = utbetaling(opprettet = opprettet, utbetalingslinjer = listOf(linje3))
        val utbetalingsliste = listOf(utbetaling1, utbetaling2, utbetaling3)

        every { utbetalingDao.hentUtbetalingerForKonsistensavstemming(any(), any(), any()) } returns utbetalingsliste

        val service = KonsistensavstemmingService(
            utbetalingDao = utbetalingDao,
            avstemmingDao = avstemmingDao,
            avstemmingsdataSender = avstemmingsdataSender
        )

        /* Case 1: Naa: linje 3 har foert til opphoer -> ingen aktive utbetalingslinjer */
        val linjerCase1 = service.lagKonsistensavstemming(
            dag = LocalDate.now(),
            saktype = Saktype.BARNEPENSJON
        ).loependeUtbetalinger.map { it.utbetalingslinjer }.flatten().map { it.id }
        assertEquals(emptyList<UtbetalingslinjeId>(), linjerCase1)

        /* Case 2: Foer linje 2 er aktiv, og foer linje 3 er opprettet -> linje 1 og 2 er aktive */
        val linjerCase2 = service.lagKonsistensavstemming(
            dag = LocalDate.of(1998, 2, 25),
            saktype = Saktype.BARNEPENSJON
        ).loependeUtbetalinger.map { it.utbetalingslinjer }.flatten().map { it.id }
        assertEquals(listOf(linje1.id, linje2.id), linjerCase2)

        /* Case 3: Etter linje 1 er aktiv, kun linje 2 er aktiv, foer linje 3 er opprettet -> linje 2 er aktiv */
        val linjerCase3 = service.lagKonsistensavstemming(
            dag = LocalDate.of(1998, 7, 25),
            saktype = Saktype.BARNEPENSJON
        ).loependeUtbetalinger.map { it.utbetalingslinjer }.flatten().map { it.id }
        assertEquals(listOf(linje2.id), linjerCase3)
    }

    /**
     * Utbetalingslinjene er sendt inn paa ulike tidspunkter, og hver linje er opprettet rett foer periodeFra
     * 1:  |--------->
     * 2:    | (OPPHOER)
     * 3:       ----->
     * 4:          | (OPPHOER)
     */
    @Test
    fun `lagKonsistensavstemming haandterer utbetaling etterfulgt av opphoer, med ny utbetaling etter opphoer`() {
        val opprettet1 = LocalDate.of(1998, 1, 1).minusDays(16).toTidspunkt()
        val linje1 = utbetalingslinje(
            utbetalingslinjeId = 1,
            periodeFra = opprettet1.toNorskLocalDate(),
            opprettet = opprettet1
        )
        val opprettet2 = LocalDate.of(1999, 12, 1).toTidspunkt()
        val linje2 = utbetalingslinje(
            utbetalingslinjeId = 2,
            erstatter = 1,
            periodeFra = opprettet2.toNorskLocalDate(),
            opprettet = opprettet2,
            type = Utbetalingslinjetype.OPPHOER
        )
        val opprettet3 = LocalDate.of(2001, 1, 1).minusDays(16).toTidspunkt()
        val linje3 = utbetalingslinje(
            utbetalingslinjeId = 3,
            erstatter = 2,
            periodeFra = opprettet3.toNorskLocalDate(),
            opprettet = opprettet3
        )
        val opprettet4 = LocalDate.of(2002, 1, 1).minusDays(15).toTidspunkt()
        val linje4 = utbetalingslinje(
            utbetalingslinjeId = 4,
            erstatter = 3,
            periodeFra = opprettet4.toNorskLocalDate(),
            opprettet = opprettet4,
            type = Utbetalingslinjetype.OPPHOER
        )

        val utbetaling1 = utbetaling(opprettet = opprettet1, utbetalingslinjer = listOf(linje1))
        val utbetaling2 = utbetaling(opprettet = opprettet2, utbetalingslinjer = listOf(linje2))
        val utbetaling3 = utbetaling(opprettet = opprettet3, utbetalingslinjer = listOf(linje3))
        val utbetaling4 = utbetaling(opprettet = opprettet4, utbetalingslinjer = listOf(linje4))
        val utbetalingsliste = listOf(utbetaling1, utbetaling2, utbetaling3, utbetaling4)

        every { utbetalingDao.hentUtbetalingerForKonsistensavstemming(any(), any(), any()) } returns utbetalingsliste

        val service = KonsistensavstemmingService(
            utbetalingDao = utbetalingDao,
            avstemmingDao = avstemmingDao,
            avstemmingsdataSender = avstemmingsdataSender
        )

        /* Case 1: kun linje 1 er opprettet og gjeldende -> kun linje 1 er aktiv */
        val linjerCase1 = service.lagKonsistensavstemming(
            dag = opprettet1.toNorskLocalDate().plusDays(1),
            saktype = Saktype.BARNEPENSJON
        ).loependeUtbetalinger.map { it.utbetalingslinjer }.flatten().map { it.id }
        assertEquals(listOf(linje1.id), linjerCase1)

        /* Case 2: opphoer i linje 2 er gjeldende -> ingen aktive linjer */
        val linjerCase2 = service.lagKonsistensavstemming(
            dag = opprettet2.toNorskLocalDate().plusDays(1),
            saktype = Saktype.BARNEPENSJON
        ).loependeUtbetalinger.map { it.utbetalingslinjer }.flatten().map { it.id }
        assertEquals(emptyList<UtbetalingslinjeId>(), linjerCase2)

        /* Case 3: linje 3 er opprettet og gjeldende -> kun linje 3 er aktiv */
        val linjerCase3 = service.lagKonsistensavstemming(
            dag = opprettet3.toNorskLocalDate().plusDays(1),
            saktype = Saktype.BARNEPENSJON
        ).loependeUtbetalinger.map { it.utbetalingslinjer }.flatten().map { it.id }
        assertEquals(listOf(linje3.id), linjerCase3)

        /* Case 4: opphoer i linje 4 er gjeldende -> ingen aktive linjer */
        val linjerCase4 = service.lagKonsistensavstemming(
            dag = opprettet4.toNorskLocalDate().plusDays(1),
            saktype = Saktype.BARNEPENSJON
        ).loependeUtbetalinger.map { it.utbetalingslinjer }.flatten().map { it.id }
        assertEquals(emptyList<UtbetalingslinjeId>(), linjerCase4)
    }

    /**
     * Utbetalingslinjene er sendt inn paa ulike tidspunkter, og hver linje er opprettet samtidig
     * 1:  |--------->
     * 2:    | (OPPHOER)
     * 3:       ----->
     * 4:          | (OPPHOER)
     */
    @Test
    fun `lagKonsistensavstemming haandterer aktive utbetalinger paa begge sider av opphoer`() {
        val opprettet = LocalDate.of(1998, 1, 1).minusDays(16).toTidspunkt()
        val periodeFra1 = LocalDate.of(1998, 1, 1).minusDays(16)
        val linje1 = utbetalingslinje(
            utbetalingslinjeId = 1,
            periodeFra = periodeFra1,
            opprettet = opprettet
        )
        val periodeFra2 = LocalDate.of(1999, 12, 1)
        val linje2 = utbetalingslinje(
            utbetalingslinjeId = 2,
            erstatter = 1,
            periodeFra = periodeFra2,
            opprettet = opprettet,
            type = Utbetalingslinjetype.OPPHOER
        )
        val periodeFra3 = LocalDate.of(2001, 1, 1).minusDays(16)
        val linje3 = utbetalingslinje(
            utbetalingslinjeId = 3,
            erstatter = 2,
            periodeFra = periodeFra3,
            opprettet = opprettet
        )
        val periodeFra4 = LocalDate.of(2002, 1, 1).minusDays(15)
        val linje4 = utbetalingslinje(
            utbetalingslinjeId = 4,
            erstatter = 3,
            periodeFra = periodeFra4,
            opprettet = opprettet,
            type = Utbetalingslinjetype.OPPHOER
        )

        val utbetaling1 = utbetaling(opprettet = opprettet, utbetalingslinjer = listOf(linje1))
        val utbetaling2 = utbetaling(opprettet = opprettet, utbetalingslinjer = listOf(linje2))
        val utbetaling3 = utbetaling(opprettet = opprettet, utbetalingslinjer = listOf(linje3))
        val utbetaling4 = utbetaling(opprettet = opprettet, utbetalingslinjer = listOf(linje4))
        val utbetalingsliste = listOf(utbetaling1, utbetaling2, utbetaling3, utbetaling4)

        every { utbetalingDao.hentUtbetalingerForKonsistensavstemming(any(), any(), any()) } returns utbetalingsliste

        val service = KonsistensavstemmingService(
            utbetalingDao = utbetalingDao,
            avstemmingDao = avstemmingDao,
            avstemmingsdataSender = avstemmingsdataSender
        )

        /* Case 1: linje 1 og 3 er opprettet og gjeldende -> kun linje 1 og 3 er aktive */
        val linjerCase1 = service.lagKonsistensavstemming(
            dag = periodeFra1.plusDays(1),
            saktype = Saktype.BARNEPENSJON
        ).loependeUtbetalinger.map { it.utbetalingslinjer }.flatten().map { it.id }
        assertEquals(listOf(linje1.id, linje3.id), linjerCase1)

        /* Case 2: opphoer i linje 2 er gjeldende -> kun linje 3 er aktiv */
        val linjerCase2 = service.lagKonsistensavstemming(
            dag = periodeFra2.plusDays(1),
            saktype = Saktype.BARNEPENSJON
        ).loependeUtbetalinger.map { it.utbetalingslinjer }.flatten().map { it.id }
        assertEquals(listOf(linje3.id), linjerCase2)

        /* Case 3: linje 3 er opprettet og gjeldende -> kun linje 3 er aktiv */
        val linjerCase3 = service.lagKonsistensavstemming(
            dag = periodeFra3.plusDays(1),
            saktype = Saktype.BARNEPENSJON
        ).loependeUtbetalinger.map { it.utbetalingslinjer }.flatten().map { it.id }
        assertEquals(listOf(linje3.id), linjerCase3)

        /* Case 4: opphoer i linje 4 er gjeldende -> ingen aktive linjer */
        val linjerCase4 = service.lagKonsistensavstemming(
            dag = periodeFra4.plusDays(1),
            saktype = Saktype.BARNEPENSJON
        ).loependeUtbetalinger.map { it.utbetalingslinjer }.flatten().map { it.id }
        assertEquals(emptyList<UtbetalingslinjeId>(), linjerCase4)
    }

    /**
     * Utbetaling etterfulgt av opphoer. Kontroll av at konsistensavstemming gir ingen aktive linjer paa opphoersdagen
     * 1:  |--------->
     * 2:      | (OPPHOER)
     */
    @Test
    fun `lagKonsistensavstemming haandterer overgangen til opphoer`() {
        val opprettet = LocalDate.of(1998, 1, 1).minusDays(16).toTidspunkt()
        val linje1 = utbetalingslinje(
            utbetalingslinjeId = 1,
            periodeFra = opprettet.toNorskLocalDate(),
            opprettet = opprettet
        )
        val opphoerFra = LocalDate.of(1999, 12, 1)
        val linje2 = utbetalingslinje(
            utbetalingslinjeId = 2,
            erstatter = 1,
            periodeFra = opphoerFra,
            opprettet = opprettet,
            type = Utbetalingslinjetype.OPPHOER
        )

        val utbetaling1 = utbetaling(opprettet = opprettet, utbetalingslinjer = listOf(linje1))
        val utbetaling2 = utbetaling(opprettet = opprettet, utbetalingslinjer = listOf(linje2))
        val utbetalingsliste = listOf(utbetaling1, utbetaling2)

        every { utbetalingDao.hentUtbetalingerForKonsistensavstemming(any(), any(), any()) } returns utbetalingsliste

        val service = KonsistensavstemmingService(
            utbetalingDao = utbetalingDao,
            avstemmingDao = avstemmingDao,
            avstemmingsdataSender = avstemmingsdataSender
        )

        /* Case 1: dagen foer opphoer av linje 1 -> linje 1 er aktiv */
        val linjerCase1 = service.lagKonsistensavstemming(
            dag = opphoerFra.minusDays(1),
            saktype = Saktype.BARNEPENSJON
        ).loependeUtbetalinger.map { it.utbetalingslinjer }.flatten().map { it.id }
        assertEquals(listOf(linje1.id), linjerCase1)
        /* Case 2: samme dag som linje 1 opphoerer -> ingen aktive linjer */
        val linjerCase2 = service.lagKonsistensavstemming(
            dag = opphoerFra,
            saktype = Saktype.BARNEPENSJON
        ).loependeUtbetalinger.map { it.utbetalingslinjer }.flatten().map { it.id }
        assertEquals(emptyList<UtbetalingslinjeId>(), linjerCase2)
    }

    /**
     * Flere saker med flere utbetalinger. Utbetalingene er opprettet rett foer periodeFra
     * Sak 1:
     * 1: |-------->
     * 2:      |--->
     *
     * Sak 2:
     * 1:  |------->
     * 2:      | (OPPHOER)
     */
    @Test
    fun `lagKonsistensavstemming haandterer flere utbetalinger med flere linjer`() {
        val opprettet1 = LocalDate.of(2020, 1, 1).minusDays(16).toTidspunkt()
        val linje1Sak1 = utbetalingslinje(
            utbetalingslinjeId = 1,
            periodeFra = opprettet1.toNorskLocalDate(),
            opprettet = opprettet1
        )
        val opprettet2 = LocalDate.of(2021, 12, 1).toTidspunkt()
        val linje2Sak1 = utbetalingslinje(
            utbetalingslinjeId = 2,
            erstatter = 1,
            periodeFra = opprettet2.toNorskLocalDate(),
            opprettet = opprettet2
        )
        val opprettet3 = LocalDate.of(2020, 6, 1).minusDays(16).toTidspunkt()
        val linje1Sak2 = utbetalingslinje(
            utbetalingslinjeId = 3,
            periodeFra = opprettet3.toNorskLocalDate(),
            opprettet = opprettet3
        )
        val linje2Sak2 = utbetalingslinje(
            utbetalingslinjeId = 4,
            erstatter = 3,
            periodeFra = opprettet2.toNorskLocalDate(),
            opprettet = opprettet2,
            type = Utbetalingslinjetype.OPPHOER
        )

        val sak1 = SakId(1L)
        val sak2 = SakId(2L)
        val utbetaling1 =
            utbetaling(sakId = sak1, opprettet = opprettet1, utbetalingslinjer = listOf(linje1Sak1))
        val utbetaling2 =
            utbetaling(sakId = sak1, opprettet = opprettet2, utbetalingslinjer = listOf(linje2Sak1))
        val utbetaling3 =
            utbetaling(sakId = sak2, opprettet = opprettet3, utbetalingslinjer = listOf(linje1Sak2))
        val utbetaling4 =
            utbetaling(sakId = sak2, opprettet = opprettet2, utbetalingslinjer = listOf(linje2Sak2))
        val utbetalingsliste = listOf(utbetaling1, utbetaling2, utbetaling3, utbetaling4)

        every { utbetalingDao.hentUtbetalingerForKonsistensavstemming(any(), any(), any()) } returns utbetalingsliste

        val service = KonsistensavstemmingService(
            utbetalingDao = utbetalingDao,
            avstemmingDao = avstemmingDao,
            avstemmingsdataSender = avstemmingsdataSender
        )

        /* Case 1: Foer noen utbetalinger er opprettet -> ingen aktive linjer */
        val linjerCase1 = service.lagKonsistensavstemming(
            dag = opprettet1.toNorskLocalDate().minusDays(1),
            saktype = Saktype.BARNEPENSJON
        ).loependeUtbetalinger.map { it.utbetalingslinjer }.flatten().map { it.id }
        assertEquals(emptyList<UtbetalingslinjeId>(), linjerCase1)

        /* Case 2: Kun linje1Sak1 er opprettet -> linje1Sak1 er aktiv */
        val linjerCase2 = service.lagKonsistensavstemming(
            dag = opprettet1.toNorskLocalDate().plusDays(1),
            saktype = Saktype.BARNEPENSJON
        ).loependeUtbetalinger.map { it.utbetalingslinjer }.flatten().map { it.id }
        assertEquals(listOf(linje1Sak1.id), linjerCase2)

        /* Case 3: linje1Sak1 og linje1Sak2 er opprettet -> linje1Sak1 og linje1Sak2 er aktive */
        val linjerCase3 = service.lagKonsistensavstemming(
            dag = opprettet3.toNorskLocalDate().plusDays(1),
            saktype = Saktype.BARNEPENSJON
        ).loependeUtbetalinger.map { it.utbetalingslinjer }.flatten().map { it.id }
        assertEquals(listOf(linje1Sak1.id, linje1Sak2.id), linjerCase3)

        /* Case 4: alle linjer opprettet, men linje1Sak2 er opphoert og linje1Sak1 erstattet -> linje2Sak1 er aktiv */
        val linjerCase4 = service.lagKonsistensavstemming(
            dag = opprettet2.toNorskLocalDate().plusDays(1),
            saktype = Saktype.BARNEPENSJON
        ).loependeUtbetalinger.map { it.utbetalingslinjer }.flatten().map { it.id }
        assertEquals(listOf(linje2Sak1.id), linjerCase4)
    }

    private fun LocalDate.toTidspunkt(): Tidspunkt = this.atStartOfDay().toNorskTidspunkt()
}