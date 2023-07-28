package no.nav.etterlatte.oppgaveny

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.etterlatte.behandling.objectMapper
import no.nav.etterlatte.grunnlagsendring.setJsonb
import no.nav.etterlatte.libs.common.oppgaveNy.OppgaveNy
import no.nav.etterlatte.libs.common.oppgaveNy.OppgaveType
import no.nav.etterlatte.libs.common.oppgaveNy.RedigerFristRequest
import no.nav.etterlatte.libs.common.oppgaveNy.SaksbehandlerEndringDto
import no.nav.etterlatte.libs.common.oppgaveNy.Status
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.getTidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.setTidspunkt
import no.nav.etterlatte.libs.database.toList
import java.sql.Connection
import java.sql.ResultSet
import java.util.*

interface OppgaveDaoMedEndringssporing : OppgaveDaoNy {
    fun hentEndringerForOppgave(oppgaveId: UUID): List<OppgaveEndring>
}

class OppgaveDaoMedEndringssporingImpl(
    private val oppgaveDao: OppgaveDaoNy,
    private val connection: () -> Connection
) : OppgaveDaoMedEndringssporing {

    private fun lagreEndringerPaaOppgave(oppgaveId: UUID, block: () -> Unit) {
        val foer = requireNotNull(hentOppgave(oppgaveId)) {
            "Må ha en oppgave for å kunne endre den"
        }
        block()
        val etter = requireNotNull(hentOppgave(oppgaveId)) {
            "Må ha en oppgave etter endring"
        }
        lagreEndringerPaaOppgave(foer, etter)
    }

    private fun lagreEndringerPaaOppgave(oppgaveFoer: OppgaveNy, oppgaveEtter: OppgaveNy) {
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

    override fun hentEndringerForOppgave(oppgaveId: UUID): List<OppgaveEndring> {
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

    override fun lagreOppgave(oppgaveNy: OppgaveNy) {
        oppgaveDao.lagreOppgave(oppgaveNy)
    }

    override fun hentOppgave(oppgaveId: UUID): OppgaveNy? {
        return oppgaveDao.hentOppgave(oppgaveId)
    }

    override fun hentOppgaverForBehandling(behandlingid: String): List<OppgaveNy> {
        return oppgaveDao.hentOppgaverForBehandling(behandlingid)
    }

    override fun hentOppgaver(oppgaveTypeTyper: List<OppgaveType>): List<OppgaveNy> {
        return oppgaveDao.hentOppgaver(oppgaveTypeTyper)
    }

    override fun finnOppgaverForStrengtFortroligOgStrengtFortroligUtland(
        oppgaveTypeTyper: List<OppgaveType>
    ): List<OppgaveNy> {
        return oppgaveDao.finnOppgaverForStrengtFortroligOgStrengtFortroligUtland(oppgaveTypeTyper)
    }

    override fun settNySaksbehandler(saksbehandlerEndringDto: SaksbehandlerEndringDto) {
        lagreEndringerPaaOppgave(saksbehandlerEndringDto.oppgaveId) {
            oppgaveDao.settNySaksbehandler(saksbehandlerEndringDto)
        }
    }

    override fun endreStatusPaaOppgave(oppgaveId: UUID, oppgaveStatus: Status) {
        lagreEndringerPaaOppgave(oppgaveId) {
            oppgaveDao.endreStatusPaaOppgave(oppgaveId, oppgaveStatus)
        }
    }

    override fun fjernSaksbehandler(oppgaveId: UUID) {
        lagreEndringerPaaOppgave(oppgaveId) {
            oppgaveDao.fjernSaksbehandler(oppgaveId)
        }
    }

    override fun redigerFrist(redigerFristRequest: RedigerFristRequest) {
        lagreEndringerPaaOppgave(redigerFristRequest.oppgaveId) {
            oppgaveDao.redigerFrist(redigerFristRequest)
        }
    }
}