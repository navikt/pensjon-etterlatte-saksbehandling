package no.nav.etterlatte.oppgave

import no.nav.etterlatte.behandling.hendelse.getUUID
import no.nav.etterlatte.common.ConnectionAutoclosing
import no.nav.etterlatte.libs.common.Enhetsnummer
import no.nav.etterlatte.libs.common.behandling.PaaVentAarsak
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.oppgave.OppgaveIntern
import no.nav.etterlatte.libs.common.oppgave.OppgaveKilde
import no.nav.etterlatte.libs.common.oppgave.OppgaveSaksbehandler
import no.nav.etterlatte.libs.common.oppgave.OppgaveType
import no.nav.etterlatte.libs.common.oppgave.OppgavebenkStats
import no.nav.etterlatte.libs.common.oppgave.Status
import no.nav.etterlatte.libs.common.oppgave.VentefristGaarUt
import no.nav.etterlatte.libs.common.person.maskerFnr
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.getTidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.getTidspunktOrNull
import no.nav.etterlatte.libs.common.tidspunkt.setTidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.toTidspunkt
import no.nav.etterlatte.libs.database.setSakId
import no.nav.etterlatte.libs.database.single
import no.nav.etterlatte.libs.database.singleOrNull
import no.nav.etterlatte.libs.database.toList
import org.slf4j.LoggerFactory
import java.sql.ResultSet
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

interface OppgaveDao {
    fun opprettOppgave(oppgaveIntern: OppgaveIntern)

    fun opprettOppgaveBulk(oppgaveListe: List<OppgaveIntern>)

    fun hentOppgave(oppgaveId: UUID): OppgaveIntern?

    fun hentOppgaverForReferanse(referanse: String): List<OppgaveIntern>

    fun hentOppgaverForGruppeId(
        gruppeId: String,
        type: OppgaveType,
    ): List<OppgaveIntern>

    fun oppgaveMedTypeFinnes(
        sakId: SakId,
        type: OppgaveType,
    ): Boolean

    fun hentOppgaverForSakMedType(
        sakId: SakId,
        typer: List<OppgaveType>,
    ): List<OppgaveIntern>

    fun hentOppgaver(
        enheter: List<Enhetsnummer>,
        oppgaveStatuser: List<String>,
        minOppgavelisteIdentFilter: String? = null,
    ): List<OppgaveIntern>

    fun hentAntallOppgaver(innloggetSaksbehandlerIdent: String): OppgavebenkStats

    fun settNySaksbehandler(
        oppgaveId: UUID,
        saksbehandler: String,
    )

    fun endreStatusPaaOppgave(
        oppgaveId: UUID,
        oppgaveStatus: Status,
    )

    fun endreEnhetPaaOppgave(
        oppgaveId: UUID,
        enhet: Enhetsnummer,
    )

    fun oppdaterIdent(
        oppgaveId: UUID,
        nyttFnr: String,
    )

    fun fjernSaksbehandler(oppgaveId: UUID)

    fun settForrigeSaksbehandlerFraSaksbehandler(oppgaveId: UUID)

    fun fjernForrigeSaksbehandler(oppgaveId: UUID)

    fun redigerFrist(
        oppgaveId: UUID,
        frist: Tidspunkt,
    )

    fun oppdaterStatusOgMerknad(
        oppgaveId: UUID,
        merknad: String,
        oppgaveStatus: Status,
    )

    fun oppdaterPaaVent(
        oppgaveId: UUID,
        merknad: String,
        aarsak: PaaVentAarsak?,
        oppgaveStatus: Status,
    )

    fun oppdaterReferanseOgMerknad(
        oppgaveId: UUID,
        referanse: String,
        merknad: String,
    )

    fun oppdaterMerknad(
        oppgaveId: UUID,
        merknad: String,
    )

    fun endreTilKildeBehandlingOgOppdaterReferanse(
        oppgaveId: UUID,
        referanse: String,
    )

    fun hentFristGaarUt(
        dato: LocalDate,
        type: Collection<OppgaveType>,
        kilde: Collection<OppgaveKilde>,
        oppgaver: List<UUID>,
        grense: Int,
    ): List<VentefristGaarUt>

    fun hentOppgaverTilSaker(
        saker: List<SakId>,
        oppgaveStatuser: List<String>,
    ): List<OppgaveIntern>
}

