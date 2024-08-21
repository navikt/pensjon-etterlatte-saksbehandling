package no.nav.etterlatte.sak

import jdk.jfr.internal.EventWriterKey.block
import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.common.ConnectionAutoclosing
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.setTidspunkt
import no.nav.etterlatte.libs.database.setJsonb
import java.sql.Connection
import java.util.UUID

class SakendringerDao(
    private val connectionAutoclosing: ConnectionAutoclosing,
    private val hentSak: (id: SakId) -> Sak?,
) {
    internal fun lagreEndringerPaaSak(
        id: SakId,
        block: (connection: Connection) -> Unit,
    ): Int {
        val foer = requireNotNull(hentSak(id)) { "Må ha en sak for å kunne endre den" }
        connectionAutoclosing.hentConnection { connection ->
            block(connection)
        }
        val etter = requireNotNull(hentSak(id)) { "Må ha en sak etter endring" }
        return lagreEndringerPaaSak(foer, etter)
    }

    internal fun opprettSak(block: (connection: Connection) -> Sak) =
        connectionAutoclosing
            .hentConnection { connection ->
                block(connection)
            }.also { lagreEndringerPaaSak(null, it) }

    internal fun lagreEndringerPaaSak(
        sakFoer: Sak?,
        sakEtter: Sak,
    ) = connectionAutoclosing.hentConnection {
        with(it) {
            val statement =
                prepareStatement(
                    """
                    INSERT INTO sakendringer(id, sakId, sakFoer, sakEtter, tidspunkt, saksbehandler)
                    VALUES(?::UUID, ?::UUID, ?::JSONB, ?::JSONB, ?, ?)
                    """.trimIndent(),
                )
            statement.setObject(1, UUID.randomUUID())
            statement.setObject(2, sakEtter.id)
            statement.setJsonb(3, sakFoer)
            statement.setJsonb(4, sakEtter)
            statement.setTidspunkt(5, Tidspunkt.now())
            statement.setString(6, Kontekst.get().AppUser.name())

            statement.executeUpdate()
        }
    }
}
