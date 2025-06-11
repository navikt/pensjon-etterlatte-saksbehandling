package no.nav.etterlatte.oppgave.kommentar

import no.nav.etterlatte.common.ConnectionAutoclosing
import no.nav.etterlatte.libs.common.oppgave.OppgaveKommentarDto
import no.nav.etterlatte.libs.common.oppgave.OppgaveSaksbehandler
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.tidspunkt.getTidspunkt
import no.nav.etterlatte.libs.database.setSakId
import no.nav.etterlatte.libs.database.toList
import org.slf4j.LoggerFactory
import java.sql.ResultSet
import java.util.UUID

interface OppgaveKommentarDao {
    fun opprettKommentar(oppgaveKommentarDto: OppgaveKommentarDto)

    fun hentKommentarer(oppgaveId: UUID): List<OppgaveKommentarDto>
}

class OppgaveKommentarDaoImpl(
    private val connectionAutoclosing: ConnectionAutoclosing,
) : OppgaveKommentarDao {
    private val logger = LoggerFactory.getLogger(this::class.java)

    override fun opprettKommentar(oppgaveKommentarDto: OppgaveKommentarDto) {
        connectionAutoclosing.hentConnection {
            with(it) {
                val statement =
                    prepareStatement(
                        """
                        INSERT INTO oppgave_kommentar(id, sak_id, oppgave_id, kommentar, saksbehandler)
                        VALUES(?::UUID, ?, ?, ?, ?)
                        """.trimIndent(),
                    )
                statement.setObject(1, oppgaveKommentarDto.id)
                statement.setSakId(2, oppgaveKommentarDto.sakId)
                statement.setObject(3, oppgaveKommentarDto.oppgaveId)
                statement.setString(4, oppgaveKommentarDto.kommentar)
                statement.setString(5, oppgaveKommentarDto.saksbehandler.ident)
                statement.executeUpdate()
                logger.info("oppretter ny kommentar for oppgave=${oppgaveKommentarDto.oppgaveId} sakId=${oppgaveKommentarDto.sakId}")
            }
        }
    }

    override fun hentKommentarer(oppgaveId: UUID): List<OppgaveKommentarDto> =
        connectionAutoclosing.hentConnection {
            with(it) {
                val statement =
                    prepareStatement(
                        """
                        SELECT id, sak_id, oppgave_id, kommentar, saksbehandler, tidspunkt
                        FROM oppgave_kommentar 
                        WHERE oppgave_id = ?
                        """.trimIndent(),
                    )
                statement.setObject(1, oppgaveId)
                statement
                    .executeQuery()
                    .toList {
                        asOppgaveKommentar()
                    }.also { kommentarer ->
                        logger.info("Hentet ${kommentarer.size} kommentar(er) for oppgaveId=$oppgaveId")
                    }
            }
        }

    private fun ResultSet.asOppgaveKommentar(): OppgaveKommentarDto =
        OppgaveKommentarDto(
            id = getObject("id") as UUID,
            sakId = SakId(getLong("sak_id")),
            oppgaveId = getObject("oppgave_id") as UUID,
            kommentar = getString("kommentar"),
            saksbehandler = OppgaveSaksbehandler(getString("saksbehandler")),
            tidspunkt = getTidspunkt("tidspunkt"),
        )
}