class OppgaveDaoImpl(
    private val connectionAutoclosing: ConnectionAutoclosing,
) : OppgaveDao {
    private val logger = LoggerFactory.getLogger(this::class.java)

    override fun opprettOppgave(oppgaveIntern: OppgaveIntern) {
        connectionAutoclosing.hentConnection {
            with(it) {
                val statement =
                    prepareStatement(
                        """
                        INSERT INTO oppgave(id, status, enhet, sak_id, type, saksbehandler, referanse, gruppe_id, merknad, opprettet, saktype, fnr, frist, kilde)
                        VALUES(?::UUID, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """.trimIndent(),
                    )
                statement.setObject(1, oppgaveIntern.id)
                statement.setString(2, oppgaveIntern.status.name)
                statement.setString(3, oppgaveIntern.enhet.enhetNr)
                statement.setSakId(4, oppgaveIntern.sakId)
                statement.setString(5, oppgaveIntern.type.name)
                statement.setString(6, oppgaveIntern.saksbehandler?.ident)
                statement.setString(7, oppgaveIntern.referanse)
                statement.setString(8, oppgaveIntern.gruppeId)
                statement.setString(9, oppgaveIntern.merknad)
                statement.setTidspunkt(10, oppgaveIntern.opprettet)
                statement.setString(11, oppgaveIntern.sakType.name)
                statement.setString(12, oppgaveIntern.fnr)
                statement.setTidspunkt(13, oppgaveIntern.frist)
                statement.setString(14, oppgaveIntern.kilde?.name)
                statement.executeUpdate()
                logger.info("lagret oppgave for ${oppgaveIntern.id} for sakid ${oppgaveIntern.sakId}")
            }
        }
    }

    override fun opprettOppgaveBulk(oppgaveListe: List<OppgaveIntern>) {
        connectionAutoclosing.hentConnection { connection ->
            with(connection) {
                val statement =
                    prepareStatement(
                        """
                        INSERT INTO oppgave(id, status, enhet, sak_id, type, saksbehandler, referanse, gruppe_id, merknad, opprettet, saktype, fnr, frist, kilde)
                        VALUES(?::UUID, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """.trimIndent(),
                    )

                oppgaveListe.forEach { oppgaveIntern ->
                    statement.setObject(1, oppgaveIntern.id)
                    statement.setString(2, oppgaveIntern.status.name)
                    statement.setString(3, oppgaveIntern.enhet.enhetNr)
                    statement.setSakId(4, oppgaveIntern.sakId)
                    statement.setString(5, oppgaveIntern.type.name)
                    statement.setString(6, oppgaveIntern.saksbehandler?.ident)
                    statement.setString(7, oppgaveIntern.referanse)
                    statement.setString(8, oppgaveIntern.gruppeId)
                    statement.setString(9, oppgaveIntern.merknad)
                    statement.setTidspunkt(10, oppgaveIntern.opprettet)
                    statement.setString(11, oppgaveIntern.sakType.name)
                    statement.setString(12, oppgaveIntern.fnr)
                    statement.setTidspunkt(13, oppgaveIntern.frist)
                    statement.setString(14, oppgaveIntern.kilde?.name)

                    statement.addBatch()
                }

                statement.executeBatch()
            }
        }
    }

    override fun hentOppgave(oppgaveId: UUID): OppgaveIntern? =
        connectionAutoclosing.hentConnection {
            with(it) {
                val statement =
                    prepareStatement(
                        """
                        SELECT o.id, o.status, o.enhet, o.sak_id, o.type, o.saksbehandler, o.referanse, o.gruppe_id,
                            o.merknad, o.opprettet, o.saktype, o.fnr, o.frist, o.kilde, o.forrige_saksbehandler, si.navn
                        FROM oppgave o 
                            LEFT JOIN saksbehandler_info si ON o.saksbehandler = si.id
                        WHERE o.id = ?::UUID
                        """.trimIndent(),
                    )
                statement.setObject(1, oppgaveId)
                statement.executeQuery().singleOrNull {
                    asOppgave()
                }
            }
        }

    override fun hentOppgaverForReferanse(referanse: String): List<OppgaveIntern> =
        connectionAutoclosing.hentConnection {
            with(it) {
                val statement =
                    prepareStatement(
                        """
                        SELECT o.id, o.status, o.enhet, o.sak_id, o.type, o.saksbehandler, o.referanse, o.gruppe_id, 
                            o.merknad, o.opprettet, o.saktype, o.fnr, o.frist, o.kilde, o.forrige_saksbehandler, si.navn
                        FROM oppgave o 
                            LEFT JOIN saksbehandler_info si ON o.saksbehandler = si.id
                        WHERE o.referanse = ?
                        """.trimIndent(),
                    )
                statement.setString(1, referanse)
                statement
                    .executeQuery()
                    .toList {
                        asOppgave()
                    }.also { oppgaver ->
                        logger.info("Hentet ${oppgaver.size} oppgave(r) for referanse: $referanse")
                    }
            }
        }

    override fun hentOppgaverForGruppeId(
        gruppeId: String,
        type: OppgaveType,
    ): List<OppgaveIntern> =
        connectionAutoclosing.hentConnection {
            with(it) {
                val statement =
                    prepareStatement(
                        """
                        SELECT o.id, o.status, o.enhet, o.sak_id, o.type, o.saksbehandler, o.referanse, o.gruppe_id, 
                            o.merknad, o.opprettet, o.saktype, o.fnr, o.frist, o.kilde, o.forrige_saksbehandler, si.navn
                        FROM oppgave o 
                            LEFT JOIN saksbehandler_info si ON o.saksbehandler = si.id
                        WHERE o.gruppe_id = ?
                            AND o.type = ?
                            AND NOT (o.status = ANY(?))
                        """.trimIndent(),
                    )
                statement.setString(1, gruppeId)
                statement.setString(2, type.name)
                statement.setArray(
                    3,
                    createArrayOf("text", listOf(Status.FERDIGSTILT, Status.FEILREGISTRERT, Status.AVBRUTT).toTypedArray()),
                )
                statement
                    .executeQuery()
                    .toList {
                        asOppgave()
                    }.also { oppgaver ->
                        // Bruker [String.maskerFnr] i tilfelle det grupperes på et fnr.
                        logger.info("Hentet ${oppgaver.size} oppgave(r) for gruppeId: ${gruppeId.maskerFnr()}")
                    }
            }
        }

    override fun oppgaveMedTypeFinnes(
        sakId: SakId,
        type: OppgaveType,
    ): Boolean {
        return connectionAutoclosing.hentConnection {
            with(it) {
                val statement =
                    prepareStatement(
                        """
                        SELECT EXISTS(
                            SELECT id
                            FROM oppgave
                            WHERE sak_id = ? AND type = ?
                        )
                        """.trimIndent(),
                    )
                statement.setLong(1, sakId.sakId)
                statement.setString(2, type.name)
                statement.executeQuery().single {
                    val trueOrFalsePostgresFormat = getString("exists")
                    return@single trueOrFalsePostgresFormat == "t"
                }
            }
        }
    }

    override fun hentOppgaverForSakMedType(
        sakId: SakId,
        typer: List<OppgaveType>,
    ): List<OppgaveIntern> =
        connectionAutoclosing.hentConnection {
            with(it) {
                val statement =
                    prepareStatement(
                        """
                        SELECT o.id, o.status, o.enhet, o.sak_id, o.type, o.saksbehandler, o.referanse, o.gruppe_id, 
                            o.merknad, o.opprettet, o.saktype, o.fnr, o.frist, o.kilde, o.forrige_saksbehandler, si.navn
                        FROM oppgave o 
                            LEFT JOIN saksbehandler_info si ON o.saksbehandler = si.id
                        WHERE o.sak_id = ? and o.type = ANY(?)
                        """.trimIndent(),
                    )
                statement.setSakId(1, sakId)
                statement.setArray(2, createArrayOf("text", typer.map { t -> t.name }.toTypedArray()))
                statement
                    .executeQuery()
                    .toList {
                        asOppgave()
                    }.also { oppgaveliste ->
                        logger.info("Hentet antall nye oppgaver for sak: ${oppgaveliste.size} sak: $sakId")
                    }
            }
        }

    override fun hentOppgaver(
        enheter: List<Enhetsnummer>,
        oppgaveStatuser: List<String>,
        minOppgavelisteIdentFilter: String?,
    ): List<OppgaveIntern> =
        connectionAutoclosing.hentConnection {
            with(it) {
                val statement =
                    prepareStatement(
                        """
                        SELECT o.id, o.status, o.enhet, o.sak_id, o.type, o.saksbehandler, o.referanse, o.gruppe_id, 
                            o.merknad, o.opprettet, o.saktype, o.fnr, o.frist, o.kilde, o.forrige_saksbehandler, si.navn
                        FROM oppgave o 
                            INNER JOIN sak s ON o.sak_id = s.id 
                            LEFT JOIN saksbehandler_info si ON o.saksbehandler = si.id
                        WHERE (? OR o.status = ANY(?))
                        AND o.enhet = ANY(?)
                        AND (? OR o.saksbehandler = ?)
                        """.trimIndent(),
                    )

                statement.setBoolean(1, oppgaveStatuser.isEmpty() || oppgaveStatuser.contains(VISALLE))
                statement.setArray(2, createArrayOf("text", oppgaveStatuser.toTypedArray()))
                statement.setArray(3, createArrayOf("text", enheter.map { it.enhetNr }.toTypedArray()))
                statement.setBoolean(4, minOppgavelisteIdentFilter == null)
                statement.setString(5, minOppgavelisteIdentFilter)

                statement
                    .executeQuery()
                    .toList {
                        asOppgave()
                    }.also { oppgaveliste ->
                        logger.info("Hentet antall nye oppgaver: ${oppgaveliste.size}")
                    }
            }
        }

    override fun hentOppgaverTilSaker(
        saker: List<SakId>,
        oppgaveStatuser: List<String>,
    ): List<OppgaveIntern> =
        connectionAutoclosing.hentConnection {
            with(it) {
                val statement =
                    prepareStatement(
                        """
                        SELECT o.id, o.status, o.enhet, o.sak_id, o.type, o.saksbehandler, o.referanse, o.gruppe_id, 
                            o.merknad, o.opprettet, o.saktype, o.fnr, o.frist, o.kilde, o.forrige_saksbehandler, si.navn
                        FROM oppgave o 
                            INNER JOIN sak s ON o.sak_id = s.id 
                            LEFT JOIN saksbehandler_info si ON o.saksbehandler = si.id
                        WHERE (? OR o.status = ANY(?))
                        AND o.sak_id = ANY(?)
                        """.trimIndent(),
                    )

                statement.setBoolean(1, oppgaveStatuser.isEmpty() || oppgaveStatuser.contains(VISALLE))
                statement.setArray(2, createArrayOf("text", oppgaveStatuser.toTypedArray()))
                statement.setArray(3, createArrayOf("bigint", saker.toTypedArray()))

                statement
                    .executeQuery()
                    .toList {
                        asOppgave()
                    }.also { oppgaveliste ->
                        logger.info("Hentet antall nye oppgaver: ${oppgaveliste.size}")
                    }
            }
        }

    override fun hentAntallOppgaver(innloggetSaksbehandlerIdent: String): OppgavebenkStats =
        connectionAutoclosing.hentConnection {
            with(it) {
                val statement =
                    prepareStatement(
                        """
                        SELECT 
                            COUNT(*) FILTER (WHERE status IN ('NY', 'UNDER_BEHANDLING', 'PAA_VENT', 'ATTESTERING', 'UNDERKJENT')) AS "antallOppgavelistaOppgaver",
                            COUNT(*) FILTER (WHERE saksbehandler = ? AND status IN ('NY', 'UNDER_BEHANDLING', 'PAA_VENT', 'ATTESTERING', 'UNDERKJENT') ) AS "antallMinOppgavelisteOppgaver"
                        FROM oppgave
                        """.trimIndent(),
                    )

                statement.setString(1, innloggetSaksbehandlerIdent)

                statement
                    .executeQuery()
                    .singleOrNull {
                        OppgavebenkStats(
                            getLong("antallOppgavelistaOppgaver"),
                            getLong("antallMinOppgavelisteOppgaver"),
                        )
                    }!!
                    .also {
                        logger.info("Henter antall oppgaver")
                    }
            }
        }

    override fun settNySaksbehandler(
        oppgaveId: UUID,
        saksbehandler: String,
    ) {
        connectionAutoclosing.hentConnection {
            with(it) {
                val statement =
                    prepareStatement(
                        """
                        UPDATE oppgave
                        SET saksbehandler = ?
                        where id = ?::UUID
                        """.trimIndent(),
                    )

                statement.setString(1, saksbehandler)
                statement.setObject(2, oppgaveId)

                statement.executeUpdate()
            }
        }
    }

    override fun endreStatusPaaOppgave(
        oppgaveId: UUID,
        oppgaveStatus: Status,
    ) {
        connectionAutoclosing.hentConnection {
            with(it) {
                val statement =
                    prepareStatement(
                        """
                        UPDATE oppgave
                        SET status = ?
                        where id = ?::UUID
                        """.trimIndent(),
                    )

                statement.setString(1, oppgaveStatus.name)
                statement.setObject(2, oppgaveId)

                statement.executeUpdate()
            }
        }
    }

    override fun endreEnhetPaaOppgave(
        oppgaveId: UUID,
        enhet: Enhetsnummer,
    ) {
        connectionAutoclosing.hentConnection {
            with(it) {
                val statement =
                    prepareStatement(
                        """
                        UPDATE oppgave
                        SET enhet = ?
                        where id = ?::UUID
                        """.trimIndent(),
                    )

                statement.setString(1, enhet.enhetNr)
                statement.setObject(2, oppgaveId)

                statement.executeUpdate()
            }
        }
    }

    override fun oppdaterIdent(
        oppgaveId: UUID,
        nyttFnr: String,
    ) {
        connectionAutoclosing.hentConnection {
            with(it) {
                prepareStatement(
                    """
                    UPDATE oppgave
                    SET fnr = ?
                    WHERE id = ?::UUID
                    """.trimIndent(),
                ).apply {
                    setString(1, nyttFnr)
                    setObject(2, oppgaveId)
                }.executeUpdate()
            }
        }
    }

    override fun fjernForrigeSaksbehandler(oppgaveId: UUID) {
        connectionAutoclosing.hentConnection {
            with(it) {
                val statement =
                    prepareStatement(
                        """
                        UPDATE oppgave
                        SET forrige_saksbehandler = NULL
                        where id = ?::UUID
                        """.trimIndent(),
                    )
                statement.setObject(1, oppgaveId)

                statement.executeUpdate()
            }
        }
    }

    override fun settForrigeSaksbehandlerFraSaksbehandler(oppgaveId: UUID) {
        connectionAutoclosing.hentConnection {
            with(it) {
                val statement =
                    prepareStatement(
                        """
                        UPDATE oppgave
                        SET forrige_saksbehandler = saksbehandler
                        where id = ?::UUID
                        """.trimIndent(),
                    )
                statement.setObject(1, oppgaveId)

                statement.executeUpdate()
            }
        }
    }

    override fun fjernSaksbehandler(oppgaveId: UUID) {
        connectionAutoclosing.hentConnection {
            with(it) {
                val statement =
                    prepareStatement(
                        """
                        UPDATE oppgave
                        SET saksbehandler = NULL
                        where id = ?::UUID
                        """.trimIndent(),
                    )
                statement.setObject(1, oppgaveId)

                statement.executeUpdate()
            }
        }
    }

    override fun redigerFrist(
        oppgaveId: UUID,
        frist: Tidspunkt,
    ) {
        connectionAutoclosing.hentConnection {
            with(it) {
                val statement =
                    prepareStatement(
                        """
                        UPDATE oppgave
                        SET frist = ?
                        where id = ?::UUID
                        """.trimIndent(),
                    )
                statement.setTidspunkt(1, frist)
                statement.setObject(2, oppgaveId)

                statement.executeUpdate()
            }
        }
    }

    override fun oppdaterMerknad(
        oppgaveId: UUID,
        merknad: String,
    ) {
        connectionAutoclosing.hentConnection {
            with(it) {
                val statement =
                    prepareStatement(
                        """
                        UPDATE oppgave
                        SET merknad = ?
                        where id = ?::UUID
                        """.trimIndent(),
                    )
                statement.setString(1, merknad)
                statement.setObject(2, oppgaveId)

                statement.executeUpdate()
            }
        }
    }

    override fun oppdaterStatusOgMerknad(
        oppgaveId: UUID,
        merknad: String,
        oppgaveStatus: Status,
    ) {
        connectionAutoclosing.hentConnection {
            with(it) {
                val statement =
                    prepareStatement(
                        """
                        UPDATE oppgave
                        SET merknad = ?, status = ?
                        where id = ?::UUID
                        """.trimIndent(),
                    )
                statement.setString(1, merknad)
                statement.setString(2, oppgaveStatus.name)
                statement.setObject(3, oppgaveId)

                statement.executeUpdate()
            }
        }
    }

    override fun oppdaterPaaVent(
        oppgaveId: UUID,
        merknad: String,
        aarsak: PaaVentAarsak?,
        oppgaveStatus: Status,
    ) {
        connectionAutoclosing.hentConnection {
            with(it) {
                val statement =
                    prepareStatement(
                        """
                        UPDATE oppgave
                        SET merknad = ?, status = ?, paavent_aarsak = ?
                        where id = ?::UUID
                        """.trimIndent(),
                    )
                statement.setString(1, merknad)
                statement.setString(2, oppgaveStatus.name)
                statement.setString(3, aarsak?.name)
                statement.setObject(4, oppgaveId)

                statement.executeUpdate()
            }
        }
    }

    override fun oppdaterReferanseOgMerknad(
        oppgaveId: UUID,
        referanse: String,
        merknad: String,
    ) {
        connectionAutoclosing.hentConnection {
            with(it) {
                val statement =
                    prepareStatement(
                        """
                        UPDATE oppgave
                        SET referanse = ?, merknad = ?
                        where id = ?::UUID
                        """.trimIndent(),
                    )
                statement.setString(1, referanse)
                statement.setString(2, merknad)
                statement.setObject(3, oppgaveId)

                statement.executeUpdate()
            }
        }
    }

    override fun endreTilKildeBehandlingOgOppdaterReferanse(
        oppgaveId: UUID,
        referanse: String,
    ) {
        connectionAutoclosing.hentConnection {
            with(it) {
                val statement =
                    prepareStatement(
                        """
                        UPDATE oppgave
                        SET kilde = 'BEHANDLING',
                            referanse = ?
                        where id = ?::UUID
                        """.trimIndent(),
                    )
                statement.setString(1, referanse)
                statement.setObject(2, oppgaveId)

                statement.executeUpdate()
            }
        }
    }

    override fun hentFristGaarUt(
        dato: LocalDate,
        type: Collection<OppgaveType>,
        kilde: Collection<OppgaveKilde>,
        oppgaver: List<UUID>,
        grense: Int,
    ): List<VentefristGaarUt> =
        connectionAutoclosing.hentConnection {
            with(it) {
                val statement =
                    prepareStatement(
                        """
                        SELECT o.id, o.referanse, o.sak_id, o.kilde, o.merknad
                        FROM oppgave o LEFT JOIN saksbehandler_info si ON o.saksbehandler = si.id
                        WHERE o.frist <= ?
                        and type = ANY(?)
                        and kilde = ANY(?)
                        and status = 'PAA_VENT'
                        LIMIT ?
                        """.trimIndent(),
                    )
                statement.setTidspunkt(1, dato.atTime(LocalTime.NOON).toTidspunkt())
                statement.setArray(2, createArrayOf("text", type.map { i -> i.name }.toTypedArray()))
                statement.setArray(3, createArrayOf("text", kilde.map { i -> i.name }.toTypedArray()))
                statement.setInt(4, grense)
                statement
                    .executeQuery()
                    .toList {
                        VentefristGaarUt(
                            oppgaveId = getUUID("id"),
                            sakId = SakId(getLong("sak_id")),
                            referanse = getString("referanse"),
                            oppgavekilde = OppgaveKilde.valueOf(getString("kilde")),
                            merknad = getString("merknad"),
                        )
                    }.also { utgaatte ->
                        logger.info("Hentet ${utgaatte.size} oppgaver der fristen går ut for dato $dato og type $type")
                    }.filter { oppgave -> oppgaver.isEmpty() || oppgaver.contains(oppgave.oppgaveId) }
            }
        }

    private fun ResultSet.asOppgave(): OppgaveIntern =
        OppgaveIntern(
            id = getObject("id") as UUID,
            status = Status.valueOf(getString("status")),
            enhet = Enhetsnummer(getString("enhet")),
            sakId = SakId(getLong("sak_id")),
            kilde = getString("kilde")?.let { OppgaveKilde.valueOf(it) },
            type = OppgaveType.valueOf(getString("type")),
            referanse = getString("referanse"),
            gruppeId = getString("gruppe_id"),
            merknad = getString("merknad"),
            opprettet = getTidspunkt("opprettet"),
            sakType = SakType.valueOf(getString("saktype")),
            fnr = getString("fnr"),
            frist = getTidspunktOrNull("frist"),
            saksbehandler =
                getString("saksbehandler")?.let {
                    OppgaveSaksbehandler(getString("saksbehandler"), getString("navn"))
                },
            forrigeSaksbehandlerIdent = getString("forrige_saksbehandler"),
        )
}
