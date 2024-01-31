package no.nav.etterlatte

import no.nav.etterlatte.behandling.klienter.SaksbehandlerInfo
import no.nav.etterlatte.libs.database.single
import no.nav.etterlatte.libs.database.toList
import javax.sql.DataSource

class SaksbehandlerInfoDao(private val dataSource: DataSource) {
    fun hentalleSaksbehandlere(): List<String?> {
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

    fun saksbehandlerFinnes(ident: String): Boolean {
        dataSource.connection.use {
            val statement =
                it.prepareStatement(
                    """
                    SELECT EXISTS(SELECT 1 FROM saksbehandlerInfo where id = ?);
                    """.trimIndent(),
                )
            statement.setString(1, ident)
            return statement.executeQuery().single {
                val trueOrFalsePostgresFormat = getString("exists")
                return@single trueOrFalsePostgresFormat == "t"
            }
        }
    }
}
