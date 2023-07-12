package no.nav.etterlatte.oppgaveny

import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.tidspunkt.getTidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.getTidspunktOrNull
import no.nav.etterlatte.libs.common.tidspunkt.setTidspunkt
import no.nav.etterlatte.libs.database.toList
import org.slf4j.LoggerFactory
import java.sql.Connection
import java.util.*

class OppgaveDaoNy(private val connection: () -> Connection) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    fun lagreOppgave(oppgaveNy: OppgaveNy) {
        with(connection()) {
            val statement = prepareStatement(
                """
                INSERT INTO oppgave(id, status, enhet, sak_id, type, saksbehandler, referanse, merknad, opprettet, saktype, fnr, frist)
                VALUES(?::UUID, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent()
            )
            statement.setObject(1, oppgaveNy.id)
            statement.setString(2, oppgaveNy.status.toString())
            statement.setString(3, oppgaveNy.enhet)
            statement.setLong(4, oppgaveNy.sakId)
            statement.setString(5, oppgaveNy.type.toString())
            statement.setString(6, oppgaveNy.saksbehandler)
            statement.setString(7, oppgaveNy.referanse)
            statement.setString(8, oppgaveNy.merknad)
            statement.setTidspunkt(9, oppgaveNy.opprettet)
            statement.setString(10, oppgaveNy.sakType.toString())
            statement.setString(11, oppgaveNy.fnr)
            statement.setTidspunkt(12, oppgaveNy.frist)
            statement.executeUpdate()
            logger.info("lagret oppgave for ${oppgaveNy.id} for sakid ${oppgaveNy.sakId}")
        }
    }

    fun hentOppgaver(): List<OppgaveNy> {
        with(connection()) {
            val statement = prepareStatement(
                """
                    SELECT id, status, enhet, sak_id, type, saksbehandler, referanse, merknad, opprettet, saktype, fnr, frist
                    FROM oppgave
                """.trimIndent()
            )
            return statement.executeQuery().toList {
                OppgaveNy(
                    id = getObject("id") as UUID,
                    status = Status.valueOf(getString("status")),
                    enhet = getString("enhet"),
                    sakId = getLong("sak_id"),
                    type = OppgaveType.valueOf(getString("type")),
                    saksbehandler = getString("saksbehandler"),
                    referanse = getString("referanse"),
                    merknad = getString("merknad"),
                    opprettet = getTidspunkt("opprettet"),
                    sakType = SakType.valueOf(getString("saktype")),
                    fnr = getString("fnr"),
                    frist = getTidspunktOrNull("frist")
                )
            }.also {
                logger.info("Hentet antall nye oppgaver: ${it.size}")
            }
        }
    }
}