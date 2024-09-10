package no.nav.etterlatte.common

import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.databaseContext
import no.nav.etterlatte.libs.database.ConnectionAutoclosing
import java.sql.Connection
import javax.sql.DataSource

abstract class ConnectionAutoclosingBehandling : ConnectionAutoclosing {
    internal fun manglerKontekst() =
        when (Kontekst.get()) {
            null -> true
            else -> false
        }
}

class ConnectionAutoclosingImpl(
    val dataSource: DataSource,
) : ConnectionAutoclosingBehandling() {
    override fun <T> hentConnection(block: (connection: Connection) -> T): T =
        if (manglerKontekst()) {
            dataSource.connection.use {
                block(it)
            }
        } else {
            block(databaseContext().activeTx())
        }
}
