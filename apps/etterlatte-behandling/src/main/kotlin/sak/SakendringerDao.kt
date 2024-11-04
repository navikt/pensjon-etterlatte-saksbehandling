package no.nav.etterlatte.sak

import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.common.ConnectionAutoclosing
import no.nav.etterlatte.libs.common.dbutils.setTidspunkt
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.database.setJsonb
import java.sql.Connection
import java.util.UUID

class SakendringerDao(
    private val connectionAutoclosing: ConnectionAutoclosing,
    private val hentSak: (id: SakId) -> Sak?,
) {
    internal fun lagreEndringerPaaSak(
        id: SakId,
        kallendeMetode: String,
        block: (connection: Connection) -> Unit,
    ): Int {
        val foer = requireNotNull(hentSak(id)) { "Må ha en sak for å kunne endre den" }
        connectionAutoclosing.hentConnection { connection ->
            block(connection)
        }
        val etter = requireNotNull(hentSak(id)) { "Må ha en sak etter endring" }
        return lagreEndringerPaaSak(foer, etter, kallendeMetode)
    }

    internal fun lagreEndringerPaaSaker(
        saker: Collection<SakId>,
        kallendeMetode: String,
        block: (connection: Connection) -> Unit,
    ) = saker.forEach {
        val foer = requireNotNull(hentSak(it)) { "Må ha en sak for å kunne endre den" }
        connectionAutoclosing.hentConnection { connection ->
            block(connection)
        }
        val etter = requireNotNull(hentSak(it)) { "Må ha en sak etter endring" }
        lagreEndringerPaaSak(foer, etter, kallendeMetode)
    }

    internal fun opprettSak(
        kallendeMetode: String,
        block: (connection: Connection) -> Sak,
    ) = connectionAutoclosing
        .hentConnection { connection ->
            block(connection)
        }.also { lagreEndringerPaaSak(null, it, kallendeMetode) }

    internal fun lagreEndringerPaaSak(
        sakFoer: Sak?,
        sakEtter: Sak,
        kallendeMetode: String,
    ) = connectionAutoclosing.hentConnection {
        with(it) {
            val statement =
                prepareStatement(
                    """
                    INSERT INTO endringer(tabell, id, foer, etter, tidspunkt, saksbehandler, kallendeMetode)
                    VALUES(?, ?::UUID, ?::JSONB, ?::JSONB, ?, ?, ?)
                    """.trimIndent(),
                )
            statement.setObject(1, "sak")
            statement.setObject(2, UUID.randomUUID())
            statement.setJsonb(3, sakFoer)
            statement.setJsonb(4, sakEtter)
            statement.setTidspunkt(5, Tidspunkt.now())
            statement.setString(6, Kontekst.get().AppUser.name())
            statement.setString(7, "${this::class.java.simpleName}: $kallendeMetode")

            statement.executeUpdate()
        }
    }
}
