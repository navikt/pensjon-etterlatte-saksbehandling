package no.nav.etterlatte

import no.nav.etterlatte.common.ConnectionAutoclosing
import java.sql.Connection
import javax.sql.DataSource

/*
    Denne gjør at vi slipper å wrapper alle db kalle med instransaction i tester
    som kaller db metoder der man ikke egentlig vil bruke intransaction
    Har vi flyter som kun bruker daoer som en vanlig sb flyt så bruker man DatabaseContext.kt i testen sin Kontekst.set
 */

class DatabaseContextTest(private val ds: DataSource) : DatabaseKontekst {
    override fun activeTx(): Connection = ds.connection

    override fun <T> inTransaction(block: () -> T): T {
        // NOOPP
        return block()
    }
}

class ConnectionAutoclosingTest(val dataSource: DataSource) : ConnectionAutoclosing {
    override fun <T> hentConnection(block: (connection: Connection) -> T): T {
        return if (manglerKontekst()) {
            dataSource.connection.use {
                block(it)
            }
        } else {
            val activeTx = databaseContext().activeTx()
            block(activeTx).also { activeTx.close() } // TODO: trådsikker?
        }
    }

    override fun manglerKontekst(): Boolean {
        val kontekst = Kontekst.get()
        return when (kontekst) {
            null -> true
            else -> false
        }
    }
}
