package no.nav.etterlatte.common

import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.databaseContext
import java.sql.Connection
import javax.sql.DataSource

interface ConnectionAutoclosing {
    fun <T> hentConnection(block: (connection: Connection) -> T): T

    fun manglerKontekst(): Boolean
}

class ConnectionAutoclosingImpl(val dataSource: DataSource) : ConnectionAutoclosing {
    override fun <T> hentConnection(block: (connection: Connection) -> T): T {
        return if (manglerKontekst()) {
            dataSource.connection.use {
                block(it)
            }
        } else {
            block(databaseContext().activeTx())
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
