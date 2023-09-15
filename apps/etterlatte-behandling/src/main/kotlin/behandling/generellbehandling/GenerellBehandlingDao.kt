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
                    INSERT INTO generellbehandling(id, type, innhold, sak_id)
                    VALUES(?::UUID, ?, ?, ?)
                    """.trimIndent()
                )
            statement.setObject(1, generellBehandling.id)
            statement.setString(2, generellBehandling.type.name)
            statement.setJsonb(3, generellBehandling.innhold)
            statement.setLong(4, generellBehandling.sakId)

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
            type = GenerellBehandlingType.valueOf(getString("type")),
            innhold = getString("innhold").let { objectMapper.readValue(it) }
        )
}