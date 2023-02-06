package no.nav.etterlatte.statistikk.database

import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.database.toList
import org.postgresql.util.PGobject
import java.sql.Date
import java.sql.PreparedStatement
import java.sql.Statement
import java.sql.Timestamp
import java.time.Instant
import java.time.LocalDate
import java.util.*
import javax.sql.DataSource

class StatistikkRepository(private val datasource: DataSource) {
    private val connection get() = datasource.connection

    companion object {
        fun using(datasource: DataSource): StatistikkRepository {
            return StatistikkRepository(datasource)
        }
    }

    fun datapakke(): List<StoenadRad> {
        return connection.use {
            it.prepareStatement(Queries.fetchDatapakke).executeQuery().toList {
                StoenadRad(
                    getLong("id"),
                    getString("fnrSoeker"),
                    objectMapper.readValue(getString("fnrForeldre"), Array<String>::class.java).toList(),
                    objectMapper.readValue(getString("fnrSoesken"), Array<String>::class.java).toList(),
                    getString("anvendtTrygdetid"),
                    getString("nettoYtelse"),
                    getString("beregningType"),
                    getString("anvendtSats"),
                    getObject("behandlingId") as UUID,
                    getLong("sakId"),
                    getLong("sakNummer"),
                    getTimestamp("tekniskTid").toInstant(),
                    getString("sakYtelse"),
                    getString("versjon"),
                    getString("saksbehandler"),
                    getString("attestant"),
                    getDate("vedtakLoependeFom").toLocalDate(),
                    getDate("vedtakLoependeTom")?.toLocalDate()
                )
            }
        }
    }

    fun lagreStoenadsrad(stoenadsrad: StoenadRad): StoenadRad? {
        connection.use { conn ->
            val (statement, insertedRows) = conn.prepareStatement(
                Queries.insertMedPlaceholders,
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
    setTimestamp(11, Timestamp.from(stoenadsrad.tekniskTid))
    setString(12, stoenadsrad.sakYtelse)
    setString(13, stoenadsrad.versjon)
    setString(14, stoenadsrad.saksbehandler)
    setString(15, stoenadsrad.attestant)
    setDate(16, Date.valueOf(stoenadsrad.vedtakLoependeFom))
    setDate(17, stoenadsrad.vedtakLoependeTom?.let { Date.valueOf(it) })
}

private object Queries {
    val insertMedPlaceholders = """INSERT INTO stoenad(
        |   fnrSoeker, fnrForeldre, fnrSoesken, anvendtTrygdetid, nettoYtelse, beregningType, anvendtSats, behandlingId,
        |   sakId, sakNummer, tekniskTid, sakYtelse, versjon, saksbehandler, attestant, vedtakLoependeFom, vedtakLoependeTom
        |) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
    """.trimMargin()
    val fetchDatapakke = """SELECT id, fnrSoeker, fnrForeldre, 
        |   fnrSoesken, anvendtTrygdetid, nettoYtelse, beregningType, anvendtSats, behandlingId, sakId, sakNummer, 
        |   tekniskTid, sakYtelse, versjon, saksbehandler, attestant, vedtakLoependeFom, vedtakLoependeTom 
        |FROM stoenad
    """.trimMargin()
}

data class StoenadRad(
    val id: Long,
    val fnrSoeker: String,
    val fnrForeldre: List<String>,
    val fnrSoesken: List<String>,
    val anvendtTrygdetid: String,
    val nettoYtelse: String,
    val beregningType: String,
    val anvendtSats: String,
    val behandlingId: UUID,
    val sakId: Long,
    val sakNummer: Long,
    val tekniskTid: Instant,
    val sakYtelse: String,
    val versjon: String,
    val saksbehandler: String,
    val attestant: String?,
    val vedtakLoependeFom: LocalDate,
    val vedtakLoependeTom: LocalDate?
)