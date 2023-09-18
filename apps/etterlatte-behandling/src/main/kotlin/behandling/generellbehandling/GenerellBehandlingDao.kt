package no.nav.etterlatte.behandling.generellbehandling

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.etterlatte.behandling.hendelse.getUUID
import no.nav.etterlatte.libs.common.generellbehandling.GenerellBehandling
import no.nav.etterlatte.libs.common.generellbehandling.Innhold
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.tidspunkt.getTidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.setTidspunkt
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
                    INSERT INTO generellbehandling(id, innhold, sak_id, opprettet, type)
                    VALUES(?::UUID, ?, ?, ?, ?)
                    """.trimIndent()
                )
            statement.setObject(1, generellBehandling.id)
            statement.setJsonb(2, generellBehandling.innhold)
            statement.setLong(3, generellBehandling.sakId)
            statement.setTidspunkt(4, generellBehandling.opprettet)
            statement.setString(5, generellBehandling.type.name)

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

    private fun ResultSet.toGenerellBehandling(): GenerellBehandling {
        val type = GenerellBehandling.GenerellBehandlingType.valueOf(getString("type"))
        val innholdTmp = getString("innhold")
        val innhold =
            when (type) {
                GenerellBehandling.GenerellBehandlingType.ANNEN -> objectMapper.readValue<Innhold.Annen>(innholdTmp)
                GenerellBehandling.GenerellBehandlingType.UTLAND -> objectMapper.readValue<Innhold.Utland>(innholdTmp)
            }
        return GenerellBehandling(
            id = getUUID("id"),
            sakId = getLong("sak_id"),
            type = type,
            innhold = innhold,
            opprettet = getTidspunkt("opprettet")
        )
    }
}