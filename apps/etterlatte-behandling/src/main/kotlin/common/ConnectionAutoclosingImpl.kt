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

    abstract fun <T> hentKotliquerySession(block: (session: Session) -> T): T
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

    override fun <T> hentKotliquerySession(block: (session: Session) -> T): T =
        hentConnection { connection: Connection ->
            block(Session(kotliquery.Connection(connection)))
        }
}
