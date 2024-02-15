package no.nav.etterlatte.common

import no.nav.etterlatte.Kontekst
import java.sql.Connection
import javax.sql.DataSource

class ConnectionAutoclosing(val dataSource: DataSource) {
    fun <T> hentConnection(block: (connection: Connection) -> T): T {
        return if (manglerKontekst()) {
            val connection = dataSource.connection
            block(connection).also { connection.close() }
        } else {
            block(Kontekst.get().databasecontxt.activeTx())
        }
    }

    fun manglerKontekst(): Boolean {
        val kontekst = Kontekst.get()
        return when (kontekst) {
            null -> true
            else -> false
        }
    }
}
