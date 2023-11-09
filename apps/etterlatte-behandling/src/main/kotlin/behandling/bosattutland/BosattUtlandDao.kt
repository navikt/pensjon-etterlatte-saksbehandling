package no.nav.etterlatte.behandling.bosattutland

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.etterlatte.behandling.hendelse.getUUID
import no.nav.etterlatte.behandling.objectMapper
import no.nav.etterlatte.libs.database.setJsonb
import no.nav.etterlatte.libs.database.single
import no.nav.etterlatte.libs.database.singleOrNull
import java.sql.Connection
import java.sql.ResultSet
import java.util.UUID

class BosattUtlandDao(private val connection: () -> Connection) {
    fun lagreBosattUtland(bosattUtland: BosattUtland): BosattUtland {
        return with(connection()) {
            val statement =
                prepareStatement(
                    "insert into bosattutland(behandlingid, rinanummer, mottattSeder, sendteSeder) " +
                        "VALUES(?,?,?,?) RETURNING behandlingid, rinanummer, mottattSeder, sendteSeder",
                )
            statement.setObject(1, bosattUtland.behandlingid)
            statement.setObject(2, bosattUtland.rinanummer)
            statement.setJsonb(3, bosattUtland.mottatteSeder)
            statement.setJsonb(4, bosattUtland.sendteSeder)
            statement.executeQuery().single { toBosattUtland() }
        }
    }

    fun hentBosattUtland(behandlingId: UUID): BosattUtland? {
        return with(connection()) {
            val statement =
                prepareStatement("SELECT behandlingid, rinanummer, mottattSeder, sendteSeder from bosattutland where behandlingid = ?")
            statement.setObject(1, behandlingId)
            statement.executeQuery().singleOrNull { toBosattUtland() }
        }
    }

    private fun ResultSet.toBosattUtland(): BosattUtland {
        return BosattUtland(
            behandlingid = getUUID("behandlingid"),
            rinanummer = getString("rinanummer"),
            mottatteSeder = getString("mottattSeder").let { objectMapper.readValue(it) },
            sendteSeder = getString("sendteSeder").let { objectMapper.readValue(it) },
        )
    }
}
