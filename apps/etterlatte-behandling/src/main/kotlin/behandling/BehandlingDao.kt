package no.nav.etterlatte.behandling

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.etterlatte.behandling.domain.Behandling
import no.nav.etterlatte.behandling.domain.Foerstegangsbehandling
import no.nav.etterlatte.behandling.domain.OpprettBehandling
import no.nav.etterlatte.behandling.domain.Revurdering
import no.nav.etterlatte.behandling.hendelse.getUUID
import no.nav.etterlatte.behandling.kommerbarnettilgode.KommerBarnetTilGodeDao
import no.nav.etterlatte.behandling.revurdering.RevurderingDao
import no.nav.etterlatte.common.ConnectionAutoclosing
import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.BoddEllerArbeidetUtlandet
import no.nav.etterlatte.libs.common.behandling.Prosesstype
import no.nav.etterlatte.libs.common.behandling.Revurderingaarsak
import no.nav.etterlatte.libs.common.behandling.Utlandstilknytning
import no.nav.etterlatte.libs.common.behandling.Virkningstidspunkt
import no.nav.etterlatte.libs.common.gyldigSoeknad.GyldighetsResultat
import no.nav.etterlatte.libs.common.sak.BehandlingOgSak
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.sak.Saker
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.getTidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.getTidspunktOrNull
import no.nav.etterlatte.libs.common.tidspunkt.setTidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.toLocalDatetimeUTC
import no.nav.etterlatte.libs.common.tidspunkt.toTidspunkt
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.libs.database.setJsonb
import no.nav.etterlatte.libs.database.singleOrNull
import no.nav.etterlatte.libs.database.toList
import java.sql.Date
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.UUID

