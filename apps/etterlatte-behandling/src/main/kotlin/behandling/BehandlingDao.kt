package no.nav.etterlatte.behandling

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.etterlatte.behandling.domain.Behandling
import no.nav.etterlatte.behandling.domain.Foerstegangsbehandling
import no.nav.etterlatte.behandling.domain.ManueltOpphoer
import no.nav.etterlatte.behandling.domain.Regulering
import no.nav.etterlatte.behandling.domain.Revurdering
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.KommerBarnetTilgode
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.behandling.RevurderingAarsak
import no.nav.etterlatte.libs.common.behandling.Saksrolle
import no.nav.etterlatte.libs.common.behandling.Virkningstidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.tilSystemDefaultLocalDateTime
import no.nav.etterlatte.libs.common.tidspunkt.tilSystemDefaultTimestamp
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingUtfall
import no.nav.etterlatte.libs.database.singleOrNull
import no.nav.etterlatte.libs.database.toList
import java.sql.Connection
import java.sql.ResultSet
import java.time.LocalDateTime
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
        return stmt.executeQuery().toList { tilBehandling(type.name)!! }
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
        }.toList { tilBehandling(type.name)!! }
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

    fun alleBehandlingerISak(sakid: Long): List<Behandling> {
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
        behandlingOpprettet = rs.somLocalDateTime("behandling_opprettet"),
        sistEndret = rs.getTimestamp("sist_endret").toLocalDateTime(),
        soeknadMottattDato = rs.getTimestamp("soeknad_mottatt_dato").toLocalDateTime(),
        persongalleri = hentPersongalleri(rs),
        gyldighetsproeving = rs.getString("gyldighetssproving")?.let { objectMapper.readValue(it) },
        status = rs.getString("status").let { BehandlingStatus.valueOf(it) },
        virkningstidspunkt = rs.getString("virkningstidspunkt")?.let { objectMapper.readValue(it) },
        kommerBarnetTilgode = rs.getString("kommer_barnet_tilgode")?.let { objectMapper.readValue(it) },
        vilkaarUtfall = rs.getString("vilkaar_utfall")?.let { VilkaarsvurderingUtfall.valueOf(it) }
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
        behandlingOpprettet = rs.somLocalDateTime("behandling_opprettet"),
        sistEndret = rs.getTimestamp("sist_endret").toLocalDateTime(),
        persongalleri = hentPersongalleri(rs),
        status = rs.getString("status").let { BehandlingStatus.valueOf(it) },
        revurderingsaarsak = rs.getString("revurdering_aarsak").let { RevurderingAarsak.valueOf(it) },
        kommerBarnetTilgode = rs.getString("kommer_barnet_tilgode")?.let { objectMapper.readValue(it) },
        vilkaarUtfall = rs.getString("vilkaar_utfall")?.let { VilkaarsvurderingUtfall.valueOf(it) },
        virkningstidspunkt = rs.getString("virkningstidspunkt")?.let { objectMapper.readValue(it) }
    )

    private fun asRegulering(rs: ResultSet) = Regulering(
        id = rs.getObject("id") as UUID,
        sak = rs.getLong("sak_id"),
        behandlingOpprettet = rs.somLocalDateTime("behandling_opprettet"),
        sistEndret = rs.getTimestamp("sist_endret").toLocalDateTime(),
        persongalleri = hentPersongalleri(rs),
        status = rs.getString("status").let { BehandlingStatus.valueOf(it) },
        revurderingsaarsak = rs.getString("revurdering_aarsak").let { RevurderingAarsak.valueOf(it) },
        kommerBarnetTilgode = rs.getString("kommer_barnet_tilgode")?.let { objectMapper.readValue(it) },
        vilkaarUtfall = rs.getString("vilkaar_utfall")?.let { VilkaarsvurderingUtfall.valueOf(it) },
        virkningstidspunkt = rs.getString("virkningstidspunkt")?.let { objectMapper.readValue(it) }
    )

    private fun asManueltOpphoer(rs: ResultSet) = ManueltOpphoer(
        id = rs.getObject("id") as UUID,
        sak = rs.getLong("sak_id"),
        behandlingOpprettet = rs.somLocalDateTime("behandling_opprettet"),
        sistEndret = rs.getTimestamp("sist_endret").toLocalDateTime(),
        persongalleri = hentPersongalleri(rs),
        status = rs.getString("status").let { BehandlingStatus.valueOf(it) },
        virkningstidspunkt = rs.getString("virkningstidspunkt")?.let { objectMapper.readValue(it) },
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
                soeknad_mottatt_dato, innsender, soeker, gjenlevende, avdoed, soesken, virkningstidspunkt, kommer_barnet_tilgode, vilkaar_utfall)
                 VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent()
            )
        with(foerstegangsbehandling) {
            stmt.setObject(1, id)
            stmt.setLong(2, sak)
            stmt.setTimestamp(
                3,
                behandlingOpprettet.somTimestamp()
            )
            stmt.setTimestamp(
                4,
                sistEndret.somTimestamp()
            )
            stmt.setString(5, status.name)
            stmt.setString(6, type.name)
            stmt.setTimestamp(
                7,
                soeknadMottattDato.somTimestamp()
            )
            with(persongalleri) {
                stmt.setString(8, innsender)
                stmt.setString(9, soeker)
                stmt.setString(10, gjenlevende.toJson())
                stmt.setString(11, avdoed.toJson())
                stmt.setString(12, soesken.toJson())
            }
            stmt.setString(
                13,
                foerstegangsbehandling.virkningstidspunkt?.toJson()
            )
            stmt.setString(14, kommerBarnetTilgode?.toJson())
            stmt.setString(15, vilkaarUtfall?.name)
        }
        stmt.executeUpdate()
    }

    fun opprettRevurdering(revurdering: Revurdering) {
        val stmt =
            connection().prepareStatement(
                """
                INSERT INTO behandling(id, sak_id, behandling_opprettet, sist_endret, status, behandlingstype, 
                    innsender, soeker, gjenlevende, avdoed, soesken, revurdering_aarsak, kommer_barnet_tilgode, 
                    vilkaar_utfall, virkningstidspunkt)
                 VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent()
            )
        with(revurdering) {
            stmt.setObject(1, id)
            stmt.setLong(2, sak)
            stmt.setTimestamp(
                3,
                behandlingOpprettet.somTimestamp()
            )
            stmt.setTimestamp(
                4,
                sistEndret.somTimestamp()
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
            stmt.setString(12, revurderingsaarsak.name)
            stmt.setString(13, kommerBarnetTilgode?.let { objectMapper.writeValueAsString(it) })
            stmt.setString(14, vilkaarUtfall?.name)
            stmt.setString(15, virkningstidspunkt?.toJson())
        }
        require(stmt.executeUpdate() == 1)
    }

    fun opprettRegulering(regulering: Regulering) {
        val stmt =
            connection().prepareStatement(
                """
                INSERT INTO behandling(id, sak_id, behandling_opprettet, sist_endret, status, behandlingstype, 
                    innsender, soeker, gjenlevende, avdoed, soesken, revurdering_aarsak, kommer_barnet_tilgode, 
                    vilkaar_utfall, virkningstidspunkt)
                 VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent()
            )
        with(regulering) {
            stmt.setObject(1, id)
            stmt.setLong(2, sak)
            stmt.setTimestamp(3, behandlingOpprettet.somTimestamp())
            stmt.setTimestamp(4, sistEndret.somTimestamp())
            stmt.setString(5, status.name)
            stmt.setString(6, type.name)
            with(persongalleri) {
                stmt.setString(7, innsender)
                stmt.setString(8, soeker)
                stmt.setString(9, gjenlevende.toJson())
                stmt.setString(10, avdoed.toJson())
                stmt.setString(11, soesken.toJson())
            }
            stmt.setString(12, revurderingsaarsak.name)
            stmt.setString(13, kommerBarnetTilgode?.let { objectMapper.writeValueAsString(it) })
            stmt.setString(14, vilkaarUtfall?.name)
            stmt.setString(15, virkningstidspunkt?.toJson())
        }
        require(stmt.executeUpdate() == 1)
    }

    fun opprettManueltOpphoer(manueltOpphoer: ManueltOpphoer): ManueltOpphoer {
        val stmt =
            connection().prepareStatement(
                """
                    INSERT INTO behandling(id, sak_id, behandling_opprettet, sist_endret, status, behandlingstype, 
                    innsender, soeker, gjenlevende, avdoed, soesken, opphoer_aarsaker, fritekst_aarsak,
                    virkningstidspunkt)
                    VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    RETURNING *
                """.trimIndent()
            )
        with(manueltOpphoer) {
            stmt.setObject(1, id)
            stmt.setLong(2, sak)
            stmt.setTimestamp(
                3,
                behandlingOpprettet.somTimestamp()
            )
            stmt.setTimestamp(
                4,
                sistEndret.somTimestamp()
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
            stmt.setString(12, opphoerAarsaker.toJson())
            stmt.setString(13, fritekstAarsak)
            stmt.setString(14, virkningstidspunkt?.toJson())
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
                    "SET gyldighetssproving = ?, status = ?, sist_endret = ? " +
                    "WHERE id = ?"
            )
        stmt.setObject(1, objectMapper.writeValueAsString(behandling.gyldighetsproeving))
        stmt.setString(2, behandling.status.name)
        stmt.setTimestamp(
            3,
            behandling.sistEndret.somTimestamp()
        )
        stmt.setObject(4, behandling.id)
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

    fun lagreStatus(behandling: UUID, status: BehandlingStatus, sistEndret: LocalDateTime): Behandling {
        val stmt =
            connection().prepareStatement("UPDATE behandling SET status = ?, sist_endret = ? WHERE id = ? RETURNING *")
        stmt.setString(1, status.name)
        stmt.setTimestamp(
            2,
            sistEndret.somTimestamp()
        )
        stmt.setObject(3, behandling)
        return requireNotNull(
            stmt.executeQuery().singleOrNull {
                behandlingAvRettType()
            }
        )
    }

    private fun ResultSet.behandlingsListe(): List<Behandling> =
        toList { tilBehandling(getString("behandlingstype")) }.filterNotNull()

    private fun ResultSet.tilBehandling(key: String?) = when (key) {
        BehandlingType.FØRSTEGANGSBEHANDLING.name -> asFoerstegangsbehandling(this)
        BehandlingType.REVURDERING.name -> asRevurdering(this)
        BehandlingType.OMREGNING.name -> asRegulering(this)
        BehandlingType.MANUELT_OPPHOER.name -> asManueltOpphoer(this)
        else -> null
    }

    private fun ResultSet.behandlingAvRettType() = tilBehandling(getString("behandlingstype"))

    fun avbrytBehandling(behandlingId: UUID): Behandling {
        return this.lagreStatus(
            behandling = behandlingId,
            status = BehandlingStatus.AVBRUTT,
            sistEndret = LocalDateTime.now()
        )
    }

    fun lagreNyttVirkningstidspunkt(behandlingId: UUID, virkningstidspunkt: Virkningstidspunkt) {
        val statement = connection().prepareStatement("UPDATE behandling SET virkningstidspunkt = ? where id = ?")
        statement.setString(1, objectMapper.writeValueAsString(virkningstidspunkt))
        statement.setObject(2, behandlingId)
        statement.executeUpdate()
    }

    fun lagreKommerBarnetTilgode(behandlingId: UUID, kommerBarnetTilgode: KommerBarnetTilgode) {
        val statement = connection().prepareStatement("UPDATE behandling SET kommer_barnet_tilgode = ? where id = ?")
        statement.setString(1, kommerBarnetTilgode.toJson())
        statement.setObject(2, behandlingId)
        statement.executeUpdate()
    }

    fun lagreVilkaarstatus(behandlingId: UUID, vilkaarUtfall: VilkaarsvurderingUtfall?) {
        val statement = connection().prepareStatement("UPDATE behandling SET vilkaar_utfall = ? where id = ?")
        statement.setString(1, vilkaarUtfall?.toString())
        statement.setObject(2, behandlingId)
        statement.executeUpdate()
    }

    /*sjekker om et fnr opptrer i persongalleriet til behandlinger. Returnerer rollen og saksnr som Pair*/
    /* TODO: EY-1430: Skriv om denne*/
    fun sakerOgRollerMedFnrIPersongalleri(fnr: String): List<Pair<Saksrolle, Long>> {
        val statement = connection().prepareStatement(
            """
              SELECT (
                SELECT string_agg(col, ', ' ORDER BY col) AS rolle
                FROM jsonb_each_text(to_jsonb(json_build_object('soeker', behandling.soeker, 'gjenlevende', behandling.gjenlevende, 'avdoed', behandling.avdoed, 'soesken', behandling.soesken))) t(col, val)
                WHERE t.val LIKE '%' || ? || '%'
              ), sak_id
              FROM behandling
              WHERE (
                SELECT string_agg(col, ', ' ORDER BY col)
                FROM jsonb_each_text(to_jsonb(json_build_object('soeker', behandling.soeker, 'gjenlevende', behandling.gjenlevende, 'avdoed', behandling.avdoed, 'soesken', behandling.soesken))) t(col, val)
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

private fun LocalDateTime.somTimestamp() = tilSystemDefaultTimestamp()

private fun ResultSet.somLocalDateTime(kolonne: String) = getTimestamp(kolonne).tilSystemDefaultLocalDateTime()

val objectMapper: ObjectMapper =
    jacksonObjectMapper().registerModule(JavaTimeModule()).disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

class BehandlingNotFoundException(override val message: String) : RuntimeException(message)