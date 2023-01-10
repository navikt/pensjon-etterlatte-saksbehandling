package no.nav.etterlatte.statistikk.database

import no.nav.etterlatte.libs.common.tidspunkt.toTidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.toTimestamp
import no.nav.etterlatte.statistikk.sak.BehandlingMetode
import no.nav.etterlatte.statistikk.sak.BehandlingResultat
import no.nav.etterlatte.statistikk.sak.SakRad
import no.nav.etterlatte.statistikk.sak.SakUtland
import no.nav.etterlatte.statistikk.sak.SoeknadFormat
import java.sql.Date
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Statement
import java.util.*
import javax.sql.DataSource

class SakstatistikkRepository(private val datasource: DataSource) {

    private val connection get() = datasource.connection

    companion object {
        fun using(datasource: DataSource): SakstatistikkRepository {
            return SakstatistikkRepository(datasource)
        }
    }

    fun lagreRad(sakRad: SakRad): SakRad? {
        connection.use { connection ->
            val (statement, insertedRows) = connection.prepareStatement(
                """
                INSERT INTO sak (
                    behandling_id, sak_id, mottatt_tid, registrert_tid, ferdigbehandlet_tid, vedtak_tid, 
                    behandling_type, behandling_status, behandling_resultat, resultat_begrunnelse, behandling_metode, 
                    opprettet_av, ansvarlig_beslutter, aktor_id, dato_foerste_utbetaling, teknisk_tid, sak_ytelse, 
                    vedtak_loepende_fom, vedtak_loepende_tom, saksbehandler, ansvarlig_enhet, soeknad_format, 
                    sak_utland
                ) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent(),
                Statement.RETURN_GENERATED_KEYS
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

    private fun ResultSet.tilSakRad(): SakRad = SakRad(
        id = getLong("id"),
        behandlingId = getObject("behandling_id") as UUID,
        sakId = getLong("sak_id"),
        mottattTidspunkt = getTimestamp("mottatt_tid").toTidspunkt(),
        registrertTidspunkt = getTimestamp("registrert_tid").toTidspunkt(),
        ferdigbehandletTidspunkt = getTimestamp("ferdigbehandlet_tid")?.toTidspunkt(),
        vedtakTidspunkt = getTimestamp("vedtak_tid")?.toTidspunkt(),
        behandlingType = enumValueOf(getString("behandling_type")),
        behandlingStatus = getString("behandling_status"),
        behandlingResultat = getString("behandling_resultat")?.let { enumValueOf<BehandlingResultat>(it) },
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
        ansvarligEnhet = getString("ansvarlig_enhet"),
        soeknadFormat = getString("soeknad_format")?.let { enumValueOf<SoeknadFormat>(it) },
        sakUtland = getString("sak_utland")?.let { enumValueOf<SakUtland>(it) }
    )

    fun hentRader(): List<SakRad> {
        val statement = connection.prepareStatement(
            """
            SELECT id, behandling_id, sak_id, mottatt_tid, registrert_tid, ferdigbehandlet_tid, vedtak_tid,
                behandling_type, behandling_status, behandling_resultat, resultat_begrunnelse, behandling_metode,
                opprettet_av, ansvarlig_beslutter, aktor_id, dato_foerste_utbetaling, teknisk_tid, sak_ytelse,
                vedtak_loepende_fom, vedtak_loepende_tom, saksbehandler, ansvarlig_enhet, soeknad_format, sak_utland
            FROM sak
            """.trimIndent()
        )
        return statement.executeQuery().toList { tilSakRad() }
    }
}

private fun PreparedStatement.setSakRad(sakRad: SakRad): PreparedStatement = this.apply {
    setObject(1, sakRad.behandlingId)
    setLong(2, sakRad.sakId)
    setTimestamp(3, sakRad.mottattTidspunkt.toTimestamp())
    setTimestamp(4, sakRad.registrertTidspunkt.toTimestamp())
    setTimestamp(5, sakRad.ferdigbehandletTidspunkt?.toTimestamp())
    setTimestamp(6, sakRad.vedtakTidspunkt?.toTimestamp())
    setString(7, sakRad.behandlingType.name)
    setString(8, sakRad.behandlingStatus)
    setString(9, sakRad.behandlingResultat?.name)
    setString(10, sakRad.resultatBegrunnelse)
    setString(11, sakRad.behandlingMetode?.name)
    setString(12, sakRad.opprettetAv)
    setString(13, sakRad.ansvarligBeslutter)
    setString(14, sakRad.aktorId)
    setDate(15, sakRad.datoFoersteUtbetaling?.let { Date.valueOf(it) })
    setTimestamp(16, sakRad.tekniskTid.toTimestamp())
    setString(17, sakRad.sakYtelse)
    setDate(18, sakRad.vedtakLoependeFom?.let { Date.valueOf(it) })
    setDate(19, sakRad.vedtakLoependeTom?.let { Date.valueOf(it) })
    setString(20, sakRad.saksbehandler)
    setString(21, sakRad.ansvarligEnhet)
    setString(22, sakRad.soeknadFormat?.name)
    setString(23, sakRad.sakUtland?.name)
}