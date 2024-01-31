package no.nav.etterlatte

import no.nav.etterlatte.behandling.klienter.SaksbehandlerInfo
import no.nav.etterlatte.libs.database.toList
import javax.sql.DataSource

class SaksbehandlerDao(private val dataSource: DataSource) {
    fun hentalleSaksbehandlere(): List<String> {
        dataSource.connection.use {
            val statement =
                it.prepareStatement(
                    """
                    select distinct saksbehandler from oppgave;
                    """.trimIndent(),
                )
            return statement.executeQuery().toList {
                getString("saksbehandler")
            }
        }
    }

    fun upsertSaksbehandler(saksbehandler: SaksbehandlerInfo) {
        dataSource.connection.use {
            val statement =
                it.prepareStatement(
                    """
                    INSERT INTO saksbehandlerInfo(id, navn) 
                    VALUES(?,?)
                    ON CONFLICT (id)
                    DO UPDATE SET navn = excluded.navn
                    """.trimIndent(),
                )
            statement.setString(1, saksbehandler.ident)
            statement.setString(2, saksbehandler.navn)
            statement.executeUpdate()
        }
    }
}
