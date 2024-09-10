package no.nav.etterlatte.libs.database

import java.sql.Connection

interface ConnectionAutoclosing {
    fun <T> hentConnection(block: (connection: Connection) -> T): T
}
