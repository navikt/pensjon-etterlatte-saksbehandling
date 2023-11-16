package no.nav.etterlatte.migrering

import kotliquery.TransactionalSession
import no.nav.etterlatte.libs.database.Transactions
import no.nav.etterlatte.libs.database.hentListe
import no.nav.etterlatte.libs.database.oppdater
import no.nav.etterlatte.libs.database.transaction
import javax.sql.DataSource

class StartMigrering(val repository: StartMigreringRepository) {
    fun startMigrering() {
        val sakerTilMigrering = repository.hentSakerTilMigrering()
        repository.settSakerMigrert(sakerTilMigrering)
    }
}

class StartMigreringRepository(private val dataSource: DataSource) : Transactions<StartMigreringRepository> {
    override fun <R> inTransaction(block: StartMigreringRepository.(TransactionalSession) -> R): R =
        dataSource.transaction {
            this.block(it)
        }

    fun hentSakerTilMigrering(tx: TransactionalSession? = null) =
        tx.session {
            hentListe(
                queryString =
                    "SELECT ${Databasetabell.SAKID} FROM ${Databasetabell.TABELLNAVN} " +
                        "WHERE ${Databasetabell.HAANDTERT} = FALSE",
            ) {
                it.long(Databasetabell.SAKID)
            }
        }

    fun settSakerMigrert(
        saker: List<Long>,
        tx: TransactionalSession? = null,
    ) = tx.session {
        oppdater(
            query =
                "UPDATE ${Databasetabell.TABELLNAVN} SET ${Databasetabell.HAANDTERT} = true WHERE " +
                    "${Databasetabell.SAKID} = ANY(:saker)",
            params =
                mapOf(
                    "saker" to createArrayOf("bigint", saker),
                ),
            loggtekst = "Markerte $saker som påstarta for migrering",
        )
    }

    internal object Databasetabell {
        const val TABELLNAVN = "saker_til_migrering"
        const val HAANDTERT = "haandtert"
        const val SAKID = "sakId"
    }
}
