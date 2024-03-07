package no.nav.etterlatte.migrering.verge

import kotliquery.TransactionalSession
import no.nav.etterlatte.libs.database.Transactions
import no.nav.etterlatte.libs.database.transaction
import javax.sql.DataSource

internal class VergeRepository(private val dataSource: DataSource) : Transactions<VergeRepository> {
    override fun <R> inTransaction(block: VergeRepository.(TransactionalSession) -> R): R =
        dataSource.transaction {
            this.block(it)
        }
}
