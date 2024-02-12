package no.nav.etterlatte.saksbehandler

import no.nav.etterlatte.behandling.klienter.SaksbehandlerInfo
import no.nav.etterlatte.libs.database.setJsonb
import no.nav.etterlatte.libs.database.singleOrNull
import no.nav.etterlatte.libs.database.toList
import java.sql.Connection

class SaksbehandlerInfoDaoTrans(private val connection: () -> Connection) {
    fun hentSaksbehandlerNavn(ident: String): String? {
        with(connection()) {
            val statement =
                prepareStatement(
                    """
                    SELECT navn from saksbehandler_info
                    where id = ?
                    """.trimIndent(),
                )
            statement.setString(1, ident)
            return statement.executeQuery().singleOrNull {
                getString(1)
            }
        }
    }

    fun hentSaksbehandlereForEnhet(enhet: String): List<SaksbehandlerInfo> {
        with(connection()) {
            val statement =
                prepareStatement(
                    """
                    SELECT id, navn from saksbehandler_info
                    where enheter @> ?::JSONB
                    """.trimIndent(),
                )
            statement.setJsonb(1, enhet)
            return statement.executeQuery().toList {
                SaksbehandlerInfo(
                    getString("id"),
                    getString("navn"),
                )
            }
        }
    }
}
