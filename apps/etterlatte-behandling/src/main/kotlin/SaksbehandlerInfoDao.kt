package no.nav.etterlatte

import no.nav.etterlatte.behandling.domain.SaksbehandlerEnhet
import no.nav.etterlatte.behandling.klienter.SaksbehandlerInfo
import no.nav.etterlatte.libs.database.setJsonb
import no.nav.etterlatte.libs.database.single
import no.nav.etterlatte.libs.database.toList
import javax.sql.DataSource

class SaksbehandlerInfoDao(private val dataSource: DataSource) {
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
            }.filterNotNull()
        }
    }

    fun upsertSaksbehandlerNavn(saksbehandler: SaksbehandlerInfo) {
        dataSource.connection.use {
            val statement =
                it.prepareStatement(
                    """
                    INSERT INTO saksbehandler_info(id, navn) 
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

    fun upsertSaksbehandlerEnheter(saksbehandlerMedEnheter: Pair<String, List<SaksbehandlerEnhet>>) {
        dataSource.connection.use {
            val statement =
                it.prepareStatement(
                    """
                    INSERT INTO saksbehandler_info(id, enheter) 
                    VALUES(?,?)
                    ON CONFLICT (id)
                    DO UPDATE SET enheter = excluded.enheter
                    """.trimIndent(),
                )
            statement.setString(1, saksbehandlerMedEnheter.first)
            statement.setJsonb(2, saksbehandlerMedEnheter.second)
            statement.executeUpdate()
        }
    }

    fun saksbehandlerFinnes(ident: String): Boolean {
        dataSource.connection.use {
            val statement =
                it.prepareStatement(
                    """
                    SELECT EXISTS(SELECT 1 FROM saksbehandler_info where id = ?);
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
