package no.nav.etterlatte.common

import kotliquery.Session
import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.databaseContext
import java.sql.Connection
import javax.sql.DataSource

abstract class ConnectionAutoclosing {
    abstract fun <T> hentConnection(block: (connection: Connection) -> T): T

    internal fun manglerKontekst(): Boolean {
        val kontekst = Kontekst.get()
        return when (kontekst) {
            null -> true
            else -> false
        }
    }

    abstract fun hentKotliquerySession(): Session
}

class ConnectionAutoclosingImpl(
    val dataSource: DataSource,
) : ConnectionAutoclosing() {
    override fun <T> hentConnection(block: (connection: Connection) -> T): T =
        if (manglerKontekst()) {
            dataSource.connection.use {
                block(it)
            }
        } else {
            block(databaseContext().activeTx())
        }

    override fun hentKotliquerySession(): Session =
        hentConnection { connection: Connection ->
            Session(kotliquery.Connection(connection))
        }
}
