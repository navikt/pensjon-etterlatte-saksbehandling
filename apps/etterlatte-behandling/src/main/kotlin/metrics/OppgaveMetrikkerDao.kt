package no.nav.etterlatte.metrics

import no.nav.etterlatte.libs.common.oppgave.Status
import no.nav.etterlatte.libs.database.toList
import org.slf4j.LoggerFactory
import java.sql.ResultSet
import java.util.UUID
import javax.sql.DataSource

class OppgaveMetrikkerDao(private val dataSource: DataSource) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    fun hentOppgaveAntall(): OppgaveAntall {
        val alleOppgaver = hentAlleOppgaver()
        val aktive = alleOppgaver.filter { !Status.erAvsluttet(it.status) }.size
        val avsluttet = alleOppgaver.filter { Status.erAvsluttet(it.status) }.size
        val totalt = aktive + avsluttet
        return OppgaveAntall(
            totalt = totalt,
            aktive = aktive,
            avsluttet = avsluttet,
        )
    }

    private fun hentAlleOppgaver(): List<OppgaveMetrikker> {
        dataSource.connection.use {
            val statement =
                it.prepareStatement(
                    """
                    SELECT id, status
                    FROM oppgave
                    """.trimIndent(),
                )
            return statement.executeQuery().toList {
                asOppgaveMetrikker()
            }
        }
    }

    private fun ResultSet.asOppgaveMetrikker(): OppgaveMetrikker {
        return OppgaveMetrikker(
            id = getObject("id") as UUID,
            status = Status.valueOf(getString("status")),
        )
    }
}

data class OppgaveMetrikker(
    val id: UUID,
    val status: Status,
)

data class OppgaveAntall(
    val totalt: Int,
    val aktive: Int,
    val avsluttet: Int,
)
