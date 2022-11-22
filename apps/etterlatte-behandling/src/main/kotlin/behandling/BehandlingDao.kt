package no.nav.etterlatte.behandling

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.etterlatte.database.singleOrNull
import no.nav.etterlatte.database.toList
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.KommerBarnetTilgode
import no.nav.etterlatte.libs.common.behandling.OppgaveStatus
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.behandling.RevurderingAarsak
import no.nav.etterlatte.libs.common.behandling.Saksrolle
import no.nav.etterlatte.libs.common.behandling.Virkningstidspunkt
import no.nav.etterlatte.libs.common.toJson
import java.sql.Connection
import java.sql.ResultSet
import java.sql.Timestamp
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*

class BehandlingDao(private val connection: () -> Connection) {

    fun hentBehandling(id: UUID, type: BehandlingType): Behandling? {
        val stmt =
            connection().prepareStatement(
                """
                    SELECT * 
                    FROM behandling where id = ? AND behandlingstype = ?
                    """
            )
        stmt.setObject(1, id)
        stmt.setString(2, type.name)

        return stmt.executeQuery().singleOrNull {
            behandlingAvRettType()
        }
    }

    fun hentBehandling(id: UUID): Behandling? {
        val stmt =
            connection().prepareStatement(
                """
                    SELECT * 
                    FROM behandling where id = ?
                    """
            )
        stmt.setObject(1, id)

        return stmt.executeQuery().singleOrNull {
            behandlingAvRettType()
        }
    }

    fun hentBehandlingType(id: UUID): BehandlingType? {
        val stmt =
            connection().prepareStatement(
                "SELECT behandlingstype from behandling WHERE id = ?"
            )
        stmt.setObject(1, id)
        return stmt.executeQuery().singleOrNull {
            getString("behandlingstype").let { BehandlingType.valueOf(it) }
        }
    }

    fun alleBehandlingerAvType(type: BehandlingType): List<Behandling> {
        val stmt =
            connection().prepareStatement(
                """
                    SELECT *
                    FROM behandling where behandlingstype = ?
                """.trimIndent()
            )
        stmt.setString(1, type.name)
        return stmt.executeQuery().toList {
            when (type) {
                BehandlingType.FØRSTEGANGSBEHANDLING -> asFoerstegangsbehandling(this)
                BehandlingType.REVURDERING -> asRevurdering(this)
                BehandlingType.MANUELT_OPPHOER -> asManueltOpphoer(this)
            }
        }
    }

    fun alleBehandlingerISakAvType(sakId: Long, type: BehandlingType): List<Behandling> {
        return connection().prepareStatement(
            """
                SELECT * 
                FROM behandling
                WHERE sak_id = ?
                    AND behandlingstype = ?
            """.trimIndent()
        ).let {
            it.setLong(1, sakId)
            it.setString(2, type.name)
            it.executeQuery()
        }.toList {
            when (type) {
                BehandlingType.FØRSTEGANGSBEHANDLING -> asFoerstegangsbehandling(this)
                BehandlingType.REVURDERING -> asRevurdering(this)
                BehandlingType.MANUELT_OPPHOER -> asManueltOpphoer(this)
            }
        }
    }

    fun alleBehandlinger(): List<Behandling> {
        val stmt =
            connection().prepareStatement(
                """
                    SELECT *
                    FROM behandling 
                """.trimIndent()
            )
        return stmt.executeQuery().behandlingsListe()
    }

    fun alleBehandingerISak(sakid: Long): List<Behandling> {
        val stmt =
            connection().prepareStatement(
                """
                    SELECT *
                    FROM behandling where sak_id = ?
                """.trimIndent()
            )
        stmt.setLong(1, sakid)
        return stmt.executeQuery().behandlingsListe()
    }

    fun alleAktiveBehandlingerISak(sakid: Long): List<Behandling> {
        with(connection()) {
            val stmt =
                prepareStatement(
                    """
                        SELECT * 
                        FROM behandling 
                        WHERE sak_id = ?
                            AND status = ANY(?)
                    """.trimIndent()
                )
            stmt.setLong(1, sakid)
            stmt.setArray(
                2,
                createArrayOf("text", BehandlingStatus.underBehandling().map { it.name }.toTypedArray())
            )
            return stmt.executeQuery().behandlingsListe()
        }
    }

