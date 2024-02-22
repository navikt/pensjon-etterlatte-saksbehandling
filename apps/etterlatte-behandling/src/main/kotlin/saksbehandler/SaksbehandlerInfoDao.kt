package no.nav.etterlatte.saksbehandler

import no.nav.etterlatte.behandling.klienter.SaksbehandlerInfo
import no.nav.etterlatte.common.ConnectionAutoclosing
import no.nav.etterlatte.libs.database.setJsonb
import no.nav.etterlatte.libs.database.single
import no.nav.etterlatte.libs.database.singleOrNull
import no.nav.etterlatte.libs.database.toList

class SaksbehandlerInfoDao(private val connectionAutoclosing: ConnectionAutoclosing) {
    fun hentSaksbehandlerNavn(ident: String): String? {
        return connectionAutoclosing.hentConnection {
            with(it) {
                val statement =
                    prepareStatement(
                        """
                        SELECT navn from saksbehandler_info
                        where id = ?
                        """.trimIndent(),
                    )
                statement.setString(1, ident)
                statement.executeQuery().singleOrNull {
                    getString(1)
                }
            }
        }
    }

    fun hentSaksbehandlereForEnhet(enhet: String): List<SaksbehandlerInfo> {
        return connectionAutoclosing.hentConnection {
            with(it) {
                val statement =
                    prepareStatement(
                        """
                        SELECT id, navn from saksbehandler_info
                        where enheter @> ?::JSONB
                        """.trimIndent(),
                    )
                statement.setJsonb(1, enhet)
                statement.executeQuery().toList {
                    SaksbehandlerInfo(
                        getString("id"),
                        getString("navn"),
                    )
                }
            }
        }
    }

    fun hentalleSaksbehandlere(): List<String> {
        return connectionAutoclosing.hentConnection {
            with(it) {
                val statement =
                    it.prepareStatement(
                        """
                        select distinct saksbehandler from oppgave;
                        """.trimIndent(),
                    )
                statement.executeQuery().toList {
                    getString("saksbehandler")
                }.filterNotNull()
            }
        }
    }

    fun upsertSaksbehandlerNavn(saksbehandler: SaksbehandlerInfo) {
        connectionAutoclosing.hentConnection {
            with(it) {
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
    }

    fun upsertSaksbehandlerEnheter(saksbehandlerMedEnheter: Pair<String, List<String>>) {
        connectionAutoclosing.hentConnection {
            with(it) {
                val statement =
                    it.prepareStatement(
                        """
                        UPDATE saksbehandler_info
                        SET enheter = ?
                        WHERE id = ?
                        """.trimIndent(),
                    )
                statement.setJsonb(1, saksbehandlerMedEnheter.second)
                statement.setString(2, saksbehandlerMedEnheter.first)
                statement.executeUpdate()
            }
        }
    }

    fun saksbehandlerFinnes(ident: String): Boolean {
        return connectionAutoclosing.hentConnection {
            with(it) {
                val statement =
                    it.prepareStatement(
                        """
                        SELECT EXISTS(SELECT 1 FROM saksbehandler_info where id = ?);
                        """.trimIndent(),
                    )
                statement.setString(1, ident)
                statement.executeQuery().single {
                    val trueOrFalsePostgresFormat = getString("exists")
                    return@single trueOrFalsePostgresFormat == "t"
                }
            }
        }
    }
}
