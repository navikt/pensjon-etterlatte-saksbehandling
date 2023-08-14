package no.nav.etterlatte.metrics

import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.oppgaveNy.OppgaveKilde
import no.nav.etterlatte.libs.common.oppgaveNy.OppgaveNy
import no.nav.etterlatte.libs.common.oppgaveNy.OppgaveType
import no.nav.etterlatte.libs.common.oppgaveNy.Status
import no.nav.etterlatte.libs.common.tidspunkt.getTidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.getTidspunktOrNull
import no.nav.etterlatte.libs.database.toList
import org.slf4j.LoggerFactory
import java.sql.ResultSet
import java.util.*
import javax.sql.DataSource

class MetrikkerDao(private val dataSource: DataSource) {

    private val logger = LoggerFactory.getLogger(this::class.java)

    fun hentAlleOppgaver(): List<OppgaveNy> {
        dataSource.connection.use {
            val statement = it.prepareStatement(
                """
                    SELECT *
                    FROM oppgave
                """.trimIndent()
            )
            return statement.executeQuery().toList {
               asOppgaveNy()
            }
        }
    }

    private fun ResultSet.asOppgaveNy(): OppgaveNy {
        return OppgaveNy(
            id = getObject("id") as UUID,
            status = Status.valueOf(getString("status")),
            enhet = getString("enhet"),
            sakId = getLong("sak_id"),
            kilde = getString("kilde")?.let { OppgaveKilde.valueOf(it) },
            type = OppgaveType.valueOf(getString("type")),
            saksbehandler = getString("saksbehandler"),
            referanse = getString("referanse"),
            merknad = getString("merknad"),
            opprettet = getTidspunkt("opprettet"),
            sakType = SakType.valueOf(getString("saktype")),
            fnr = getString("fnr"),
            frist = getTidspunktOrNull("frist")
        )
    }
}