    fun alleBehandlingerForSoekerMedFnr(fnr: String): List<Behandling> {
        val stmt =
            connection().prepareStatement(
                """
                    SELECT * 
                    FROM behandling where soeker = ?
                """.trimIndent()
            )
        stmt.setString(1, fnr)
        return stmt.executeQuery().behandlingsListe()
    }

    private fun asFoerstegangsbehandling(rs: ResultSet) = Foerstegangsbehandling(
        id = rs.getObject("id") as UUID,
        sak = rs.getLong("sak_id"),
        behandlingOpprettet = rs.getTimestamp("behandling_opprettet").toInstant().atZone(ZoneId.systemDefault())
            .toLocalDateTime(),
        sistEndret = rs.getTimestamp("sist_endret").toLocalDateTime(),
        soeknadMottattDato = rs.getTimestamp("soekand_mottatt_dato").toLocalDateTime(),
        persongalleri = hentPersongalleri(rs),
        gyldighetsproeving = rs.getString("gyldighetssproving")?.let { objectMapper.readValue(it) },
        status = rs.getString("status").let { BehandlingStatus.valueOf(it) },
        type = rs.getString("behandlingstype").let { BehandlingType.valueOf(it) },
        oppgaveStatus = rs.getString("oppgave_status")?.let { OppgaveStatus.valueOf(it) },
        virkningstidspunkt = rs.getString("virkningstidspunkt")?.let { objectMapper.readValue(it) },
        kommerBarnetTilgode = rs.getString("kommer_barnet_tilgode")?.let { objectMapper.readValue(it) }
    )

    private fun hentPersongalleri(rs: ResultSet): Persongalleri = Persongalleri(
        innsender = rs.getString("innsender"),
        soeker = rs.getString("soeker"),
        gjenlevende = rs.getString("gjenlevende").let { objectMapper.readValue(it) },
        avdoed = rs.getString("avdoed").let { objectMapper.readValue(it) },
        soesken = rs.getString("soesken").let { objectMapper.readValue(it) }
    )

    private fun asRevurdering(rs: ResultSet) = Revurdering(
        id = rs.getObject("id") as UUID,
        sak = rs.getLong("sak_id"),
        behandlingOpprettet = rs.getTimestamp("behandling_opprettet").toInstant().atZone(ZoneId.systemDefault())
            .toLocalDateTime(),
        sistEndret = rs.getTimestamp("sist_endret").toLocalDateTime(),
        persongalleri = hentPersongalleri(rs),
        status = rs.getString("status").let { BehandlingStatus.valueOf(it) },
        type = rs.getString("behandlingstype").let { BehandlingType.valueOf(it) },
        oppgaveStatus = rs.getString("oppgave_status")?.let { OppgaveStatus.valueOf(it) },
        revurderingsaarsak = rs.getString("revurdering_aarsak").let { RevurderingAarsak.valueOf(it) },
        kommerBarnetTilgode = rs.getString("kommer_barnet_tilgode")?.let { objectMapper.readValue(it) }
    )

    private fun asManueltOpphoer(rs: ResultSet) = ManueltOpphoer(
        id = rs.getObject("id") as UUID,
        sak = rs.getLong("sak_id"),
        behandlingOpprettet = rs.getTimestamp("behandling_opprettet").toInstant().atZone(ZoneId.systemDefault())
            .toLocalDateTime(),
        sistEndret = rs.getTimestamp("sist_endret").toLocalDateTime(),
        persongalleri = hentPersongalleri(rs),
        status = rs.getString("status").let { BehandlingStatus.valueOf(it) },
        type = rs.getString("behandlingstype").let { BehandlingType.valueOf(it) },
        oppgaveStatus = rs.getString("oppgave_status")?.let { OppgaveStatus.valueOf(it) },
        opphoerAarsaker = rs.getString("opphoer_aarsaker").let { objectMapper.readValue(it) },
        fritekstAarsak = rs.getString("fritekst_aarsak")
    )

    private fun ResultSet.asRolleSak() =
        Pair(this.getString("rolle"), this.getLong("sak_id"))

