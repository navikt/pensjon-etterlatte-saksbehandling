package no.nav.etterlatte.behandling.generellbehandling

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.etterlatte.behandling.hendelse.getUUID
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.database.setJsonb
import no.nav.etterlatte.libs.database.singleOrNull
import no.nav.etterlatte.libs.database.toList
import java.sql.Connection
import java.sql.ResultSet
import java.util.*

class GenerellBehandlingDao(private val connection: () -> Connection) {
    fun opprettGenerellbehandling(generellBehandling: GenerellBehandling) {
        with(connection()) {
            val statement =
                prepareStatement(
                    """
                    INSERT INTO generellbehandling(id, innhold, sak_id)
                    VALUES(?::UUID, ?, ?)
                    """.trimIndent()
                )
            statement.setObject(1, generellBehandling.id)
            statement.setJsonb(2, generellBehandling.innhold)
            statement.setLong(3, generellBehandling.sakId)

            statement.executeUpdate()
        }
    }

    fun hentGenerellBehandlingMedId(id: UUID): GenerellBehandling? {
        return with(connection()) {
            val statement =
                prepareStatement(
                    """
                    SELECT * FROM generellbehandling
                    WHERE id = ?
                    """.trimIndent()
                )
            statement.setObject(1, id)
            statement.executeQuery().singleOrNull {
                toGenerellBehandling()
            }
        }
    }

    fun hentGenerellBehandlingForSak(sakId: Long): List<GenerellBehandling> {
        return with(connection()) {
            val statement =
                prepareStatement(
                    """
                    SELECT * FROM generellbehandling
                    WHERE sak_id = ?
                    """.trimIndent()
                )
            statement.setLong(1, sakId)
            statement.executeQuery().toList {
                toGenerellBehandling()
            }
        }
    }

    private fun ResultSet.toGenerellBehandling() =
        GenerellBehandling(
            id = getUUID("id"),
            sakId = getLong("sak_id"),
            innhold = getString("innhold").let { objectMapper.readValue(it) }
        )
}