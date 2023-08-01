package no.nav.etterlatte.oppgaveny

import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.oppgaveNy.OppgaveKilde
import no.nav.etterlatte.libs.common.oppgaveNy.OppgaveNy
import no.nav.etterlatte.libs.common.oppgaveNy.OppgaveType
import no.nav.etterlatte.libs.common.oppgaveNy.Status
import no.nav.etterlatte.libs.common.person.AdressebeskyttelseGradering
import no.nav.etterlatte.libs.common.tidspunkt.getTidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.getTidspunktOrNull
import no.nav.etterlatte.libs.common.tidspunkt.setTidspunkt
import no.nav.etterlatte.libs.database.singleOrNull
import no.nav.etterlatte.libs.database.toList
import org.slf4j.LoggerFactory
import java.sql.Connection
import java.sql.ResultSet
import java.util.*

interface OppgaveDaoNy {

    fun lagreOppgave(oppgaveNy: OppgaveNy)
    fun hentOppgave(oppgaveId: UUID): OppgaveNy?
    fun hentOppgaverForBehandling(behandlingid: String): List<OppgaveNy>
    fun hentOppgaver(oppgaveTypeTyper: List<OppgaveType>): List<OppgaveNy>
    fun finnOppgaverForStrengtFortroligOgStrengtFortroligUtland(oppgaveTypeTyper: List<OppgaveType>): List<OppgaveNy>
}

class OppgaveDaoNyImpl(private val connection: () -> Connection) : OppgaveDaoNy {
    private val logger = LoggerFactory.getLogger(this::class.java)

    override fun lagreOppgave(oppgaveNy: OppgaveNy) {
        with(connection()) {
            val statement = prepareStatement(
                """
                INSERT INTO oppgave(id, status, enhet, sak_id, type, saksbehandler, referanse, merknad, opprettet, saktype, fnr, frist, kilde)
                VALUES(?::UUID, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (id) DO UPDATE 
                    SET status = excluded.status, enhet = excluded.enhet, sak_id = excluded.sak_id, type = excluded.type,
                     saksbehandler = excluded.saksbehandler, referanse = excluded.referanse, merknad = excluded.merknad, 
                     saktype = excluded.saktype, fnr = excluded.fnr, frist = excluded.frist, kilde = excluded.kilde
                """.trimIndent()
            )
            statement.setObject(1, oppgaveNy.id)
            statement.setString(2, oppgaveNy.status.name)
            statement.setString(3, oppgaveNy.enhet)
            statement.setLong(4, oppgaveNy.sakId)
            statement.setString(5, oppgaveNy.type.name)
            statement.setString(6, oppgaveNy.saksbehandler)
            statement.setString(7, oppgaveNy.referanse)
            statement.setString(8, oppgaveNy.merknad)
            statement.setTidspunkt(9, oppgaveNy.opprettet)
            statement.setString(10, oppgaveNy.sakType.name)
            statement.setString(11, oppgaveNy.fnr)
            statement.setTidspunkt(12, oppgaveNy.frist)
            statement.setString(13, oppgaveNy.kilde?.name)
            statement.executeUpdate()
            logger.info("lagret oppgave for ${oppgaveNy.id} for sakid ${oppgaveNy.sakId}")
        }
    }

    override fun hentOppgave(oppgaveId: UUID): OppgaveNy? {
        with(connection()) {
            val statement = prepareStatement(
                """
                    SELECT id, status, enhet, sak_id, type, saksbehandler, referanse, merknad, opprettet, saktype, fnr, frist, kilde
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

    override fun hentOppgaverForBehandling(behandlingid: String): List<OppgaveNy> {
        with(connection()) {
            val statement = prepareStatement(
                """
                    SELECT id, status, enhet, sak_id, type, saksbehandler, referanse, merknad, opprettet, saktype, fnr, frist, kilde
                    FROM oppgave
                    WHERE referanse = ?
                """.trimIndent()
            )
            statement.setString(1, behandlingid)
            return statement.executeQuery().toList {
                asOppgaveNy()
            }.also {
                logger.info("Hentet antall nye oppgaver for behandling: ${it.size} behandling: $behandlingid")
            }
        }
    }

    override fun hentOppgaver(oppgaveTypeTyper: List<OppgaveType>): List<OppgaveNy> {
        if (oppgaveTypeTyper.isEmpty()) return emptyList()

        with(connection()) {
            val statement = prepareStatement(
                """
                    SELECT o.id, o.status, o.enhet, o.sak_id, o.type, o.saksbehandler, o.referanse, o.merknad, o.opprettet, o.saktype, o.fnr, o.frist, o.kilde
                    FROM oppgave o INNER JOIN sak s ON o.sak_id = s.id
                    WHERE o.type = ANY(?)
                    AND s.adressebeskyttelse is null OR 
                    (s.adressebeskyttelse is NOT NULL AND (s.adressebeskyttelse != ? AND s.adressebeskyttelse != ?))
                """.trimIndent()
            )
            statement.setArray(1, createArrayOf("text", oppgaveTypeTyper.toTypedArray()))
            statement.setString(2, AdressebeskyttelseGradering.STRENGT_FORTROLIG.name)
            statement.setString(3, AdressebeskyttelseGradering.STRENGT_FORTROLIG_UTLAND.name)

            return statement.executeQuery().toList {
                asOppgaveNy()
            }.also {
                logger.info("Hentet antall nye oppgaver: ${it.size}")
            }
        }
    }

    override fun finnOppgaverForStrengtFortroligOgStrengtFortroligUtland(
        oppgaveTypeTyper: List<OppgaveType>
    ): List<OppgaveNy> {
        with(connection()) {
            val statement = prepareStatement(
                """
                    SELECT o.id, o.status, o.enhet, o.sak_id, o.type, o.saksbehandler, o.referanse, o.merknad, o.opprettet, o.saktype, o.fnr, o.frist, o.kilde
                    FROM oppgave o INNER JOIN sak s ON o.sak_id = s.id
                    WHERE ((s.adressebeskyttelse = ?) OR (s.adressebeskyttelse = ?))
                    AND o.type = ANY(?)
                """.trimIndent()
            )
            statement.setString(1, AdressebeskyttelseGradering.STRENGT_FORTROLIG.name)
            statement.setString(2, AdressebeskyttelseGradering.STRENGT_FORTROLIG_UTLAND.name)
            statement.setArray(3, createArrayOf("text", oppgaveTypeTyper.toTypedArray()))

            return statement.executeQuery().toList {
                asOppgaveNy()
            }.also {
                logger.info("Hentet antall nye oppgaver: ${it.size}")
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