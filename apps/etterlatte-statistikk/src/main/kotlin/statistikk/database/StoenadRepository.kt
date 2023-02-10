package no.nav.etterlatte.statistikk.database

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.tidspunkt.toTidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.toTimestamp
import no.nav.etterlatte.libs.database.toList
import no.nav.etterlatte.statistikk.domain.StoenadRad
import org.postgresql.util.PGobject
import java.sql.Date
import java.sql.PreparedStatement
import java.sql.Statement
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
                    getTimestamp("tekniskTid").toTidspunkt(),
                    getString("sakYtelse"),
                    getString("versjon"),
                    getString("saksbehandler"),
                    getString("attestant"),
                    getDate("vedtakLoependeFom").toLocalDate(),
                    getDate("vedtakLoependeTom")?.toLocalDate(),
                    objectMapper.readValue(getString("beregning"))
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
    setTimestamp(11, stoenadsrad.tekniskTid.toTimestamp())
    setString(12, stoenadsrad.sakYtelse)
    setString(13, stoenadsrad.versjon)
    setString(14, stoenadsrad.saksbehandler)
    setString(15, stoenadsrad.attestant)
    setDate(16, Date.valueOf(stoenadsrad.vedtakLoependeFom))
    setDate(17, stoenadsrad.vedtakLoependeTom?.let { Date.valueOf(it) })
    setJsonb(18, stoenadsrad?.beregning)
}

private object Queries {
    val insertMedPlaceholders = """INSERT INTO stoenad(
        |   fnrSoeker, fnrForeldre, fnrSoesken, anvendtTrygdetid, nettoYtelse, beregningType, anvendtSats, behandlingId,
        |   sakId, sakNummer, tekniskTid, sakYtelse, versjon, saksbehandler, attestant, vedtakLoependeFom, 
        |   vedtakLoependeTom, beregning
        |) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
    """.trimMargin()
    val fetchDatapakke = """SELECT id, fnrSoeker, fnrForeldre, 
        |   fnrSoesken, anvendtTrygdetid, nettoYtelse, beregningType, anvendtSats, behandlingId, sakId, sakNummer, 
        |   tekniskTid, sakYtelse, versjon, saksbehandler, attestant, vedtakLoependeFom, vedtakLoependeTom, beregning
        |FROM stoenad
    """.trimMargin()
}