    fun alleSakIderMedUavbruttBehandlingForSoekerMedFnr(fnr: String): List<Long> {
        return connection().prepareStatement(
            """
                SELECT DISTINCT sak_id FROM behandling
                    WHERE soeker = ? 
                    AND  status != 'AVBRUTT'
            """.trimIndent()
        ).let {
            it.setString(1, fnr)
            it.executeQuery()
        }.toList {
            getLong(1)
        }
    }

    fun opprettFoerstegangsbehandling(foerstegangsbehandling: Foerstegangsbehandling) {
        val stmt =
            connection().prepareStatement(
                """
                INSERT INTO behandling(id, sak_id, behandling_opprettet, sist_endret, status, behandlingstype, 
                soekand_mottatt_dato, innsender, soeker, gjenlevende, avdoed, soesken, oppgave_status, virkningstidspunkt, kommer_barnet_tilgode)
                 VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent()
            )
        with(foerstegangsbehandling) {
            stmt.setObject(1, id)
            stmt.setLong(2, sak)
            stmt.setTimestamp(
                3,
                Timestamp.from(behandlingOpprettet.atZone(ZoneId.systemDefault()).toInstant())
            )
            stmt.setTimestamp(
                4,
                Timestamp.from(sistEndret.atZone(ZoneId.systemDefault()).toInstant())
            )
            stmt.setString(5, status.name)
            stmt.setString(6, type.name)
            stmt.setTimestamp(
                7,
                Timestamp.from(soeknadMottattDato.atZone(ZoneId.systemDefault()).toInstant())
            )
            with(persongalleri) {
                stmt.setString(8, innsender)
                stmt.setString(9, soeker)
                stmt.setString(10, gjenlevende.toJson())
                stmt.setString(11, avdoed.toJson())
                stmt.setString(12, soesken.toJson())
            }
            stmt.setString(13, oppgaveStatus?.name)
            stmt.setString(
                14,
                foerstegangsbehandling.hentVirkningstidspunkt()?.toJson()
            )
            stmt.setString(
                15,
                kommerBarnetTilgode?.toJson()
            )
        }
        stmt.executeUpdate()
    }

    fun opprettRevurdering(revurdering: Revurdering) {
        val stmt =
            connection().prepareStatement(
                """
                INSERT INTO behandling(id, sak_id, behandling_opprettet, sist_endret, status, behandlingstype, 
                 innsender, soeker, gjenlevende, avdoed, soesken, oppgave_status, revurdering_aarsak, kommer_barnet_tilgode)
                 VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent()
            )
        with(revurdering) {
            stmt.setObject(1, id)
            stmt.setLong(2, sak)
            stmt.setTimestamp(
                3,
                Timestamp.from(behandlingOpprettet.atZone(ZoneId.systemDefault()).toInstant())
            )
            stmt.setTimestamp(
                4,
                Timestamp.from(sistEndret.atZone(ZoneId.systemDefault()).toInstant())
            )
            stmt.setString(5, status.name)
            stmt.setString(6, type.name)
            with(persongalleri) {
                stmt.setString(7, innsender)
                stmt.setString(8, soeker)
                stmt.setString(9, gjenlevende.toJson())
                stmt.setString(10, avdoed.toJson())
                stmt.setString(11, soesken.toJson())
            }
            stmt.setString(12, oppgaveStatus?.name)
            stmt.setString(13, revurderingsaarsak.name)
            stmt.setString(14, kommerBarnetTilgode?.let { objectMapper.writeValueAsString(it) })
        }
        require(stmt.executeUpdate() == 1)
    }

