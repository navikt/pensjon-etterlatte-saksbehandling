package no.nav.etterlatte.oppgave

import no.nav.etterlatte.common.ConnectionAutoclosing
import no.nav.etterlatte.libs.common.oppgave.OppgaveIntern
import no.nav.etterlatte.libs.common.oppgave.OppgaveKilde
import no.nav.etterlatte.libs.common.oppgave.OppgaveType
import no.nav.etterlatte.libs.common.oppgave.OppgavebenkStats
import no.nav.etterlatte.libs.common.oppgave.Status
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.getTidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.setTidspunkt
import no.nav.etterlatte.libs.database.toList
import no.nav.etterlatte.oppgave.EndringType.ENDRET_ENHET
import no.nav.etterlatte.oppgave.EndringType.ENDRET_FRIST
import no.nav.etterlatte.oppgave.EndringType.ENDRET_KILDE
import no.nav.etterlatte.oppgave.EndringType.ENDRET_STATUS
import no.nav.etterlatte.oppgave.EndringType.ENDRET_STATUS_OG_MERKNAD
import no.nav.etterlatte.oppgave.EndringType.ENDRET_TILDELING
import no.nav.etterlatte.oppgave.EndringType.OPPRETTET_OPPGAVE
import java.sql.ResultSet
import java.time.LocalDate
import java.util.UUID

interface OppgaveDaoMedEndringssporing : OppgaveDao {
    fun hentEndringerForOppgave(oppgaveId: UUID): List<OppgaveEndring>
}

