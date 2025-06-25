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
import no.nav.etterlatte.libs.common.Enhetsnummer
import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.behandling.AarsakTilAvbrytelse
import no.nav.etterlatte.libs.common.behandling.BehandlingOpprinnelse
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.BoddEllerArbeidetUtlandet
import no.nav.etterlatte.libs.common.behandling.JaNei
import no.nav.etterlatte.libs.common.behandling.Prosesstype
import no.nav.etterlatte.libs.common.behandling.Revurderingaarsak
import no.nav.etterlatte.libs.common.behandling.TidligereFamiliepleier
import no.nav.etterlatte.libs.common.behandling.Utlandstilknytning
import no.nav.etterlatte.libs.common.behandling.Virkningstidspunkt
import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import no.nav.etterlatte.libs.common.feilhaandtering.krev
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.gyldigSoeknad.GyldighetsResultat
import no.nav.etterlatte.libs.common.person.AdressebeskyttelseGradering
import no.nav.etterlatte.libs.common.sak.BehandlingOgSak
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.getTidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.getTidspunktOrNull
import no.nav.etterlatte.libs.common.tidspunkt.setTidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.toLocalDatetimeUTC
import no.nav.etterlatte.libs.common.tidspunkt.toTidspunkt
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarType
import no.nav.etterlatte.libs.database.setJsonb
import no.nav.etterlatte.libs.database.setSakId
import no.nav.etterlatte.libs.database.singleOrNull
import no.nav.etterlatte.libs.database.toList
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
        SELECT b.*, i.omgjoering_sluttbehandling, s.sakType, s.enhet, s.adressebeskyttelse, s.erSkjermet, s.fnr 
        FROM behandling b
        INNER JOIN sak s ON b.sak_id = s.id
        LEFT JOIN behandling_info i ON b.id = i.behandling_id
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

    fun hentBehandlingerForSak(sakid: SakId): List<Behandling> =
        connectionAutoclosing.hentConnection {
            with(it) {
                val stmt =
                    prepareStatement(
                        """
                        $alleBehandlingerMedSak
                        WHERE sak_id = ?
                        """.trimIndent(),
                    )

                stmt.setSakId(1, sakid)
                stmt.executeQuery().behandlingsListe()
            }
        }

    fun hentAlleRevurderingerISakMedAarsak(
        sakid: SakId,
        revurderingaarsak: Revurderingaarsak,
    ): List<Revurdering> = hentAlleRevurderingerISakMedAarsak(sakid, listOf(revurderingaarsak))

    fun hentAlleRevurderingerISakMedAarsak(
        sakid: SakId,
        revurderingaarsaker: List<Revurderingaarsak>,
    ): List<Revurdering> =
        connectionAutoclosing.hentConnection {
            with(it) {
                val stmt =
                    prepareStatement(
                        """
                        $alleBehandlingerMedSak
                        WHERE sak_id = ? AND behandlingstype = 'REVURDERING'
                        AND revurdering_aarsak = ANY (?)
                        """.trimIndent(),
                    )

                stmt.setSakId(1, sakid)
                stmt.setArray(2, createArrayOf("text", revurderingaarsaker.toTypedArray()))
                stmt.executeQuery().toList { asRevurdering(this) }
            }
        }

    fun hentInnvilgaFoerstegangsbehandling(sakid: SakId): Foerstegangsbehandling? =
        connectionAutoclosing.hentConnection {
            with(it) {
                val stmt =
                    prepareStatement(
                        """
                        $alleBehandlingerMedSak
                        WHERE sak_id = ? AND behandlingstype = 'FØRSTEGANGSBEHANDLING'
                        AND status = 'IVERKSATT'
                        """.trimIndent(),
                    )

                stmt.setSakId(1, sakid)
                stmt.executeQuery().singleOrNull { asFoerstegangsbehandling(this) }
            }
        }

    fun migrerStatusPaaAlleBehandlingerSomTrengerNyBeregning(sakIder: List<SakId>): List<BehandlingOgSak> =
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
                stmt.setArray(
                    1,
                    createArrayOf("text", BehandlingStatus.skalIkkeOmregnesVedGRegulering().toTypedArray()),
                )
                stmt.setArray(2, createArrayOf("bigint", sakIder.toTypedArray()))

                stmt.executeQuery().toList { BehandlingOgSak(getUUID("id"), SakId(getLong("sak_id"))) }
            }
        }

    fun hentAapneBehandlinger(sakIder: List<SakId>): List<BehandlingOgSak> =
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
                stmt.setArray(2, createArrayOf("bigint", sakIder.toTypedArray()))

                stmt.executeQuery().toList { BehandlingOgSak(getUUID("id"), SakId(getLong("sak_id"))) }
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
            opprinnelse = rs.getString("opprinnelse").let { BehandlingOpprinnelse.valueOf(it) },
            vedtaksloesning = rs.getString("vedtaksloesning").let { Vedtaksloesning.valueOf(it) },
            sendeBrev = rs.getBoolean("sende_brev"),
            opphoerFraOgMed = rs.getString("opphoer_fom")?.let { objectMapper.readValue(it) },
            tidligereFamiliepleier =
                rs
                    .getString("tidligere_familiepleier")
                    ?.let { objectMapper.readValue(it) },
            erSluttbehandling = rs.getBoolean("omgjoering_sluttbehandling"),
            relatertBehandlingId = rs.getString("relatert_behandling"),
        )
    }

    private fun asRevurdering(rs: ResultSet) =
        revurderingDao.asRevurdering(
            rs,
            mapSak(rs),
        ) { i: UUID -> kommerBarnetTilGodeDao.hentKommerBarnetTilGode(i) }

    private fun mapSak(rs: ResultSet) =
        Sak(
            id = SakId(rs.getLong("sak_id")),
            sakType = enumValueOf(rs.getString("saktype")),
            ident = rs.getString("fnr"),
            enhet = Enhetsnummer(rs.getString("enhet")),
            adressebeskyttelse = rs.getString("adressebeskyttelse")?.let { enumValueOf<AdressebeskyttelseGradering>(it) },
            erSkjermet = rs.getBoolean("erSkjermet"),
        )

    fun opprettBehandling(behandling: OpprettBehandling) =
        connectionAutoclosing.hentConnection {
            with(it) {
                val stmt =
                    prepareStatement(
                        """
                        INSERT INTO behandling(id, sak_id, behandling_opprettet, sist_endret, status, behandlingstype, 
                        soeknad_mottatt_dato, virkningstidspunkt, utlandstilknytning, bodd_eller_arbeidet_utlandet, 
                        revurdering_aarsak, prosesstype, vedtaksloesning, begrunnelse, relatert_behandling,
                        sende_brev, opphoer_fom, tidligere_familiepleier, opprinnelse)
                        VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """.trimIndent(),
                    )

                with(behandling) {
                    stmt.setObject(1, id)
                    stmt.setSakId(2, sakId)
                    stmt.setTidspunkt(3, opprettet)
                    stmt.setTidspunkt(4, opprettet)
                    stmt.setString(5, status.name)
                    stmt.setString(6, type.name)
                    stmt.setTidspunkt(7, soeknadMottattDato?.toTidspunkt())
                    stmt.setString(8, virkningstidspunkt?.toJson())
                    stmt.setJsonb(9, utlandstilknytning)
                    stmt.setString(10, objectMapper.writeValueAsString(boddEllerArbeidetUtlandet))
                    stmt.setString(11, revurderingsAarsak?.name)
                    stmt.setString(12, prosesstype.toString())
                    stmt.setString(13, vedtaksloesning.toString())
                    stmt.setString(14, begrunnelse)
                    stmt.setString(15, relatertBehandlingId)
                    stmt.setBoolean(16, sendeBrev)
                    stmt.setString(17, opphoerFraOgMed?.let { fom -> objectMapper.writeValueAsString(fom) })
                    stmt.setJsonb(18, tidligereFamiliepleier)
                    stmt.setString(19, opprinnelse.name)
                }
                krev(stmt.executeUpdate() == 1) {
                    "Kunne ikke opprette behandling for ${behandling.id}"
                }
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
            krev(stmt.executeUpdate() == 1) {
                "Kunne ikke lagreGyldighetsproeving behandling for $behandlingId"
            }
        }
    }

    fun lagreStatus(lagretBehandling: Behandling) {
        if (lagretBehandling.status == BehandlingStatus.AVBRUTT) {
            throw InternfeilException(
                "Behandlinger skal ikke avbrytes med status direkte, siden vi da mangler " +
                    "utsending av statistikkmelding og lagring av behandlingshendelse.",
            )
        }
        lagreStatus(lagretBehandling.id, lagretBehandling.status, lagretBehandling.sistEndret)
    }

    private fun lagreStatus(
        behandlingId: UUID,
        status: BehandlingStatus,
        sistEndret: LocalDateTime,
    ) = connectionAutoclosing.hentConnection {
        with(it) {
            val stmt = prepareStatement("UPDATE behandling SET status = ?, sist_endret = ? WHERE id = ?")

            stmt.setString(1, status.name)
            stmt.setTidspunkt(2, sistEndret.toTidspunkt())
            stmt.setObject(3, behandlingId)
            krev(stmt.executeUpdate() == 1) {
                "Kunne ikke lagreStatus behandling for $behandlingId"
            }
        }
    }

    fun lagreAvbruttAarsak(
        behandlingId: UUID,
        aarsakTilAvbrytelse: AarsakTilAvbrytelse,
        kommentar: String,
    ) = connectionAutoclosing.hentConnection {
        with(it) {
            val stmt =
                prepareStatement("UPDATE behandling SET aarsak_til_avbrytelse = ?, kommentar_til_avbrytelse = ? WHERE id = ?")

            stmt.setString(1, aarsakTilAvbrytelse.name)
            stmt.setString(2, kommentar)
            stmt.setObject(3, behandlingId)
            krev(stmt.executeUpdate() == 1) {
                "Kunne ikke lagreStatus behandling for $behandlingId"
            }
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
            krev(stmt.executeUpdate() == 1) {
                "Kunne ikke lagreBoddEllerArbeidetUtlandet behandling for $behandlingId"
            }
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
            krev(statement.executeUpdate() == 1) {
                "Kunne ikke lagreUtlandstilknytning behandling for $behandlingId"
            }
        }
    }

    fun lagreTidligereFamiliepleier(
        behandlingId: UUID,
        tidligereFamiliepleier: TidligereFamiliepleier,
    ) = connectionAutoclosing.hentConnection {
        with(it) {
            val statement = prepareStatement("UPDATE behandling set tidligere_familiepleier = ? where id = ?")
            statement.setJsonb(1, tidligereFamiliepleier)
            statement.setObject(2, behandlingId)
            krev(statement.executeUpdate() == 1) {
                "Kunne ikke lagreTidligereFamiliepleier behandling for $behandlingId"
            }
        }
    }

    fun hentTidligereFamiliepleier(behandlingId: UUID): TidligereFamiliepleier? =
        connectionAutoclosing.hentConnection {
            with(it) {
                val statement = prepareStatement("SELECT tidligere_familiepleier FROM behandling where id = ?")
                statement.setObject(1, behandlingId)
                statement.executeQuery().singleOrNull {
                    toTidligereFamiliepleier()
                }
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
                            "(skalViderefoere, dato, kilde, begrunnelse, behandling_id, vilkaar, aktiv) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?)" +
                            "ON CONFLICT (behandling_id) where aktiv = true " +
                            "DO UPDATE SET " +
                            "skalViderefoere=excluded.skalViderefoere, " +
                            "dato=excluded.dato, " +
                            "kilde=excluded.kilde, " +
                            "begrunnelse=excluded.begrunnelse, " +
                            "behandling_id=excluded.behandling_id, " +
                            "vilkaar=excluded.vilkaar, " +
                            "aktiv=excluded.aktiv, " +
                            "sist_endret=CURRENT_TIMESTAMP",
                    )
                statement.setString(1, viderefoertOpphoer.skalViderefoere.name)
                statement.setString(2, objectMapper.writeValueAsString(viderefoertOpphoer.dato))
                statement.setJsonb(3, viderefoertOpphoer.kilde)
                statement.setString(4, viderefoertOpphoer.begrunnelse)
                statement.setObject(5, behandlingId)
                statement.setString(6, viderefoertOpphoer.vilkaar?.name)
                statement.setBoolean(7, viderefoertOpphoer.aktiv)

                krev(statement.executeUpdate() == 1) {
                    "Feil ved lagring av videreført opphør"
                }
            }
        }
    }

    fun fjernViderefoertOpphoer(
        behandlingId: UUID,
        kilde: Grunnlagsopplysning.Kilde,
    ) {
        lagreOpphoerFom(behandlingId, null)
        connectionAutoclosing.hentConnection {
            with(it) {
                val statement =
                    prepareStatement(
                        "UPDATE viderefoert_opphoer " +
                            "SET aktiv = false, " +
                            "kilde = ?, " +
                            "sist_endret = CURRENT_TIMESTAMP " +
                            "WHERE behandling_id = ? " +
                            "AND aktiv = true",
                    )
                statement.setJsonb(1, kilde)
                statement.setObject(2, behandlingId)

                krev(statement.executeUpdate() == 1) {
                    "Feil ved fjerning av videreført opphør"
                }
            }
        }
    }

    fun hentViderefoertOpphoer(behandlingId: UUID) =
        connectionAutoclosing.hentConnection {
            with(it) {
                val statement =
                    prepareStatement(
                        "SELECT skalViderefoere, dato, kilde, begrunnelse, vilkaar, aktiv " +
                            "FROM viderefoert_opphoer " +
                            "WHERE behandling_id = ? " +
                            "AND aktiv = TRUE",
                    )
                statement.setObject(1, behandlingId)
                statement.executeQuery().singleOrNull {
                    ViderefoertOpphoer(
                        skalViderefoere = JaNei.valueOf(getString("skalViderefoere")),
                        dato = getString("dato").let { objectMapper.readValue<YearMonth?>(it) },
                        kilde = getString("kilde").let { objectMapper.readValue(it) },
                        begrunnelse = getString("begrunnelse"),
                        behandlingId = behandlingId,
                        vilkaar = getString("vilkaar")?.let { VilkaarType.valueOf(it) },
                        aktiv = getBoolean("aktiv"),
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
            krev(statement.executeUpdate() == 1) {
                "Kunne ikke send brev behandling for $behandlingId"
            }
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

    fun avbrytBehandling(
        behandlingId: UUID,
        aarsakTilAvbrytelse: AarsakTilAvbrytelse?,
        kommentar: String?,
    ) {
        this.lagreStatus(
            behandlingId = behandlingId,
            status = BehandlingStatus.AVBRUTT,
            sistEndret = Tidspunkt.now().toLocalDatetimeUTC(),
        )

        if (aarsakTilAvbrytelse != null) {
            this.lagreAvbruttAarsak(behandlingId, aarsakTilAvbrytelse, kommentar ?: "")
        }
    }

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
        opphoerFraOgMed: YearMonth?,
    ) = connectionAutoclosing.hentConnection {
        with(it) {
            val statement = prepareStatement("UPDATE behandling SET opphoer_fom = ? where id = ?")
            statement.setString(1, objectMapper.writeValueAsString(opphoerFraOgMed))
            statement.setObject(2, behandlingId)
            statement.executeUpdate()
        }
    }

    fun endreProsesstype(
        behandlingId: UUID,
        ny: Prosesstype,
    ) = connectionAutoclosing.hentConnection {
        with(it) {
            val statement = prepareStatement("UPDATE behandling SET prosesstype = ? WHERE id = ?")
            statement.setString(1, ny.name)
            statement.setObject(2, behandlingId)
            statement.executeUpdate()
        }
    }
}

fun ResultSet.somLocalDateTimeUTC(kolonne: String) = getTidspunkt(kolonne).toLocalDatetimeUTC()

private fun ResultSet.toTidligereFamiliepleier(): TidligereFamiliepleier? =
    this.getString("tidligere_familiepleier")?.let {
        objectMapper.readValue(it)
    }

val objectMapper: ObjectMapper =
    jacksonObjectMapper().registerModule(JavaTimeModule()).disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
