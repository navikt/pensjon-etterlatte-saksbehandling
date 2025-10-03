package no.nav.etterlatte.oppgave

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.behandling.objectMapper
import no.nav.etterlatte.common.ConnectionAutoclosing
import no.nav.etterlatte.libs.common.Enhetsnummer
import no.nav.etterlatte.libs.common.behandling.PaaVentAarsak
import no.nav.etterlatte.libs.common.feilhaandtering.krevIkkeNull
import no.nav.etterlatte.libs.common.oppgave.OppgaveIntern
import no.nav.etterlatte.libs.common.oppgave.OppgaveKilde
import no.nav.etterlatte.libs.common.oppgave.OppgaveType
import no.nav.etterlatte.libs.common.oppgave.OppgavebenkStats
import no.nav.etterlatte.libs.common.oppgave.Status
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.getTidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.setTidspunkt
import no.nav.etterlatte.libs.database.setJsonb
import no.nav.etterlatte.libs.database.toList
import java.sql.ResultSet
import java.time.LocalDate
import java.util.UUID

interface OppgaveDaoMedEndringssporing : OppgaveDao {
    fun hentEndringerForOppgave(oppgaveId: UUID): List<OppgaveEndring>

    fun tilbakestillOppgaveUnderAttestering(oppgaveTilAttestering: OppgaveIntern)
}

