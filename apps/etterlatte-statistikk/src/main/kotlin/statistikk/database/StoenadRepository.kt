package no.nav.etterlatte.statistikk.database

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.tidspunkt.toNorskTidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.toTidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.toTimestamp
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import no.nav.etterlatte.libs.database.toList
import no.nav.etterlatte.statistikk.domain.MaanedStoenadRad
import no.nav.etterlatte.statistikk.domain.StoenadRad
import org.postgresql.util.PGobject
import java.sql.Date
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Statement
import java.time.LocalTime
import java.time.YearMonth
import java.util.*
import javax.sql.DataSource

class StoenadRepository(private val datasource: DataSource) {
    private val connection get() = datasource.connection

    companion object {
        fun using(datasource: DataSource): StoenadRepository {
            return StoenadRepository(datasource)
        }
    }

    fun datapakke(): List<StoenadRad> {
        return connection.use {
            it.prepareStatement(
                """
                SELECT id, fnrSoeker, fnrForeldre, 
                    fnrSoesken, anvendtTrygdetid, nettoYtelse, beregningType, anvendtSats, behandlingId, sakId, 
                    sakNummer, tekniskTid, sakYtelse, versjon, saksbehandler, attestant, vedtakLoependeFom, 
                    vedtakLoependeTom, beregning, vedtakType
                FROM stoenad
                """.trimIndent()
            ).executeQuery().toList {
                asStoenadRad()
            }
        }
    }

    fun lagreMaanedStatistikkRad(maanedStatistikkRad: MaanedStoenadRad) {
        return connection.use { conn ->
            maanedStatistikkRad
            conn.prepareStatement(
                """
                INSERT INTO maaned_stoenad(
                    fnrSoeker, fnrForeldre, fnrSoesken, anvendtTrygdetid, nettoYtelse, beregningType, anvendtSats, 
                    behandlingId, sakId, tekniskTid, sakYtelse, versjon, saksbehandler, attestant, 
                    vedtakLoependeFom, vedtakLoependeTom, statistikkMaaned
                ) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent()
            ).apply {
                setString(1, maanedStatistikkRad.fnrSoeker)
                setJsonb(2, maanedStatistikkRad.fnrForeldre)
                setJsonb(3, maanedStatistikkRad.fnrSoesken)
                setString(4, maanedStatistikkRad.anvendtTrygdetid)
                setString(5, maanedStatistikkRad.nettoYtelse)
                setString(6, maanedStatistikkRad.beregningType)
                setString(7, maanedStatistikkRad.anvendtSats)
                setObject(8, maanedStatistikkRad.behandlingId)
                setLong(9, maanedStatistikkRad.sakId)
                setTimestamp(10, maanedStatistikkRad.tekniskTid.toTimestamp())
                setString(11, maanedStatistikkRad.sakYtelse)
                setString(12, maanedStatistikkRad.versjon)
                setString(13, maanedStatistikkRad.saksbehandler)
                setString(14, maanedStatistikkRad.attestant)
                setDate(15, Date.valueOf(maanedStatistikkRad.vedtakLoependeFom))
                setDate(16, maanedStatistikkRad.vedtakLoependeTom?.let { Date.valueOf(it) })
                setString(17, maanedStatistikkRad.statistikkMaaned.toString())
            }.executeUpdate()
        }
    }

    fun lagreStoenadsrad(stoenadsrad: StoenadRad): StoenadRad? {
        connection.use { conn ->
            val (statement, insertedRows) = conn.prepareStatement(
                """
                INSERT INTO stoenad(
                    fnrSoeker, fnrForeldre, fnrSoesken, anvendtTrygdetid, nettoYtelse, beregningType, anvendtSats, 
                    behandlingId, sakId, sakNummer, tekniskTid, sakYtelse, versjon, saksbehandler, attestant, 
                    vedtakLoependeFom, vedtakLoependeTom, beregning, vedtakType
                ) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent(),
                Statement.RETURN_GENERATED_KEYS
            ).apply {
                setStoenadRad(stoenadsrad)
            }.let {
                it to it.executeUpdate()
            }
            if (insertedRows == 0) {
                return null
            }
            statement.generatedKeys.use { rs ->
                if (rs.next()) {
                    return stoenadsrad.copy(id = rs.getLong(1))
                }
                return null
            }
        }
    }

    fun hentRaderInnenforMaaned(maaned: YearMonth): List<StoenadRad> {
        return connection.use { conn ->
            conn.prepareStatement(
                """
                SELECT * FROM stoenad 
                WHERE vedtakLoependeFom <= ? AND COALESCE(vedtakLoependeTom, ?) >= ? 
                    AND tekniskTid <= ?
                """.trimIndent()
            ).apply {
                setDate(1, Date.valueOf(maaned.atEndOfMonth()))
                setDate(2, Date.valueOf(maaned.atEndOfMonth()))
                setDate(3, Date.valueOf(maaned.atEndOfMonth()))
                setTimestamp(4, maaned.atEndOfMonth().atTime(LocalTime.MAX).toNorskTidspunkt().toTimestamp())
            }.executeQuery().toList { asStoenadRad() }
        }
    }

