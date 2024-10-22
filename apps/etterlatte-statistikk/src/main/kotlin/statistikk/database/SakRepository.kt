package no.nav.etterlatte.statistikk.database

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.etterlatte.libs.common.Enhetsnummer
import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.behandling.PaaVentAarsak
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.tidspunkt.setTidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.toTidspunkt
import no.nav.etterlatte.libs.database.setSakId
import no.nav.etterlatte.libs.database.toList
import no.nav.etterlatte.statistikk.domain.BehandlingMetode
import no.nav.etterlatte.statistikk.domain.SakRad
import no.nav.etterlatte.statistikk.domain.SakUtland
import no.nav.etterlatte.statistikk.domain.SakYtelsesgruppe
import no.nav.etterlatte.statistikk.domain.SoeknadFormat
import java.sql.Date
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Statement
import java.sql.Types
import java.util.UUID
import javax.sql.DataSource

class SakRepository(
    private val datasource: DataSource,
) {
    companion object {
        fun using(datasource: DataSource): SakRepository = SakRepository(datasource)
    }

    fun lagreRad(sakRad: SakRad): SakRad? {
        datasource.connection.use { connection ->
            val (statement, insertedRows) =
                connection
                    .prepareStatement(
                        """
                        INSERT INTO sak (
                            behandling_id, sak_id, mottatt_tid, registrert_tid, ferdigbehandlet_tid, vedtak_tid, 
                            behandling_type, behandling_status, behandling_resultat, resultat_begrunnelse, behandling_metode, 
                            opprettet_av, ansvarlig_beslutter, aktor_id, dato_foerste_utbetaling, teknisk_tid, sak_ytelse, 
                            vedtak_loepende_fom, vedtak_loepende_tom, saksbehandler, ansvarlig_enhet, soeknad_format, 
                            sak_utland, beregning, sak_ytelsesgruppe, avdoede_foreldre, revurdering_aarsak, avkorting,
                            kilde, pesysid, relatert_til, paa_vent_aarsak
                        ) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """.trimIndent(),
                        Statement.RETURN_GENERATED_KEYS,
                    ).apply {
                        setSakRad(sakRad)
                    }.let {
                        it to it.executeUpdate()
                    }
            if (insertedRows == 0) {
                return null
            }
            statement.generatedKeys.use {
                if (it.next()) {
                    return sakRad.copy(id = it.getLong(1))
                }
                return null
            }
        }
    }

    private fun ResultSet.tilSakRad(): SakRad =
        SakRad(
            id = getLong("id"),
            referanseId = getObject("behandling_id") as UUID,
            sakId = SakId(getLong("sak_id")),
            mottattTidspunkt = getTimestamp("mottatt_tid").toTidspunkt(),
            registrertTidspunkt = getTimestamp("registrert_tid").toTidspunkt(),
            ferdigbehandletTidspunkt = getTimestamp("ferdigbehandlet_tid")?.toTidspunkt(),
            vedtakTidspunkt = getTimestamp("vedtak_tid")?.toTidspunkt(),
            type = getString("behandling_type"),
            status = getString("behandling_status"),
            resultat = getString("behandling_resultat"),
            resultatBegrunnelse = getString("resultat_begrunnelse"),
            behandlingMetode = getString("behandling_metode")?.let { enumValueOf<BehandlingMetode>(it) },
            opprettetAv = getString("opprettet_av"),
            ansvarligBeslutter = getString("ansvarlig_beslutter"),
            aktorId = getString("aktor_id"),
            datoFoersteUtbetaling = getDate("dato_foerste_utbetaling")?.toLocalDate(),
            tekniskTid = getTimestamp("teknisk_tid").toTidspunkt(),
            sakYtelse = getString("sak_ytelse"),
            vedtakLoependeFom = getDate("vedtak_loepende_fom")?.toLocalDate(),
            vedtakLoependeTom = getDate("vedtak_loepende_tom")?.toLocalDate(),
            saksbehandler = getString("saksbehandler"),
            ansvarligEnhet = Enhetsnummer.nullable(getString("ansvarlig_enhet")),
            soeknadFormat = getString("soeknad_format")?.let { enumValueOf<SoeknadFormat>(it) },
            sakUtland = getString("sak_utland")?.let { enumValueOf<SakUtland>(it) },
            beregning = getString("beregning")?.let { objectMapper.readValue(it) },
            sakYtelsesgruppe = getString("sak_ytelsesgruppe")?.let { enumValueOf<SakYtelsesgruppe>(it) },
            avdoedeForeldre = getString("avdoede_foreldre")?.let { objectMapper.readValue(it) },
            revurderingAarsak = getString("revurdering_aarsak"),
            avkorting = getString("avkorting")?.let { objectMapper.readValue(it) },
            kilde = getString("kilde").let { Vedtaksloesning.valueOf(it) },
            pesysId = getLong("pesysid"),
            relatertTil = getString("relatert_til"),
            paaVentAarsak = getString("paa_vent_aarsak")?.let { enumValueOf<PaaVentAarsak>(it) },
        )

    fun hentRader(): List<SakRad> =
        datasource.connection.use { connection ->
            val statement =
                connection.prepareStatement(
                    """
                    SELECT id, behandling_id, sak_id, mottatt_tid, registrert_tid, ferdigbehandlet_tid, vedtak_tid,
                        behandling_type, behandling_status, behandling_resultat, resultat_begrunnelse, behandling_metode,
                        opprettet_av, ansvarlig_beslutter, aktor_id, dato_foerste_utbetaling, teknisk_tid, sak_ytelse,
                        vedtak_loepende_fom, vedtak_loepende_tom, saksbehandler, ansvarlig_enhet, soeknad_format, sak_utland,
                        beregning, sak_ytelsesgruppe, avdoede_foreldre, revurdering_aarsak, avkorting, kilde, pesysid,
                        relatert_til, paa_vent_aarsak
                    FROM sak
                    """.trimIndent(),
                )
            statement.executeQuery().toList { tilSakRad() }
        }

    fun hentSisteRad(behandlingId: UUID): SakRad? =
        datasource.connection.use { connection ->
            val statement =
                connection
                    .prepareStatement(
                        """
                        SELECT id, behandling_id, sak_id, mottatt_tid, registrert_tid, ferdigbehandlet_tid, vedtak_tid,
                            behandling_type, behandling_status, behandling_resultat, resultat_begrunnelse, behandling_metode,
                            opprettet_av, ansvarlig_beslutter, aktor_id, dato_foerste_utbetaling, teknisk_tid, sak_ytelse,
                            vedtak_loepende_fom, vedtak_loepende_tom, saksbehandler, ansvarlig_enhet, soeknad_format, sak_utland,
                            beregning, sak_ytelsesgruppe, avdoede_foreldre, revurdering_aarsak, avkorting, kilde, pesysid,
                            relatert_til, paa_vent_aarsak
                        FROM sak where behandling_id = ? order by id desc 
                        """.trimIndent(),
                    ).apply {
                        setObject(1, behandlingId)
                    }
            statement.executeQuery().toList { tilSakRad() }.firstOrNull()
        }
}

