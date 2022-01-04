package no.nav.etterlatte.vilkaar.model

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.Year

internal class TidslinjeTest{

    val AAR_2019 = Year.of(2019)
    val AAR_2020 = Year.of(2020)
    val AAR_2021 = Year.of(2021)
    val AAR_2022 = Year.of(2022)

    @Test
    fun enkelTidslinje(){
        val tidslinje = Tidslinje(
            AAR_2019.let { it.atDay(1) to it.value},
            AAR_2020.let { it.atDay(1) to it.value},
            AAR_2021.let { it.atDay(1) to it.value},
            AAR_2022.let { it.atDay(1) to it.value},

        )

        assertEquals(2019, tidslinje[LocalDate.of(2019, 5, 5)])
        assertEquals(2020, tidslinje[LocalDate.of(2020, 1, 1)])
        assertEquals(2021, tidslinje[LocalDate.of(2021, 12, 31)])
        assertEquals(2022, tidslinje[LocalDate.of(2025, 5, 5)])

    }

    @Test
    fun tidslinerKanZippes(){
        val tidslinjeAarstall = Tidslinje(
            AAR_2019.let { it.atDay(1) to it.value},
            AAR_2020.let { it.atDay(1) to it.value},
            AAR_2021.let { it.atDay(1) to it.value},
            AAR_2022.let { it.atDay(1) to it.value},
            )
        val tidslinjeSkuddaar = Tidslinje(
            AAR_2019.let { it.atDay(1) to it.isLeap},
            AAR_2020.let { it.atDay(1) to it.isLeap},
            AAR_2021.let { it.atDay(1) to it.isLeap},
            AAR_2022.let { it.atDay(1) to it.isLeap},
        )

        val zippet = tidslinjeAarstall + tidslinjeSkuddaar

        assertEquals(2019, zippet[LocalDate.of(2019, 5, 5)]?.first)
        assertEquals(false, zippet[LocalDate.of(2019, 5, 5)]?.second)
        assertEquals(2020, zippet[LocalDate.of(2020, 1, 1)]?.first)
        assertEquals(true, zippet[LocalDate.of(2020, 1, 1)]?.second)
        assertEquals(2021, zippet[LocalDate.of(2021, 12, 31)]?.first)
        assertEquals(false, zippet[LocalDate.of(2021, 12, 31)]?.second)
        assertEquals(2022, zippet[LocalDate.of(2025, 5, 5)]?.first)
        assertEquals(false, zippet[LocalDate.of(2025, 5, 5)]?.second)

    }

    @Test
    fun normaliseringFjernerLikePerioderYtenAaMisteInformasjon(){
        val tidslinjeSkuddaar =(1990..2030)
            .map { Year.of(it) }
            .let { Tidslinje(*it.map { it.atDay(1) to it.isLeap }.toTypedArray()) }

        val normalisert = tidslinjeSkuddaar.normaliser()

        assertEquals(41, tidslinjeSkuddaar.size())
        assertEquals(21, normalisert.size())

        (1990..2030).forEach {
            val year = Year.of(it)
            if(year.isLeap) assertTrue(normalisert[year.atDay(1)]!!)
            else assertFalse(normalisert[year.atDay(1)]!!)

        }

    }
}