class OppgaveDaoMedEndringssporingImpl(
    private val oppgaveDao: OppgaveDao,
    private val connectionAutoclosing: ConnectionAutoclosing,
) : OppgaveDaoMedEndringssporing {
    private fun lagreEndringerPaaOppgave(
        oppgaveId: UUID,
        block: () -> Unit,
    ) {
        val foer =
            krevIkkeNull(hentOppgave(oppgaveId)) {
                "Må ha en oppgave for å kunne endre den"
            }
        block()
        val etter =
            krevIkkeNull(hentOppgave(oppgaveId)) {
                "Må ha en oppgave etter endring"
            }
        lagreEndringerPaaOppgave(foer, etter)
    }

    private fun lagreEndringerPaaOppgave(
        oppgaveFoer: OppgaveIntern,
        oppgaveEtter: OppgaveIntern,
    ) {
        connectionAutoclosing.hentConnection {
            with(it) {
                val statement =
                    prepareStatement(
                        """
                        INSERT INTO oppgaveendringer(id, oppgaveId, oppgaveFoer, oppgaveEtter, tidspunkt, saksbehandler)
                        VALUES(?::UUID, ?::UUID, ?::JSONB, ?::JSONB, ?, ?)
                        """.trimIndent(),
                    )
                statement.setObject(1, UUID.randomUUID())
                statement.setObject(2, oppgaveEtter.id)
                statement.setJsonb(3, oppgaveFoer)
                statement.setJsonb(4, oppgaveEtter)
                statement.setTidspunkt(5, Tidspunkt.now())
                statement.setString(6, Kontekst.get().AppUser.name())

                statement.executeUpdate()
            }
        }
    }

    override fun hentEndringerForOppgave(oppgaveId: UUID): List<OppgaveEndring> =
        connectionAutoclosing.hentConnection {
            with(it) {
                val statement =
                    prepareStatement(
                        """
                        SELECT id, oppgaveId, oppgaveFoer, oppgaveEtter, tidspunkt
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

    private fun ResultSet.asOppgaveEndring(): OppgaveEndring =
        OppgaveEndring(
            id = getObject("id") as UUID,
            oppgaveId = getObject("oppgaveId") as UUID,
            oppgaveFoer = getString("oppgaveFoer").let { objectMapper.readValue(it) },
            oppgaveEtter = getString("oppgaveEtter").let { objectMapper.readValue(it) },
            tidspunkt = getTidspunkt("tidspunkt"),
        )

    override fun opprettOppgave(oppgaveIntern: OppgaveIntern) {
        oppgaveDao.opprettOppgave(oppgaveIntern)
    }

    override fun opprettOppgaveBulk(oppgaveListe: List<OppgaveIntern>) {
        oppgaveDao.opprettOppgaveBulk(oppgaveListe)
    }

    override fun hentOppgave(oppgaveId: UUID): OppgaveIntern? = oppgaveDao.hentOppgave(oppgaveId)

    override fun hentOppgaverForReferanse(referanse: String): List<OppgaveIntern> = oppgaveDao.hentOppgaverForReferanse(referanse)

    override fun hentOppgaverForGruppeId(
        gruppeId: String,
        type: OppgaveType,
    ): List<OppgaveIntern> = oppgaveDao.hentOppgaverForGruppeId(gruppeId, type)

    override fun oppgaveMedTypeFinnes(
        sakId: SakId,
        type: OppgaveType,
    ): Boolean = oppgaveDao.oppgaveMedTypeFinnes(sakId, type)

    override fun hentOppgaverForSakMedType(
        sakId: SakId,
        typer: List<OppgaveType>,
    ): List<OppgaveIntern> =
        oppgaveDao.hentOppgaverForSakMedType(
            sakId,
            typer,
        )

    override fun hentOppgaver(
        enheter: List<Enhetsnummer>,
        oppgaveStatuser: List<String>,
        minOppgavelisteIdentFilter: String?,
    ): List<OppgaveIntern> = oppgaveDao.hentOppgaver(enheter, oppgaveStatuser, minOppgavelisteIdentFilter)

    override fun hentAntallOppgaver(innloggetSaksbehandlerIdent: String): OppgavebenkStats =
        oppgaveDao.hentAntallOppgaver(innloggetSaksbehandlerIdent)

    override fun settNySaksbehandler(
        oppgaveId: UUID,
        saksbehandler: String,
    ) {
        lagreEndringerPaaOppgave(oppgaveId) {
            oppgaveDao.settNySaksbehandler(oppgaveId, saksbehandler)
        }
    }

    override fun endreStatusPaaOppgave(
        oppgaveId: UUID,
        oppgaveStatus: Status,
    ) {
        lagreEndringerPaaOppgave(oppgaveId) {
            oppgaveDao.endreStatusPaaOppgave(oppgaveId, oppgaveStatus)
        }
    }

    override fun endreEnhetPaaOppgave(
        oppgaveId: UUID,
        enhet: Enhetsnummer,
    ) {
        lagreEndringerPaaOppgave(oppgaveId) {
            oppgaveDao.endreEnhetPaaOppgave(oppgaveId, enhet)
        }
    }

    override fun oppdaterIdent(
        oppgaveId: UUID,
        nyttFnr: String,
    ) {
        lagreEndringerPaaOppgave(oppgaveId) {
            oppgaveDao.oppdaterIdent(oppgaveId, nyttFnr)
        }
    }

    override fun fjernSaksbehandler(oppgaveId: UUID) {
        lagreEndringerPaaOppgave(oppgaveId) {
            oppgaveDao.fjernSaksbehandler(oppgaveId)
        }
    }

    override fun settForrigeSaksbehandlerFraSaksbehandler(oppgaveId: UUID) {
        lagreEndringerPaaOppgave(oppgaveId) {
            oppgaveDao.settForrigeSaksbehandlerFraSaksbehandler(oppgaveId)
        }
    }

    override fun fjernForrigeSaksbehandler(oppgaveId: UUID) {
        lagreEndringerPaaOppgave(oppgaveId) {
            oppgaveDao.fjernForrigeSaksbehandler(oppgaveId)
        }
    }

    override fun redigerFrist(
        oppgaveId: UUID,
        frist: Tidspunkt,
    ) {
        lagreEndringerPaaOppgave(oppgaveId) {
            oppgaveDao.redigerFrist(oppgaveId, frist)
        }
    }

    override fun oppdaterStatusOgMerknad(
        oppgaveId: UUID,
        merknad: String,
        oppgaveStatus: Status,
    ) {
        lagreEndringerPaaOppgave(oppgaveId) {
            oppgaveDao.oppdaterStatusOgMerknad(oppgaveId, merknad, oppgaveStatus)
        }
    }

    override fun oppdaterPaaVent(
        oppgaveId: UUID,
        merknad: String,
        aarsak: PaaVentAarsak?,
        oppgaveStatus: Status,
    ) {
        lagreEndringerPaaOppgave(oppgaveId) {
            oppgaveDao.oppdaterPaaVent(oppgaveId, merknad, aarsak, oppgaveStatus)
        }
    }

    override fun oppdaterReferanseOgMerknad(
        oppgaveId: UUID,
        referanse: String,
        merknad: String,
    ) {
        lagreEndringerPaaOppgave(oppgaveId) {
            oppgaveDao.oppdaterReferanseOgMerknad(oppgaveId, referanse, merknad)
        }
    }

    override fun oppdaterMerknad(
        oppgaveId: UUID,
        merknad: String,
    ) {
        lagreEndringerPaaOppgave(oppgaveId) {
            oppgaveDao.oppdaterMerknad(oppgaveId, merknad)
        }
    }

    override fun endreTilKildeBehandlingOgOppdaterReferanse(
        oppgaveId: UUID,
        referanse: String,
    ) {
        lagreEndringerPaaOppgave(oppgaveId) {
            oppgaveDao.endreTilKildeBehandlingOgOppdaterReferanse(oppgaveId, referanse)
        }
    }

    override fun hentFristGaarUt(
        dato: LocalDate,
        type: Collection<OppgaveType>,
        kilde: Collection<OppgaveKilde>,
        oppgaver: List<UUID>,
        grense: Int,
    ) = oppgaveDao.hentFristGaarUt(dato, type, kilde, oppgaver, grense)

    override fun hentOppgaverTilSaker(
        saker: List<SakId>,
        oppgaveStatuser: List<String>,
    ) = oppgaveDao.hentOppgaverTilSaker(saker, oppgaveStatuser)

    override fun tilbakestillOppgaveUnderAttestering(oppgaveTilAttestering: OppgaveIntern) =
        lagreEndringerPaaOppgave(oppgaveTilAttestering.id) {
            oppgaveDao.oppdaterStatusOgMerknad(
                oppgaveId = oppgaveTilAttestering.id,
                merknad = "G-regulering - saken returnert for å få med nytt grunnbeløp",
                oppgaveStatus = Status.UNDER_BEHANDLING,
            )
        }
}
