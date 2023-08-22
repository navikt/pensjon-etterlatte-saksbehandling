package no.nav.etterlatte.metrics

import no.nav.etterlatte.libs.common.oppgaveNy.Status
import no.nav.etterlatte.libs.database.toList
import org.slf4j.LoggerFactory
import javax.sql.DataSource

class MetrikkerDao(private val dataSource: DataSource) {

    private val logger = LoggerFactory.getLogger(this::class.java)
    fun hentOppgaverMedStatus(): List<Pair<Status, Long>> {
        dataSource.connection.use {
            val statement = it.prepareStatement(
                """
                SELECT status, count(*) as antall
                FROM oppgave
                GROUP BY status
                """.trimIndent()
            )
            return statement.executeQuery().toList {
                enumValueOf<Status>(getString("status")) to getLong("antall")
            }.also {
                logger.debug("Hentet ut antall oppgaver per status for metrikker")
            }
        }
    }
}