    fun opprettManueltOpphoer(manueltOpphoer: ManueltOpphoer): ManueltOpphoer {
        val stmt =
            connection().prepareStatement(
                """
                    INSERT INTO behandling(id, sak_id, behandling_opprettet, sist_endret, status, behandlingstype, 
                    innsender, soeker, gjenlevende, avdoed, soesken, oppgave_status, opphoer_aarsaker, fritekst_aarsak)
                    VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    RETURNING *
                """.trimIndent()
            )
        with(manueltOpphoer) {
            stmt.setObject(1, id)
            stmt.setLong(2, sak)
            stmt.setTimestamp(
                3,
                Timestamp.from(behandlingOpprettet.atZone(ZoneId.systemDefault()).toInstant())
            )
            stmt.setTimestamp(
                4,
                Timestamp.from(sistEndret.atZone(ZoneId.systemDefault()).toInstant())
            )
            stmt.setString(5, status.name)
            stmt.setString(6, type.name)
            with(persongalleri) {
                stmt.setString(7, innsender)
                stmt.setString(8, soeker)
                stmt.setString(9, gjenlevende.toJson())
                stmt.setString(10, avdoed.toJson())
                stmt.setString(11, soesken.toJson())
            }
            stmt.setString(12, oppgaveStatus?.name)
            stmt.setString(13, opphoerAarsaker.toJson())
            stmt.setString(14, fritekstAarsak)
        }
        return stmt.executeQuery().singleOrNull {
            behandlingAvRettType() as ManueltOpphoer
        }
            ?: throw BehandlingNotFoundException("Fant ikke manuelt opphoer med id ${manueltOpphoer.id}")
    }

    fun lagreGyldighetsproving(behandling: Foerstegangsbehandling) {
        val stmt =
            connection().prepareStatement(
                "UPDATE behandling " +
                    "SET gyldighetssproving = ?, status = ?, oppgave_status = ?, sist_endret = ? " +
                    "WHERE id = ?"
            )
        stmt.setObject(1, objectMapper.writeValueAsString(behandling.gyldighetsproeving))
        stmt.setString(2, behandling.status.name)
        stmt.setString(3, behandling.oppgaveStatus?.name)
        stmt.setTimestamp(
            4,
            Timestamp.from(behandling.sistEndret.atZone(ZoneId.systemDefault()).toInstant())
        )
        stmt.setObject(5, behandling.id)
        require(stmt.executeUpdate() == 1)
    }

    fun slettBehandlingerISak(id: Long) {
        val statement = connection().prepareStatement("DELETE from behandling where sak_id = ?")
        statement.setLong(1, id)
        statement.executeUpdate()
    }

    fun lagreStatus(lagretBehandling: Behandling) {
        lagreStatus(lagretBehandling.id, lagretBehandling.status, lagretBehandling.sistEndret)
    }

    private fun lagreStatus(behandling: UUID, status: BehandlingStatus, sistEndret: LocalDateTime): Behandling {
        val stmt =
            connection().prepareStatement("UPDATE behandling SET status = ?, sist_endret = ? WHERE id = ? RETURNING *")
        stmt.setString(1, status.name)
        stmt.setTimestamp(
            2,
            Timestamp.from(sistEndret.atZone(ZoneId.systemDefault()).toInstant())
        )
        stmt.setObject(3, behandling)
        return requireNotNull(
            stmt.executeQuery().singleOrNull {
                behandlingAvRettType()
            }
        )
    }

    fun lagreOppgaveStatus(behandling: Behandling) {
        val stmt =
            connection().prepareStatement("UPDATE behandling SET oppgave_status = ?, sist_endret = ? WHERE id = ?")
        stmt.setString(1, behandling.oppgaveStatus?.name)
        stmt.setTimestamp(
            2,
            Timestamp.from(behandling.sistEndret.atZone(ZoneId.systemDefault()).toInstant())
        )
        stmt.setObject(3, behandling.id)
        require(stmt.executeUpdate() == 1)
    }

    fun lagreStatusOgOppgaveStatus(
        behandling: UUID,
        behandlingStatus: BehandlingStatus,
        oppgaveStatus: OppgaveStatus?,
        sistEndret: LocalDateTime
    ): Behandling {
        val stmt =
            connection().prepareStatement(
                "UPDATE behandling SET status = ?, oppgave_status = ?, sist_endret = ? WHERE id = ? RETURNING *"
            )
        stmt.setString(1, behandlingStatus.name)
        stmt.setString(2, oppgaveStatus?.name)
        stmt.setTimestamp(
            3,
            Timestamp.from(sistEndret.atZone(ZoneId.systemDefault()).toInstant())
        )
        stmt.setObject(4, behandling)
        return requireNotNull(
            stmt.executeQuery().singleOrNull {
                behandlingAvRettType()
            }
        )
    }

