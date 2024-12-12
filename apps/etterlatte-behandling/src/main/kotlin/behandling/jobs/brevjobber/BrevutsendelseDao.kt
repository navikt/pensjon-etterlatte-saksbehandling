package no.nav.etterlatte.behandling.jobs.brevjobber

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

    fun opprettJobb(jobb: Brevutsendelse): Brevutsendelse {
        connectionAutoclosing.hentConnection {
            with(it) {
                val statement =
                    prepareStatement(
                        """
                        INSERT INTO brevutsendelse(id, sak_id, status, merknad, resultat, opprettet, endret, jobbtype)
                        VALUES(?::UUID, ?, ?, ?, ?, ?, ?, ?)
                        """.trimIndent(),
                    )
                statement.setObject(1, jobb.id)
                statement.setLong(2, jobb.sakId.sakId)
                statement.setString(3, jobb.status.name)
                statement.setObject(4, jobb.merknad)
                statement.setObject(5, jobb.resultat)
                statement.setTidspunkt(6, jobb.opprettet)
                statement.setTidspunkt(7, jobb.sistEndret)
                statement.setString(8, jobb.type.name)
                statement.executeUpdate()
                logger.info("Opprettet jobb av type ${jobb.type.name} for sak ${jobb.sakId} med status ${jobb.status}")
            }
        }

        return hentJobb(jobb.id) ?: throw IllegalStateException("Fant ikke jobb $jobb")
    }

    fun oppdaterJobb(jobb: Brevutsendelse): Brevutsendelse {
        connectionAutoclosing.hentConnection {
            with(it) {
                val statement =
                    prepareStatement(
                        """
                        UPDATE brevutsendelse SET status = ? WHERE id = ?
                        """.trimIndent(),
                    )
                statement.setString(1, jobb.status.name)
                statement.setObject(2, jobb.id)
                statement.executeUpdate()
            }
        }
        return hentJobb(jobb.id) ?: throw IllegalStateException("Fant ikke jobb $jobb")
    }

    fun hentJobb(id: UUID): Brevutsendelse? =
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
                    asJobb()
                }
            }
        }

    fun hentNyeJobber(
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
                statement.setString(paramIndex, ArbeidStatus.NY.name)
                paramIndex += 1

                if (spesifikkeSaker.isNotEmpty()) {
                    statement.setArray(paramIndex, createArrayOf("bigint", spesifikkeSaker.toTypedArray()))
                    paramIndex += 1
                }
                if (ekskluderteSaker.isNotEmpty()) {
                    statement.setArray(paramIndex, createArrayOf("bigint", ekskluderteSaker.toTypedArray()))
                    paramIndex += 1
                }

                statement.executeQuery().toList {
                    asJobb()
                }
            }
        }

    private fun ResultSet.asJobb() =
        Brevutsendelse(
            id = getObject("id") as UUID,
            sakId = SakId(getLong("sak_id")),
            type = JobbType.valueOf(getString("jobbtype")),
            status = ArbeidStatus.valueOf(getString("status")),
            resultat = getString("resultat"),
            merknad = getString("merknad"),
            opprettet = getTidspunkt("opprettet"),
            sistEndret = getTidspunkt("endret"),
        )
}