    fun kjoertStatusForMaanedsstatistikk(maaned: YearMonth): KjoertStatus {
        return connection.use { conn ->
            conn.prepareStatement(
                """
                SELECT id, statistikkMaaned, kjoertStatus, raderRegistrert, raderMedFeil 
                FROM maanedsstatistikk_job 
                WHERE statistikkMaaned = ?
                """.trimIndent()
            )
                .apply {
                    setString(1, maaned.toString())
                }
                .executeQuery().toList {
                    MaanedstatistikkJobExecution(
                        id = getLong("id"),
                        statistikkMaaned = YearMonth.parse(getString("statistikkMaaned")),
                        kjoertStatus = KjoertStatus.valueOf(getString("kjoertStatus")),
                        raderRegistrert = getLong("raderRegistrert"),
                        raderMedFeil = getLong("raderMedFeil")
                    )
                }.map { it.kjoertStatus }.toSet()
                .let {
                    if (it.contains(KjoertStatus.INGEN_FEIL)) {
                        KjoertStatus.INGEN_FEIL
                    } else if (it.contains(KjoertStatus.FEIL)) {
                        KjoertStatus.FEIL
                    } else {
                        KjoertStatus.IKKE_KJOERT
                    }
                }
        }
    }

    fun lagreMaanedJobUtfoert(
        maaned: YearMonth,
        raderMedFeil: Long,
        raderRegistrert: Long
    ) {
        val kjoertStatus = when (raderMedFeil) {
            0L -> KjoertStatus.INGEN_FEIL
            else -> KjoertStatus.FEIL
        }
        connection.use {
            it.prepareStatement(
                """
                INSERT INTO maanedsstatistikk_job (
                    statistikkMaaned, kjoertStatus, raderRegistrert, raderMedFeil
                ) VALUES (?, ?, ?, ?)
                """.trimIndent()
            )
                .apply {
                    setString(1, maaned.toString())
                    setString(2, kjoertStatus.toString())
                    setLong(3, raderRegistrert)
                    setLong(4, raderMedFeil)
                }.executeUpdate()
        }
    }
}

data class MaanedstatistikkJobExecution(
    val id: Long,
    val statistikkMaaned: YearMonth,
    val kjoertStatus: KjoertStatus,
    val raderRegistrert: Long,
    val raderMedFeil: Long
)

enum class KjoertStatus {
    INGEN_FEIL, FEIL, IKKE_KJOERT
}

inline fun <reified T> PreparedStatement.setJsonb(parameterIndex: Int, jsonb: T): PreparedStatement {
    val jsonObject = PGobject()
    jsonObject.type = "json"
    jsonObject.value = objectMapper.writeValueAsString(jsonb)
    this.setObject(parameterIndex, jsonObject)
    return this
}

private fun PreparedStatement.setStoenadRad(stoenadsrad: StoenadRad): PreparedStatement = this.apply {
    setString(1, stoenadsrad.fnrSoeker)
    setJsonb(2, stoenadsrad.fnrForeldre)
    setJsonb(3, stoenadsrad.fnrSoesken)
    setString(4, stoenadsrad.anvendtTrygdetid)
    setString(5, stoenadsrad.nettoYtelse)
    setString(6, stoenadsrad.beregningType)
    setString(7, stoenadsrad.anvendtSats)
    setObject(8, stoenadsrad.behandlingId)
    setLong(9, stoenadsrad.sakId)
    setLong(10, stoenadsrad.sakId)
    setTimestamp(11, stoenadsrad.tekniskTid.toTimestamp())
    setString(12, stoenadsrad.sakYtelse)
    setString(13, stoenadsrad.versjon)
    setString(14, stoenadsrad.saksbehandler)
    setString(15, stoenadsrad.attestant)
    setDate(16, Date.valueOf(stoenadsrad.vedtakLoependeFom))
    setDate(17, stoenadsrad.vedtakLoependeTom?.let { Date.valueOf(it) })
    setJsonb(18, stoenadsrad.beregning)
    setString(19, stoenadsrad.vedtakType?.toString())
}

private fun ResultSet.asStoenadRad(): StoenadRad = StoenadRad(
    id = getLong("id"),
    fnrSoeker = getString("fnrSoeker"),
    fnrForeldre = objectMapper.readValue(getString("fnrForeldre"), Array<String>::class.java).toList(),
    fnrSoesken = objectMapper.readValue(getString("fnrSoesken"), Array<String>::class.java).toList(),
    anvendtTrygdetid = getString("anvendtTrygdetid"),
    nettoYtelse = getString("nettoYtelse"),
    beregningType = getString("beregningType"),
    anvendtSats = getString("anvendtSats"),
    behandlingId = getObject("behandlingId") as UUID,
    sakId = getLong("sakId"),
    sakNummer = getLong("sakNummer"),
    tekniskTid = getTimestamp("tekniskTid").toTidspunkt(),
    sakYtelse = getString("sakYtelse"),
    versjon = getString("versjon"),
    saksbehandler = getString("saksbehandler"),
    attestant = getString("attestant"),
    vedtakLoependeFom = getDate("vedtakLoependeFom").toLocalDate(),
    vedtakLoependeTom = getDate("vedtakLoependeTom")?.toLocalDate(),
    beregning = getString("beregning")?.let { objectMapper.readValue(it) },
    vedtakType = getString("vedtakType")?.let { enumValueOf<VedtakType>(it) }
)