    fun ResultSet.behandlingsListe(): List<Behandling> =
        toList {
            when (getString("behandlingstype")) {
                BehandlingType.FØRSTEGANGSBEHANDLING.name -> asFoerstegangsbehandling(this)
                BehandlingType.REVURDERING.name -> asRevurdering(this)
                BehandlingType.MANUELT_OPPHOER.name -> asManueltOpphoer(this)
                else -> null
            }
        }.filterNotNull()

    private fun ResultSet.behandlingAvRettType() =
        when (getString("behandlingstype")) {
            BehandlingType.FØRSTEGANGSBEHANDLING.name -> asFoerstegangsbehandling(this)
            BehandlingType.REVURDERING.name -> asRevurdering(this)
            BehandlingType.MANUELT_OPPHOER.name -> asManueltOpphoer(this)
            else -> null
        }

    fun slettRevurderingerISak(sakId: Long) {
        val statement =
            connection().prepareStatement("DELETE from behandling where sak_id = ? AND behandlingstype = 'REVURDERING'")
        statement.setLong(1, sakId)
        statement.executeUpdate()
    }

    fun avbrytBehandling(behandlingId: UUID): Behandling {
        return this.lagreStatusOgOppgaveStatus(
            behandling = behandlingId,
            behandlingStatus = BehandlingStatus.AVBRUTT,
            oppgaveStatus = OppgaveStatus.LUKKET,
            sistEndret = LocalDateTime.now()
        )
    }

    fun lagreNyttVirkningstidspunkt(behandlingId: UUID, virkningstidspunkt: Virkningstidspunkt) {
        val statement =
            connection().prepareStatement("UPDATE behandling SET virkningstidspunkt = ? where id = ?")
        statement.setString(1, objectMapper.writeValueAsString(virkningstidspunkt))
        statement.setObject(2, behandlingId)
        statement.executeUpdate()
    }

    fun lagreKommerBarnetTilgode(behandlingId: UUID, kommerBarnetTilgode: KommerBarnetTilgode) {
        val statement =
            connection().prepareStatement("UPDATE behandling SET kommer_barnet_tilgode = ? where id = ?")
        statement.setString(1, kommerBarnetTilgode.toJson())
        statement.setObject(2, behandlingId)
        statement.executeUpdate()
    }

    /*sjekker om et fnr opptrer i persongalleriet til behandlinger. Returnerer rollen og saksnr som Pair*/
    fun sakerOgRollerMedFnrIPersongalleri(fnr: String): List<Pair<Saksrolle, Long>> {
        val statement = connection().prepareStatement(
            """
              SELECT (
                SELECT string_agg(col, ', ' ORDER BY col) AS rolle
                FROM jsonb_each_text(to_jsonb(json_build_object('innsender', behandling.innsender, 'soeker', behandling.soeker, 'gjenlevende', behandling.gjenlevende, 'avdoed', behandling.avdoed, 'soesken', behandling.soesken))) t(col, val)
                WHERE t.val LIKE '%' || ? || '%'
              ), sak_id
              FROM behandling
              WHERE (
                SELECT string_agg(col, ', ' ORDER BY col)
                FROM jsonb_each_text(to_jsonb(json_build_object('innsender', behandling.innsender, 'soeker', behandling.soeker, 'gjenlevende', behandling.gjenlevende, 'avdoed', behandling.avdoed, 'soesken', behandling.soesken))) t(col, val)
                WHERE t.val LIKE '%' || ? || '%'
              ) IS NOT NULL;
            """.trimIndent()
        ).also {
            it.setString(1, fnr)
            it.setString(2, fnr)
        }
        return statement.executeQuery().toList {
            asRolleSak()
        }.flatMap { par ->
            par.first.split(", ").map {
                Pair(Saksrolle.enumVedNavnEllerUkjent(it), par.second)
            }
        }
    }
}

val objectMapper: ObjectMapper =
    jacksonObjectMapper().registerModule(JavaTimeModule()).disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

class BehandlingNotFoundException(override val message: String) : RuntimeException(message)