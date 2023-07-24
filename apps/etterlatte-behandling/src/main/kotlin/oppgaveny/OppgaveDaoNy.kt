package no.nav.etterlatte.oppgaveny

import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.oppgaveNy.OppgaveNy
import no.nav.etterlatte.libs.common.oppgaveNy.OppgaveType
import no.nav.etterlatte.libs.common.oppgaveNy.RedigerFristRequest
import no.nav.etterlatte.libs.common.oppgaveNy.SaksbehandlerEndringDto
import no.nav.etterlatte.libs.common.oppgaveNy.Status
import no.nav.etterlatte.libs.common.tidspunkt.getTidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.getTidspunktOrNull
import no.nav.etterlatte.libs.common.tidspunkt.setTidspunkt
import no.nav.etterlatte.libs.database.singleOrNull
import no.nav.etterlatte.libs.database.toList
import org.slf4j.LoggerFactory
import java.sql.Connection
import java.sql.ResultSet
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

    fun hentOppgave(oppgaveId: UUID): OppgaveNy? {
        with(connection()) {
            val statement = prepareStatement(
                """
                    SELECT id, status, enhet, sak_id, type, saksbehandler, referanse, merknad, opprettet, saktype, fnr, frist
                    FROM oppgave
                    WHERE id = ?::UUID
                """.trimIndent()
            )
            statement.setObject(1, oppgaveId)
            return statement.executeQuery().singleOrNull {
                asOppgaveNy()
            }
        }
    }

    fun hentOppgaveForBehandling(behandlingid: String): OppgaveNy? {
        with(connection()) {
            val statement = prepareStatement(
                """
                    SELECT id, status, enhet, sak_id, type, saksbehandler, referanse, merknad, opprettet, saktype, fnr, frist
                    FROM oppgave
                    WHERE referanse = ?::UUID
                """.trimIndent()
            )
            statement.setString(1, behandlingid)
            return statement.executeQuery().singleOrNull {
                asOppgaveNy()
            }
        }
    }

    fun settNySaksbehandler(saksbehandlerEndringDto: SaksbehandlerEndringDto) {
        with(connection()) {
            val statement = prepareStatement(
                """
                UPDATE oppgave
                SET saksbehandler = ?
                where id = ?::UUID
                """.trimIndent()
            )

            statement.setString(1, saksbehandlerEndringDto.saksbehandler)
            statement.setObject(2, saksbehandlerEndringDto.oppgaveId)

            statement.executeUpdate()
        }
    }

    fun endreStatusPaaOppgave(oppgaveId: UUID, oppgaveStatus: Status) {
        with(connection()) {
            val statement = prepareStatement(
                """
                UPDATE oppgave
                SET status = ?
                where id = ?::UUID
                """.trimIndent()
            )

            statement.setString(1, oppgaveStatus.toString())
            statement.setObject(2, oppgaveId)

            statement.executeUpdate()
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
                asOppgaveNy()
            }.also {
                logger.info("Hentet antall nye oppgaver: ${it.size}")
            }
        }
    }

    fun fjernSaksbehandler(oppgaveId: UUID) {
        with(connection()) {
            val statement = prepareStatement(
                """
                UPDATE oppgave
                SET saksbehandler = NULL
                where id = ?::UUID
                """.trimIndent()
            )

            statement.setObject(1, oppgaveId)

            statement.executeUpdate()
        }
    }

    fun redigerFrist(redigerFristRequest: RedigerFristRequest) {
        with(connection()) {
            val statement = prepareStatement(
                """
                UPDATE oppgave
                SET frist = ?
                where id = ?::UUID
                """.trimIndent()
            )
            statement.setTidspunkt(1, redigerFristRequest.frist)
            statement.setObject(2, redigerFristRequest.oppgaveId)

            statement.executeUpdate()
        }
    }

    private fun ResultSet.asOppgaveNy(): OppgaveNy {
        return OppgaveNy(
            id = getObject("id") as UUID,
            status = Status.valueOf(getString("status")),
            enhet = getString("enhet"),
            sakId = getLong("sak_id"),
            type = OppgaveType.valueOf(getString("type")),
            saksbehandler = getString("saksbehandler"),
            referanse = getString("referanse"),
            merknad = getString("merknad"),
            opprettet = getTidspunkt("opprettet"),
            sakType = getString("saktype")?.let { SakType.valueOf(it) },
            fnr = getString("fnr"),
            frist = getTidspunktOrNull("frist")
        )
    }
}