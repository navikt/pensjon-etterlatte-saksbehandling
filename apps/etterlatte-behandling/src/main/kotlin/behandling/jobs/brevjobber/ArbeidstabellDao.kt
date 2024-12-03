package no.nav.etterlatte.behandling.jobs.brevjobber

import no.nav.etterlatte.common.ConnectionAutoclosing
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.tidspunkt.getTidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.setTidspunkt
import no.nav.etterlatte.libs.database.toList
import org.slf4j.LoggerFactory
import java.sql.ResultSet
import java.util.UUID

class ArbeidstabellDao(
    private val connectionAutoclosing: ConnectionAutoclosing,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    fun opprettJobb(jobb: Arbeidsjobb) {
        connectionAutoclosing.hentConnection {
            with(it) {
                val statement =
                    prepareStatement(
                        """
                        INSERT INTO arbeidstabell(id, sak_id, status, merknad, resultat, opprettet, endret, jobbtype)
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
                logger.info("Opprettet en jobb for sak ${jobb.sakId} med status ${jobb.status}")
            }
        }
    }

    fun hentKlareJobber(): List<Arbeidsjobb> =
        connectionAutoclosing.hentConnection {
            with(it) {
                val statement =
                    prepareStatement(
                        """
                        SELECT * from arbeidstabell where status = ?
                        """.trimIndent(),
                    )
                statement.setString(1, ArbeidStatus.NY.name)
                statement.executeQuery().toList {
                    asJobb()
                }
            }
        }

    private fun ResultSet.asJobb() =
        Arbeidsjobb(
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
