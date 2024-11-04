package no.nav.etterlatte.behandling.bosattutland

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.etterlatte.behandling.objectMapper
import no.nav.etterlatte.common.ConnectionAutoclosing
import no.nav.etterlatte.libs.common.dbutils.getUUID
import no.nav.etterlatte.libs.database.setJsonb
import no.nav.etterlatte.libs.database.singleOrNull
import java.sql.ResultSet
import java.util.UUID

class BosattUtlandDao(
    private val connectionAutoclosing: ConnectionAutoclosing,
) {
    fun lagreBosattUtland(bosattUtland: BosattUtland) =
        connectionAutoclosing.hentConnection {
            with(it) {
                val statement =
                    prepareStatement(
                        """
                        insert into bosattutland(behandlingid, rinanummer, mottattSeder, sendteSeder)
                            VALUES(?,?,?,?)
                            ON CONFLICT(behandlingid)
                            DO UPDATE SET rinanummer = excluded.rinanummer,
                            mottattSeder = excluded.mottattSeder, sendteSeder = excluded.sendteSeder
                        """.trimIndent(),
                    )
                statement.setObject(1, bosattUtland.behandlingId)
                statement.setObject(2, bosattUtland.rinanummer)
                statement.setJsonb(3, bosattUtland.mottatteSeder)
                statement.setJsonb(4, bosattUtland.sendteSeder)
                require(statement.executeUpdate() == 1)
            }
        }

    fun hentBosattUtland(behandlingId: UUID): BosattUtland? =
        connectionAutoclosing.hentConnection {
            with(it) {
                val statement =
                    prepareStatement(
                        "SELECT behandlingid, rinanummer, mottattSeder, sendteSeder from bosattutland where behandlingid = ?",
                    )
                statement.setObject(1, behandlingId)
                statement.executeQuery().singleOrNull { toBosattUtland() }
            }
        }

    private fun ResultSet.toBosattUtland(): BosattUtland =
        BosattUtland(
            behandlingId = getUUID("behandlingid"),
            rinanummer = getString("rinanummer"),
            mottatteSeder = getString("mottattSeder").let { objectMapper.readValue(it) },
            sendteSeder = getString("sendteSeder").let { objectMapper.readValue(it) },
        )
}
