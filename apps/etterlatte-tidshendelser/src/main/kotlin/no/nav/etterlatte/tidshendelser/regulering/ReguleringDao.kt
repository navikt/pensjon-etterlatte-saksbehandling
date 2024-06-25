package no.nav.etterlatte.tidshendelser.regulering

import kotliquery.TransactionalSession
import no.nav.etterlatte.libs.database.Transactions
import no.nav.etterlatte.libs.database.hent
import no.nav.etterlatte.libs.database.transaction
import javax.sql.DataSource

class ReguleringDao(
    private val datasource: DataSource,
) : Transactions<ReguleringDao> {
    override fun <R> inTransaction(block: ReguleringDao.(TransactionalSession) -> R): R =
        datasource.transaction {
            this.block(it)
        }

    fun hentNyesteKonfigurasjon(): Reguleringskonfigurasjon =
        datasource.transaction { tx ->
            with(Databasetabell) {
                tx.hent(
                    "SELECT $ANTALL, $DATO, $SPESIFIKKE_SAKER, $EKSKLUDERTE_SAKER FROM $TABELLNAVN " +
                        "WHERE $AKTIV=true ORDER BY $OPPRETTET DESC LIMIT 1",
                ) { row ->
                    Reguleringskonfigurasjon(
                        antall = row.int(ANTALL),
                        dato = row.localDate(DATO),
                        spesifikkeSaker = row.array<Long>(SPESIFIKKE_SAKER).toList(),
                        ekskluderteSaker = row.array<Long>(EKSKLUDERTE_SAKER).toList(),
                    )
                }
            } ?: throw IllegalStateException("Kan ikke kjøre regulering uten å ha gyldig reguleringskonfigurasjon")
        }

    internal object Databasetabell {
        const val TABELLNAVN = "reguleringskonfigurasjon"
        const val ANTALL = "antall"
        const val DATO = "dato"
        const val SPESIFIKKE_SAKER = "spesifikke_saker"
        const val EKSKLUDERTE_SAKER = "ekskluderte_saker"
        const val AKTIV = "aktiv"
        const val OPPRETTET = "opprettet"
    }
}
