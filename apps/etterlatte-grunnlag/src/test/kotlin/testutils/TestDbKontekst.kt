package testutils

import no.nav.etterlatte.DatabaseKontekst
import java.sql.Connection

object TestDbKontekst : DatabaseKontekst {
    override fun activeTx(): Connection {
        TODO("Not yet implemented")
    }
    override fun <T> inTransaction(block: () -> T): T {
        return block()
    }
}
