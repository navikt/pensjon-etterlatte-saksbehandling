package no.nav.etterlatte.tidshendelser.regulering

import io.kotest.matchers.collections.containsInOrder
import no.nav.etterlatte.insert
import no.nav.etterlatte.tidshendelser.DatabaseExtension
import no.nav.etterlatte.tidshendelser.regulering.ReguleringDao.Databasetabell
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
        dataSource.insert(
            tabellnavn = Databasetabell.TABELLNAVN,
            params = { tx ->
                mapOf(
                    Databasetabell.ANTALL to 10,
                    Databasetabell.DATO to dato,
                    Databasetabell.SPESIFIKKE_SAKER to tx.createArrayOf("bigint", listOf(1, 2, 3)),
                    Databasetabell.EKSKLUDERTE_SAKER to tx.createArrayOf("bigint", listOf(2)),
                    Databasetabell.AKTIV to true,
                )
            },
        )
        val dao = ReguleringDao(datasource = dataSource)
        val konfigurasjon: Reguleringskonfigurasjon = dao.hentNyesteKonfigurasjon()
        assertEquals(10, konfigurasjon.antall)
        assertEquals(dato, konfigurasjon.dato)
        containsInOrder(listOf(1, 2, 3), konfigurasjon.spesifikkeSaker)
        containsInOrder(listOf(2), konfigurasjon.ekskluderteSaker)
    }
}
