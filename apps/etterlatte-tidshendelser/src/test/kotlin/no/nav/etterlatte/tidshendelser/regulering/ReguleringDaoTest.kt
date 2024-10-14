package no.nav.etterlatte.tidshendelser.regulering

import io.kotest.matchers.collections.containsInOrder
import kotliquery.TransactionalSession
import no.nav.etterlatte.behandling.sakId1
import no.nav.etterlatte.behandling.sakId2
import no.nav.etterlatte.behandling.sakId3
import no.nav.etterlatte.insert
import no.nav.etterlatte.libs.common.sak.SakId
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
        leggInnReguleringskonfigurasjon(dato, 10, listOf(sakId1, sakId2, sakId3), listOf(sakId2))
        leggInnReguleringskonfigurasjon(
            dato,
            20,
            listOf(sakId1, sakId2, sakId3, SakId(4), SakId(5)),
            listOf(sakId1, sakId2, SakId(4)),
        )
        val dao = ReguleringDao(datasource = dataSource)
        val konfigurasjon: Reguleringskonfigurasjon = dao.hentNyesteKonfigurasjon()
        assertEquals(20, konfigurasjon.antall)
        assertEquals(dato, konfigurasjon.dato)
        containsInOrder(listOf(1, 2, 3, 4, 5), konfigurasjon.spesifikkeSaker)
        containsInOrder(listOf(1, 2, 4), konfigurasjon.ekskluderteSaker)
    }

    @Test
    fun taklerAtViIkkeHarSpesifisertSaker() {
        val dato = LocalDate.of(2024, Month.JUNE, 22)
        leggInnReguleringskonfigurasjon(dato, 10, null, null)
        leggInnReguleringskonfigurasjon(dato, 20, null, null)
        val dao = ReguleringDao(datasource = dataSource)
        val konfigurasjon: Reguleringskonfigurasjon = dao.hentNyesteKonfigurasjon()
        assertEquals(20, konfigurasjon.antall)
        assertEquals(dato, konfigurasjon.dato)
        containsInOrder(listOf(1, 2, 3, 4, 5), konfigurasjon.spesifikkeSaker)
        containsInOrder(listOf(1, 2, 4), konfigurasjon.ekskluderteSaker)
    }

    @Test
    fun ignorerInaktivKonfigurasjon() {
        val dato = LocalDate.of(2024, Month.JUNE, 22)
        leggInnReguleringskonfigurasjon(dato, 10, listOf(), listOf())
        leggInnReguleringskonfigurasjon(dato, 20, listOf(), listOf(), false)
        val dao = ReguleringDao(datasource = dataSource)
        val konfigurasjon: Reguleringskonfigurasjon = dao.hentNyesteKonfigurasjon()
        assertEquals(10, konfigurasjon.antall)
        assertEquals(dato, konfigurasjon.dato)
    }

    private fun leggInnReguleringskonfigurasjon(
        dato: LocalDate,
        antall: Int,
        spesifikkeSaker: List<SakId>?,
        ekskluderteSaker: List<SakId>?,
        aktiv: Boolean = true,
    ) = dataSource.insert(
        tabellnavn = Databasetabell.TABELLNAVN,
        params = { tx ->
            mapOf(
                Databasetabell.ANTALL to antall,
                Databasetabell.DATO to dato,
                Databasetabell.SPESIFIKKE_SAKER to spesifikkeSaker?.tilDatabasetabell(tx),
                Databasetabell.EKSKLUDERTE_SAKER to ekskluderteSaker?.tilDatabasetabell(tx),
                Databasetabell.AKTIV to aktiv,
            )
        },
    )
}

fun List<SakId>.tilDatabasetabell(tx: TransactionalSession) = tx.createArrayOf("bigint", this.map { it })