private fun PreparedStatement.setSakRad(sakRad: SakRad): PreparedStatement =
    this.apply {
        setObject(1, sakRad.referanseId)
        setSakId(2, sakRad.sakId)
        setTidspunkt(3, sakRad.mottattTidspunkt)
        setTidspunkt(4, sakRad.registrertTidspunkt)
        setTidspunkt(5, sakRad.ferdigbehandletTidspunkt)
        setTidspunkt(6, sakRad.vedtakTidspunkt)
        setString(7, sakRad.type)
        setString(8, sakRad.status)
        setString(9, sakRad.resultat)
        setString(10, sakRad.resultatBegrunnelse)
        setString(11, sakRad.behandlingMetode?.name)
        setString(12, sakRad.opprettetAv)
        setString(13, sakRad.ansvarligBeslutter)
        setString(14, sakRad.aktorId)
        setDate(15, sakRad.datoFoersteUtbetaling?.let { Date.valueOf(it) })
        setTidspunkt(16, sakRad.tekniskTid)
        setString(17, sakRad.sakYtelse)
        setDate(18, sakRad.vedtakLoependeFom?.let { Date.valueOf(it) })
        setDate(19, sakRad.vedtakLoependeTom?.let { Date.valueOf(it) })
        setString(20, sakRad.saksbehandler)
        setString(21, sakRad.ansvarligEnhet?.enhetNr)
        setString(22, sakRad.soeknadFormat?.name)
        setString(23, sakRad.sakUtland?.name)
        setJsonb(24, sakRad.beregning)
        setString(25, sakRad.sakYtelsesgruppe?.name)
        setJsonb(26, sakRad.avdoedeForeldre)
        setString(27, sakRad.revurderingAarsak)
        setJsonb(28, sakRad.avkorting)
        setString(29, sakRad.kilde.name)
        sakRad.pesysId?.let { setLong(30, it) } ?: setNull(30, Types.BIGINT)
        sakRad.relatertTil?.let { setString(31, it) } ?: setNull(31, Types.CHAR)
        sakRad.paaVentAarsak?.let { setString(32, it.name) } ?: setString(32, null)
    }
