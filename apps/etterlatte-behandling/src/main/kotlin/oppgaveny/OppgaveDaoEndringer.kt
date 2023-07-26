package no.nav.etterlatte.oppgaveny

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.etterlatte.behandling.objectMapper
import no.nav.etterlatte.grunnlagsendring.setJsonb
import no.nav.etterlatte.libs.common.oppgaveNy.OppgaveNy
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.getTidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.setTidspunkt
import no.nav.etterlatte.libs.database.toList
import java.sql.Connection
import java.sql.ResultSet
import java.util.*

class OppgaveDaoEndringer(private val connection: () -> Connection) {
    fun lagreEndringerPaaOppgave(oppgaveFoer: OppgaveNy, oppgaveEtter: OppgaveNy) {
        with(connection()) {
            val statement = prepareStatement(
                """
                INSERT INTO oppgaveendringer(id, oppgaveId, oppgaveFoer, oppgaveEtter, tidspunkt)
                VALUES(?::UUID, ?::UUID, ?::JSONB, ?::JSONB, ?)
                """.trimIndent()
            )
            statement.setObject(1, UUID.randomUUID())
            statement.setObject(2, oppgaveEtter.id)
            statement.setJsonb(3, oppgaveFoer)
            statement.setJsonb(4, oppgaveEtter)
            statement.setTidspunkt(5, Tidspunkt.now())

            statement.executeUpdate()
        }
    }

    fun hentEndringerForOppgave(oppgaveId: UUID): List<OppgaveEndring> {
        with(connection()) {
            val statement = prepareStatement(
                """
                   SELECT id, oppgaveId, oppgaveFoer, oppgaveEtter, tidspunkt
                   FROM oppgaveendringer
                   where oppgaveId = ?::UUID
                """.trimIndent()
            )
            statement.setObject(1, oppgaveId)

            return statement.executeQuery().toList {
                asOppgaveEndring()
            }
        }
    }

    private fun ResultSet.asOppgaveEndring(): OppgaveEndring {
        return OppgaveEndring(
            id = getObject("id") as UUID,
            oppgaveId = getObject("id") as UUID,
            oppgaveFoer = getString("oppgaveFoer").let { objectMapper.readValue(it) },
            oppgaveEtter = getString("oppgaveEtter").let { objectMapper.readValue(it) },
            tidspunkt = getTidspunkt("tidspunkt")
        )
    }
}