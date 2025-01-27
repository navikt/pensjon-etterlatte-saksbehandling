package no.nav.etterlatte.behandling.brevutsendelse

import no.nav.etterlatte.common.ConnectionAutoclosing
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.tidspunkt.getTidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.setTidspunkt
import no.nav.etterlatte.libs.database.singleOrNull
import no.nav.etterlatte.libs.database.toList
import org.slf4j.LoggerFactory
import java.sql.ResultSet
import java.util.UUID

class BrevutsendelseDao(
    private val connectionAutoclosing: ConnectionAutoclosing,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    fun opprettBrevutsendelse(brevutsendelse: Brevutsendelse): Brevutsendelse {
        connectionAutoclosing.hentConnection {
            with(it) {
                val statement =
                    prepareStatement(
                        """
                        INSERT INTO brevutsendelse(id, sak_id, status, merknad, resultat, opprettet, endret, jobbtype)
                        VALUES(?::UUID, ?, ?, ?, ?, ?, ?, ?)
                        """.trimIndent(),
                    )
                statement.setObject(1, brevutsendelse.id)
                statement.setLong(2, brevutsendelse.sakId.value)
                statement.setString(3, brevutsendelse.status.name)
                statement.setObject(4, brevutsendelse.merknad)
                statement.setObject(5, brevutsendelse.resultat)
                statement.setTidspunkt(6, brevutsendelse.opprettet)
                statement.setTidspunkt(7, brevutsendelse.sistEndret)
                statement.setString(8, brevutsendelse.type.name)
                statement.executeUpdate()
                logger.info(
                    "Opprettet brevutsendelse av type ${brevutsendelse.type.name} for sak ${brevutsendelse.sakId} med status ${brevutsendelse.status}",
                )
            }
        }

        return hentBrevutsendelse(brevutsendelse.id) ?: throw IllegalStateException("Fant ikke brevutsendelse $brevutsendelse")
    }

    fun oppdaterBrevutsendelse(brevutsendelse: Brevutsendelse): Brevutsendelse {
        connectionAutoclosing.hentConnection {
            with(it) {
                val statement =
                    prepareStatement(
                        """
                        UPDATE brevutsendelse SET status = ? WHERE id = ?
                        """.trimIndent(),
                    )
                statement.setString(1, brevutsendelse.status.name)
                statement.setObject(2, brevutsendelse.id)
                statement.executeUpdate()
            }
        }
        return hentBrevutsendelse(brevutsendelse.id) ?: throw IllegalStateException("Fant ikke brevutsendelse $brevutsendelse")
    }

    fun hentBrevutsendelse(id: UUID): Brevutsendelse? =
        connectionAutoclosing.hentConnection {
            with(it) {
                val statement =
                    prepareStatement(
                        """
                        SELECT * from brevutsendelse where id = ?
                        """.trimIndent(),
                    )
                statement.setObject(1, id)
                statement.executeQuery().singleOrNull {
                    asBrevutsendelse()
                }
            }
        }

    fun hentBrevutsendelser(
        antall: Int,
        spesifikkeSaker: List<SakId> = emptyList(),
        ekskluderteSaker: List<SakId> = emptyList(),
    ): List<Brevutsendelse> =
        connectionAutoclosing.hentConnection {
            with(it) {
                val statement =
                    prepareStatement(
                        """
                        SELECT * FROM brevutsendelse 
                        WHERE status = ?
                        ${if (spesifikkeSaker.isEmpty()) "" else " AND sak_id = ANY(?)"}
                        ${if (ekskluderteSaker.isEmpty()) "" else " AND NOT(sak_id = ANY(?))"}
                        ORDER BY sak_id ASC
                        LIMIT $antall
                        """.trimIndent(),
                    )

                var paramIndex = 1
                statement.setString(paramIndex, BrevutsendelseStatus.NY.name)
                paramIndex += 1

                if (spesifikkeSaker.isNotEmpty()) {
                    statement.setArray(paramIndex, createArrayOf("bigint", spesifikkeSaker.toTypedArray()))
                    paramIndex += 1
                }
                if (ekskluderteSaker.isNotEmpty()) {
                    statement.setArray(paramIndex, createArrayOf("bigint", ekskluderteSaker.toTypedArray()))
                    paramIndex += 1
                }

                statement.executeQuery().toList { asBrevutsendelse() }
            }
        }

    private fun ResultSet.asBrevutsendelse() =
        Brevutsendelse(
            id = getObject("id") as UUID,
            sakId = SakId(getLong("sak_id")),
            type = BrevutsendelseType.valueOf(getString("jobbtype")),
            status = BrevutsendelseStatus.valueOf(getString("status")),
            resultat = getString("resultat"),
            merknad = getString("merknad"),
            opprettet = getTidspunkt("opprettet"),
            sistEndret = getTidspunkt("endret"),
        )
}
