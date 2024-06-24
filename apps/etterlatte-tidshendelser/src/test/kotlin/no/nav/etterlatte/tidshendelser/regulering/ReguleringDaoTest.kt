package no.nav.etterlatte.tidshendelser.regulering

import io.kotest.matchers.collections.containsInOrder
import no.nav.etterlatte.insert
import no.nav.etterlatte.tidshendelser.DatabaseExtension
import no.nav.etterlatte.tidshendelser.regulering.ReguleringDao.Databasetabell
import no.nav.etterlatte.tilDatabasetabell
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate
import java.time.Month
import javax.sql.DataSource

@ExtendWith(DatabaseExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ReguleringDaoTest(
    private val dataSource: DataSource,
) {
    @Test
    fun lagreOgHentOppReguleringskonfigurasjon() {
        val dato = LocalDate.of(2024, Month.JUNE, 22)
        leggInnReguleringskonfigurasjon(dato, 10, listOf(1L, 2, 3), listOf(2))
        leggInnReguleringskonfigurasjon(dato, 20, listOf(1L, 2, 3, 4, 5), listOf(1L, 2, 4))
        val dao = ReguleringDao(datasource = dataSource)
        val konfigurasjon: Reguleringskonfigurasjon = dao.hentNyesteKonfigurasjon()
        assertEquals(20, konfigurasjon.antall)
        assertEquals(dato, konfigurasjon.dato)
        containsInOrder(listOf(1, 2, 3, 4, 5), konfigurasjon.spesifikkeSaker)
        containsInOrder(listOf(1, 2, 4), konfigurasjon.ekskluderteSaker)
    }

    private fun leggInnReguleringskonfigurasjon(
        dato: LocalDate,
        antall: Int,
        spesifikkeSaker: List<Long>,
        ekskluderteSaker: List<Long>,
    ) = dataSource.insert(
        tabellnavn = Databasetabell.TABELLNAVN,
        params = { tx ->
            mapOf(
                Databasetabell.ANTALL to antall,
                Databasetabell.DATO to dato,
                Databasetabell.SPESIFIKKE_SAKER to spesifikkeSaker.tilDatabasetabell(tx),
                Databasetabell.EKSKLUDERTE_SAKER to ekskluderteSaker.tilDatabasetabell(tx),
                Databasetabell.AKTIV to true,
            )
        },
    )
}