class BehandlingDao(
    private val kommerBarnetTilGodeDao: KommerBarnetTilGodeDao,
    private val revurderingDao: RevurderingDao,
    private val connectionAutoclosing: ConnectionAutoclosing,
) {
    private val alleBehandlingerMedSak =
        """
        SELECT b.*, s.sakType, s.enhet, s.fnr 
        FROM behandling b
        INNER JOIN sak s ON b.sak_id = s.id
        """.trimIndent()

    fun hentBehandling(id: UUID): Behandling? =
        connectionAutoclosing.hentConnection {
            with(it) {
                val stmt =
                    prepareStatement(
                        """
                    $alleBehandlingerMedSak
                    WHERE b.id = ?::UUID
                    """,
                    )
                stmt.setObject(1, id)

                stmt.executeQuery().singleOrNull {
                    behandlingAvRettType()
                }
            }
        }

    fun hentBehandlingerForSak(sakid: Long): List<Behandling> =
        connectionAutoclosing.hentConnection {
            with(it) {
                val stmt =
                    prepareStatement(
                        """
                        $alleBehandlingerMedSak
                        WHERE sak_id = ?
                        """.trimIndent(),
                    )

                stmt.setLong(1, sakid)
                stmt.executeQuery().behandlingsListe()
            }
        }

    fun hentAlleRevurderingerISakMedAarsak(
        sakid: Long,
        revurderingaarsak: Revurderingaarsak,
    ): List<Revurdering> =
        connectionAutoclosing.hentConnection {
            with(it) {
                val stmt =
                    prepareStatement(
                        """
                        SELECT b.*, s.sakType, s.enhet, s.fnr 
                        FROM behandling b
                        INNER JOIN sak s ON b.sak_id = s.id
                        WHERE sak_id = ? AND behandlingstype = 'REVURDERING'
                        AND revurdering_aarsak = ?
                        """.trimIndent(),
                    )

                stmt.setLong(1, sakid)
                stmt.setString(2, revurderingaarsak.name)
                stmt.executeQuery().toList { asRevurdering(this) }
            }
        }

    fun migrerStatusPaaAlleBehandlingerSomTrengerNyBeregning(saker: Saker): List<BehandlingOgSak> =
        connectionAutoclosing.hentConnection { connection ->
            with(connection) {
                val stmt =
                    prepareStatement(
                        """
                        UPDATE behandling
                        SET status = '${BehandlingStatus.TRYGDETID_OPPDATERT}'
                        WHERE status <> ALL (?)
                            AND sak_id = ANY (?)
                        RETURNING id, sak_id
                        """.trimIndent(),
                    )
                stmt.setArray(1, createArrayOf("text", BehandlingStatus.skalIkkeOmregnesVedGRegulering().toTypedArray()))
                stmt.setArray(2, createArrayOf("bigint", saker.saker.map { it.id }.toTypedArray()))

                stmt.executeQuery().toList { BehandlingOgSak(getUUID("id"), getLong("sak_id")) }
            }
        }

    fun hentAapneBehandlinger(saker: Saker): List<BehandlingOgSak> =
        connectionAutoclosing.hentConnection { connection ->
            with(connection) {
                val stmt =
                    prepareStatement(
                        """
                        SELECT id, sak_id FROM behandling
                        WHERE status = ANY (?)
                            AND sak_id = ANY (?)
                        """.trimIndent(),
                    )
                stmt.setArray(1, createArrayOf("text", BehandlingStatus.underBehandling().toTypedArray()))
                stmt.setArray(2, createArrayOf("bigint", saker.saker.map { it.id }.toTypedArray()))

                stmt.executeQuery().toList { BehandlingOgSak(getUUID("id"), getLong("sak_id")) }
            }
        }

    private fun asFoerstegangsbehandling(rs: ResultSet): Foerstegangsbehandling {
        val id = rs.getUUID("id")
        return Foerstegangsbehandling(
            id = id,
            sak = mapSak(rs),
            behandlingOpprettet = rs.somLocalDateTimeUTC("behandling_opprettet"),
            sistEndret = rs.somLocalDateTimeUTC("sist_endret"),
            soeknadMottattDato = rs.getTidspunktOrNull("soeknad_mottatt_dato")?.toLocalDatetimeUTC(),
            gyldighetsproeving = rs.getString("gyldighetssproving")?.let { objectMapper.readValue(it) },
            status = rs.getString("status").let { BehandlingStatus.valueOf(it) },
            virkningstidspunkt = rs.getString("virkningstidspunkt")?.let { objectMapper.readValue(it) },
            utlandstilknytning =
                rs
                    .getString("utlandstilknytning")
                    ?.let { objectMapper.readValue(it) },
            boddEllerArbeidetUtlandet =
                rs
                    .getString("bodd_eller_arbeidet_utlandet")
                    ?.let { objectMapper.readValue(it) },
            kommerBarnetTilgode = kommerBarnetTilGodeDao.hentKommerBarnetTilGode(id),
            prosesstype = rs.getString("prosesstype").let { Prosesstype.valueOf(it) },
            kilde = rs.getString("kilde").let { Vedtaksloesning.valueOf(it) },
            sendeBrev = rs.getBoolean("sende_brev"),
        )
    }

    private fun asRevurdering(rs: ResultSet) =
        revurderingDao.asRevurdering(
            rs,
            mapSak(rs),
        ) { i: UUID -> kommerBarnetTilGodeDao.hentKommerBarnetTilGode(i) }

    private fun mapSak(rs: ResultSet) =
        Sak(
            id = rs.getLong("sak_id"),
            sakType = enumValueOf(rs.getString("saktype")),
            ident = rs.getString("fnr"),
            enhet = rs.getString("enhet"),
        )

    fun opprettBehandling(behandling: OpprettBehandling) =
        connectionAutoclosing.hentConnection {
            with(it) {
                val stmt =
                    prepareStatement(
                        """
                        INSERT INTO behandling(id, sak_id, behandling_opprettet, sist_endret, status, behandlingstype, 
                        soeknad_mottatt_dato, virkningstidspunkt, utlandstilknytning, bodd_eller_arbeidet_utlandet, 
                        revurdering_aarsak, fritekst_aarsak, prosesstype, kilde, begrunnelse, relatert_behandling,
                        sende_brev, opphoer_fom)
                        VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """.trimIndent(),
                    )

                with(behandling) {
                    stmt.setObject(1, id)
                    stmt.setLong(2, sakId)
                    stmt.setTidspunkt(3, opprettet)
                    stmt.setTidspunkt(4, opprettet)
                    stmt.setString(5, status.name)
                    stmt.setString(6, type.name)
                    stmt.setTidspunkt(7, soeknadMottattDato?.toTidspunkt())
                    stmt.setString(8, virkningstidspunkt?.toJson())
                    stmt.setJsonb(9, utlandstilknytning)
                    stmt.setString(10, objectMapper.writeValueAsString(boddEllerArbeidetUtlandet))
                    stmt.setString(11, revurderingsAarsak?.name)
                    stmt.setString(12, fritekstAarsak)
                    stmt.setString(13, prosesstype.toString())
                    stmt.setString(14, kilde.toString())
                    stmt.setString(15, begrunnelse)
                    stmt.setString(16, relatertBehandlingId)
                    stmt.setBoolean(17, sendeBrev)
                    stmt.setString(18, opphoerFraOgMed?.let { fom -> objectMapper.writeValueAsString(fom) })
                }
                require(stmt.executeUpdate() == 1)
            }
        }

    fun lagreGyldighetsproeving(
        behandlingId: UUID,
        gyldighetsproeving: GyldighetsResultat?,
    ) = connectionAutoclosing.hentConnection {
        with(it) {
            val stmt =
                prepareStatement(
                    """
                    UPDATE behandling SET gyldighetssproving = ?, sist_endret = ?
                    WHERE id = ?
                    """.trimIndent(),
                )

            stmt.setObject(1, objectMapper.writeValueAsString(gyldighetsproeving))
            stmt.setTidspunkt(2, Tidspunkt.now().toLocalDatetimeUTC().toTidspunkt())
            stmt.setObject(3, behandlingId)
            require(stmt.executeUpdate() == 1)
        }
    }

    fun lagreStatus(lagretBehandling: Behandling) {
        lagreStatus(lagretBehandling.id, lagretBehandling.status, lagretBehandling.sistEndret)
    }

    fun lagreStatus(
        behandlingId: UUID,
        status: BehandlingStatus,
        sistEndret: LocalDateTime,
    ) = connectionAutoclosing.hentConnection {
        with(it) {
            val stmt = prepareStatement("UPDATE behandling SET status = ?, sist_endret = ? WHERE id = ?")

            stmt.setString(1, status.name)
            stmt.setTidspunkt(2, sistEndret.toTidspunkt())
            stmt.setObject(3, behandlingId)
            require(stmt.executeUpdate() == 1)
        }
    }

    fun lagreBoddEllerArbeidetUtlandet(
        behandlingId: UUID,
        boddEllerArbeidetUtlandet: BoddEllerArbeidetUtlandet,
    ) = connectionAutoclosing.hentConnection {
        with(it) {
            val stmt = prepareStatement("UPDATE behandling SET bodd_eller_arbeidet_utlandet = ? WHERE id = ?")

            stmt.setString(1, objectMapper.writeValueAsString(boddEllerArbeidetUtlandet))
            stmt.setObject(2, behandlingId)
            require(stmt.executeUpdate() == 1)
        }
    }

    fun lagreUtlandstilknytning(
        behandlingId: UUID,
        utlandstilknytning: Utlandstilknytning,
    ) = connectionAutoclosing.hentConnection {
        with(it) {
            val statement = prepareStatement("UPDATE behandling set utlandstilknytning = ? where id = ?")
            statement.setJsonb(1, utlandstilknytning)
            statement.setObject(2, behandlingId)
            require(statement.executeUpdate() == 1)
        }
    }

    fun lagreViderefoertOpphoer(
        behandlingId: UUID,
        viderefoertOpphoer: ViderefoertOpphoer,
    ) {
        lagreOpphoerFom(behandlingId, viderefoertOpphoer.dato)
        connectionAutoclosing.hentConnection {
            with(it) {
                val statement =
                    prepareStatement(
                        "INSERT INTO viderefoert_opphoer " +
                            "(dato, kilde, begrunnelse, kravdato, behandling_id, vilkaar) VALUES (?, ?, ?, ?, ?, ?)",
                    )
                statement.setString(1, objectMapper.writeValueAsString(viderefoertOpphoer.dato))
                statement.setJsonb(2, viderefoertOpphoer.kilde)
                statement.setString(3, viderefoertOpphoer.begrunnelse)
                statement.setDate(4, viderefoertOpphoer.kravdato?.let { d -> Date.valueOf(d) })
                statement.setObject(5, behandlingId)
                statement.setString(6, viderefoertOpphoer.vilkaar)
                statement.updateSuccessful()
            }
        }
    }

    fun hentViderefoertOpphoer(behandlingId: UUID) =
        connectionAutoclosing.hentConnection {
            with(it) {
                val statement =
                    prepareStatement("SELECT dato, kilde, begrunnelse, kravdato, vilkaar FROM viderefoert_opphoer WHERE behandling_id = ?")
                statement.setObject(1, behandlingId)
                statement.executeQuery().singleOrNull {
                    ViderefoertOpphoer(
                        dato = getString("dato").let { objectMapper.readValue<YearMonth>(it) },
                        kilde = getString("kilde").let { objectMapper.readValue(it) },
                        begrunnelse = getString("begrunnelse"),
                        kravdato = getDate("kravdato")?.let { it.toLocalDate() },
                        behandlingId = behandlingId,
                        vilkaar = getString("vilkaar"),
                    )
                }
            }
        }

    fun lagreSendeBrev(
        behandlingId: UUID,
        skalSendeBrev: Boolean,
    ) = connectionAutoclosing.hentConnection {
        with(it) {
            val statement = prepareStatement("UPDATE behandling set sende_brev = ? where id = ?")
            statement.setBoolean(1, skalSendeBrev)
            statement.setObject(2, behandlingId)
            require(statement.executeUpdate() == 1)
        }
    }

    private fun ResultSet.behandlingsListe(): List<Behandling> = toList { tilBehandling(getString("behandlingstype")) }.filterNotNull()

    private fun ResultSet.tilBehandling(key: String?) =
        when (key) {
            BehandlingType.FØRSTEGANGSBEHANDLING.name -> asFoerstegangsbehandling(this)
            BehandlingType.REVURDERING.name -> asRevurdering(this)
            else -> null
        }

    private fun ResultSet.behandlingAvRettType() = tilBehandling(getString("behandlingstype"))

    fun avbrytBehandling(behandlingId: UUID) =
        this.lagreStatus(
            behandlingId = behandlingId,
            status = BehandlingStatus.AVBRUTT,
            sistEndret = Tidspunkt.now().toLocalDatetimeUTC(),
        )

    fun lagreNyttVirkningstidspunkt(
        behandlingId: UUID,
        virkningstidspunkt: Virkningstidspunkt,
    ) = connectionAutoclosing.hentConnection {
        with(it) {
            val statement = prepareStatement("UPDATE behandling SET virkningstidspunkt = ? where id = ?")
            statement.setString(1, objectMapper.writeValueAsString(virkningstidspunkt))
            statement.setObject(2, behandlingId)
            statement.executeUpdate()
        }
    }

    fun lagreOpphoerFom(
        behandlingId: UUID,
        opphoerFraOgMed: YearMonth,
    ) = connectionAutoclosing.hentConnection {
        with(it) {
            val statement = prepareStatement("UPDATE behandling SET opphoer_fom = ? where id = ?")
            statement.setString(1, objectMapper.writeValueAsString(opphoerFraOgMed))
            statement.setObject(2, behandlingId)
            statement.executeUpdate()
        }
    }
}

fun ResultSet.somLocalDateTimeUTC(kolonne: String) = getTidspunkt(kolonne).toLocalDatetimeUTC()

val objectMapper: ObjectMapper =
    jacksonObjectMapper().registerModule(JavaTimeModule()).disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

fun PreparedStatement.updateSuccessful() = require(this.executeUpdate() == 1)
