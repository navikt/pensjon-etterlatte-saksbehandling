package no.nav.etterlatte.common

import no.nav.etterlatte.Kontekst
import java.sql.Connection
import javax.sql.DataSource

class ConnectionAutoclosing(val dataSource: DataSource) {
    fun <T> hentConnection(block: (connection: Connection) -> T): T {
        return if (manglerKontekst()) {
            dataSource.connection.use {
                block(it)
            }
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