class OppgaveDaoMedEndringssporingImpl(
    private val oppgaveDao: OppgaveDao,
    private val connectionAutoclosing: ConnectionAutoclosing,
) : OppgaveDaoMedEndringssporing {
    private fun lagreEndringerPaaOppgave(
        oppgaveId: UUID,
        endringType: EndringType,
        block: () -> Unit,
    ) {
        block()

        val endretOppgave =
            requireNotNull(hentOppgave(oppgaveId)) {
                "Må ha en oppgave etter endring"
            }
        lagreEndringerPaaOppgave(endretOppgave, endringType)
    }

    private fun lagreEndringerPaaOppgave(
        oppgave: OppgaveIntern,
        type: EndringType,
    ) {
        connectionAutoclosing.hentConnection {
            with(it) {
                val statement =
                    prepareStatement(
                        """
                        INSERT INTO oppgaveendringer(id, oppgaveId, tidspunkt, saksbehandler, status, merknad, enhet, kilde, type)
                        VALUES(?::UUID, ?::UUID, ?, ?, ?, ?, ?, ?, ?)
                        """.trimIndent(),
                    )
                statement.setObject(1, UUID.randomUUID())
                statement.setObject(2, oppgave.id)
                statement.setTidspunkt(3, Tidspunkt.now())
                statement.setString(4, oppgave.saksbehandler?.ident)
                statement.setString(5, oppgave.status.name)
                statement.setString(6, oppgave.merknad)
                statement.setString(7, oppgave.enhet)
                statement.setString(8, oppgave.kilde?.name)
                statement.setString(9, type.name)

                statement.executeUpdate()
            }
        }
    }

    override fun hentEndringerForOppgave(oppgaveId: UUID): List<OppgaveEndring> {
        return connectionAutoclosing.hentConnection {
            with(it) {
                val statement =
                    prepareStatement(
                        """
                        SELECT id, oppgaveId, tidspunkt, saksbehandler, status, merknad, enhet, kilde, type
                        FROM oppgaveendringer
                        where oppgaveId = ?::UUID
                        """.trimIndent(),
                    )
                statement.setObject(1, oppgaveId)

                statement.executeQuery().toList {
                    asOppgaveEndring()
                }
            }
        }
    }

    private fun ResultSet.asOppgaveEndring(): OppgaveEndring {
        return OppgaveEndring(
            id = getObject("id") as UUID,
            oppgaveId = getObject("id") as UUID,
            tidspunkt = getTidspunkt("tidspunkt"),
            saksbehandler = getString("saksbehandler"),
            status = Status.valueOf(getString("status")),
            merknad = getString("merknad"),
            enhet = getString("enhet"),
            kilde = getString("kilde"),
            type = EndringType.valueOf(getString("type")),
        )
    }

    override fun opprettOppgave(oppgaveIntern: OppgaveIntern) {
        lagreEndringerPaaOppgave(oppgaveIntern.id, OPPRETTET_OPPGAVE) {
            oppgaveDao.opprettOppgave(oppgaveIntern)
        }
    }

    override fun hentOppgave(oppgaveId: UUID): OppgaveIntern? {
        return oppgaveDao.hentOppgave(oppgaveId)
    }

    override fun hentOppgaverForReferanse(referanse: String): List<OppgaveIntern> {
        return oppgaveDao.hentOppgaverForReferanse(referanse)
    }

    override fun hentOppgaverForSak(sakId: Long): List<OppgaveIntern> {
        return oppgaveDao.hentOppgaverForSak(sakId)
    }

    override fun hentOppgaver(
        oppgaveTyper: List<OppgaveType>,
        enheter: List<String>,
        oppgaveStatuser: List<String>,
        minOppgavelisteIdentFilter: String?,
    ): List<OppgaveIntern> {
        return oppgaveDao.hentOppgaver(oppgaveTyper, enheter, oppgaveStatuser, minOppgavelisteIdentFilter)
    }

    override fun hentAntallOppgaver(innloggetSaksbehandlerIdent: String): OppgavebenkStats {
        return oppgaveDao.hentAntallOppgaver(innloggetSaksbehandlerIdent)
    }

    override fun finnOppgaverForStrengtFortroligOgStrengtFortroligUtland(oppgaveTypeTyper: List<OppgaveType>): List<OppgaveIntern> {
        return oppgaveDao.finnOppgaverForStrengtFortroligOgStrengtFortroligUtland(oppgaveTypeTyper)
    }

    override fun settNySaksbehandler(
        oppgaveId: UUID,
        saksbehandler: String,
    ) {
        lagreEndringerPaaOppgave(oppgaveId, ENDRET_TILDELING) {
            oppgaveDao.settNySaksbehandler(oppgaveId, saksbehandler)
        }
    }

    override fun endreStatusPaaOppgave(
        oppgaveId: UUID,
        oppgaveStatus: Status,
    ) {
        lagreEndringerPaaOppgave(oppgaveId, ENDRET_STATUS) {
            oppgaveDao.endreStatusPaaOppgave(oppgaveId, oppgaveStatus)
        }
    }

    override fun endreEnhetPaaOppgave(
        oppgaveId: UUID,
        enhet: String,
    ) {
        lagreEndringerPaaOppgave(oppgaveId, ENDRET_ENHET) {
            oppgaveDao.endreEnhetPaaOppgave(oppgaveId, enhet)
        }
    }

    override fun fjernSaksbehandler(oppgaveId: UUID) {
        lagreEndringerPaaOppgave(oppgaveId, ENDRET_TILDELING) {
            oppgaveDao.fjernSaksbehandler(oppgaveId)
        }
    }

    override fun redigerFrist(
        oppgaveId: UUID,
        frist: Tidspunkt,
    ) {
        lagreEndringerPaaOppgave(oppgaveId, ENDRET_FRIST) {
            oppgaveDao.redigerFrist(oppgaveId, frist)
        }
    }

    override fun oppdaterStatusOgMerknad(
        oppgaveId: UUID,
        merknad: String,
        oppgaveStatus: Status,
    ) {
        lagreEndringerPaaOppgave(oppgaveId, ENDRET_STATUS_OG_MERKNAD) {
            oppgaveDao.oppdaterStatusOgMerknad(oppgaveId, merknad, oppgaveStatus)
        }
    }

    override fun endreTilKildeBehandlingOgOppdaterReferanse(
        oppgaveId: UUID,
        referanse: String,
    ) {
        lagreEndringerPaaOppgave(oppgaveId, ENDRET_KILDE) {
            oppgaveDao.endreTilKildeBehandlingOgOppdaterReferanse(oppgaveId, referanse)
        }
    }

    override fun hentFristGaarUt(
        dato: LocalDate,
        type: Collection<OppgaveType>,
        kilde: Collection<OppgaveKilde>,
        oppgaver: List<UUID>,
    ) = oppgaveDao.hentFristGaarUt(dato, type, kilde, oppgaver)
}
