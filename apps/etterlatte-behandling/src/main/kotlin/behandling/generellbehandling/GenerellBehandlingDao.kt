package no.nav.etterlatte.behandling.generellbehandling

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.etterlatte.behandling.hendelse.getUUID
import no.nav.etterlatte.common.ConnectionAutoclosing
import no.nav.etterlatte.libs.common.generellbehandling.GenerellBehandling
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.tidspunkt.getTidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.setTidspunkt
import no.nav.etterlatte.libs.database.setJsonb
import no.nav.etterlatte.libs.database.single
import no.nav.etterlatte.libs.database.singleOrNull
import no.nav.etterlatte.libs.database.toList
import java.sql.ResultSet
import java.util.UUID

class GenerellBehandlingDao(
    private val connectionAutoclosing: ConnectionAutoclosing,
) {
    fun opprettGenerellbehandling(generellBehandling: GenerellBehandling): GenerellBehandling =
        connectionAutoclosing.hentConnection {
            with(it) {
                val statement =
                    prepareStatement(
                        """
                        INSERT INTO generellbehandling(id, innhold, sak_id, opprettet, type, tilknyttet_behandling, status)
                        VALUES(?::UUID, ?, ?, ?, ?, ?, ?)
                        RETURNING id, innhold, sak_id, opprettet, type, tilknyttet_behandling, status, behandler, attestant, kommentar
                        """.trimIndent(),
                    )
                statement.setObject(1, generellBehandling.id)
                statement.setJsonb(2, generellBehandling.innhold)
                statement.setLong(3, generellBehandling.sakId)
                statement.setTidspunkt(4, generellBehandling.opprettet)
                statement.setString(5, generellBehandling.type.name)
                statement.setObject(6, generellBehandling.tilknyttetBehandling)
                statement.setString(7, generellBehandling.status.name)

                statement.executeQuery().single { toGenerellBehandling() }
            }
        }

    fun oppdaterGenerellBehandling(generellBehandling: GenerellBehandling): GenerellBehandling =
        connectionAutoclosing.hentConnection {
            with(it) {
                val statement =
                    prepareStatement(
                        """
                        UPDATE generellbehandling
                        SET innhold = ?, status = ?, behandler = ?, attestant = ?, kommentar = ?
                        where id = ?
                        RETURNING id, innhold, sak_id, opprettet, type, tilknyttet_behandling, status, behandler, attestant, kommentar
                        """.trimIndent(),
                    )
                statement.setJsonb(1, generellBehandling.innhold)
                statement.setString(2, generellBehandling.status.name)
                statement.setJsonb(3, generellBehandling.behandler)
                statement.setJsonb(4, generellBehandling.attestant)
                statement.setString(5, generellBehandling.returnertKommenar)
                statement.setObject(6, generellBehandling.id)
                statement.executeQuery().single { toGenerellBehandling() }
            }
        }

    fun hentGenerellBehandlingMedId(id: UUID): GenerellBehandling? =
        connectionAutoclosing.hentConnection {
            with(it) {
                val statement =
                    prepareStatement(
                        """
                        SELECT id, innhold, sak_id, opprettet, type, tilknyttet_behandling, status, behandler, attestant, kommentar
                        FROM generellbehandling
                        WHERE id = ?
                        """.trimIndent(),
                    )
                statement.setObject(1, id)
                statement.executeQuery().singleOrNull {
                    toGenerellBehandling()
                }
            }
        }

    fun hentGenerellBehandlingForSak(sakId: Long): List<GenerellBehandling> =
        connectionAutoclosing.hentConnection {
            with(it) {
                val statement =
                    prepareStatement(
                        """
                        SELECT id, innhold, sak_id, opprettet, type, tilknyttet_behandling, status, behandler, attestant, kommentar
                        FROM generellbehandling
                        WHERE sak_id = ?
                        """.trimIndent(),
                    )
                statement.setLong(1, sakId)
                statement.executeQuery().toList {
                    toGenerellBehandling()
                }
            }
        }

    fun hentBehandlingForTilknyttetBehandling(tilknyttetBehandlingId: UUID): GenerellBehandling? =
        connectionAutoclosing.hentConnection {
            with(it) {
                val statement =
                    prepareStatement(
                        """
                        SELECT id, innhold, sak_id, opprettet, type, tilknyttet_behandling, status, behandler, attestant, kommentar
                        FROM generellbehandling
                        WHERE tilknyttet_behandling = ?::uuid
                        """.trimIndent(),
                    )

                statement.setObject(1, tilknyttetBehandlingId)
                statement.executeQuery().singleOrNull {
                    toGenerellBehandling()
                }
            }
        }

    private fun ResultSet.toGenerellBehandling(): GenerellBehandling =
        GenerellBehandling(
            id = getUUID("id"),
            sakId = getLong("sak_id"),
            type = GenerellBehandling.GenerellBehandlingType.valueOf(getString("type")),
            innhold = getString("innhold")?.let { objectMapper.readValue(it) },
            opprettet = getTidspunkt("opprettet"),
            tilknyttetBehandling = getObject("tilknyttet_behandling") as UUID?,
            status = GenerellBehandling.Status.valueOf(getString("status")),
            behandler = getString("behandler")?.let { objectMapper.readValue(it) },
            attestant = getString("attestant")?.let { objectMapper.readValue(it) },
            returnertKommenar = getString("kommentar"),
        )
}
