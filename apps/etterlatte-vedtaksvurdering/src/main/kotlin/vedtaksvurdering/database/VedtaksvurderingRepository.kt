package no.nav.etterlatte.vedtaksvurdering.database

import com.fasterxml.jackson.module.kotlin.readValue
import kotliquery.Row
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.behandling.VedtakStatus
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.toJsonNode
import no.nav.etterlatte.libs.common.vedtak.Periode
import no.nav.etterlatte.libs.common.vedtak.Utbetalingsperiode
import no.nav.etterlatte.libs.common.vedtak.UtbetalingsperiodeType
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingDto
import no.nav.etterlatte.vedtaksvurdering.Beregningsresultat
import no.nav.etterlatte.vedtaksvurdering.Vedtak
import org.slf4j.LoggerFactory
import java.sql.Date
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.time.LocalDate
import java.time.YearMonth
import java.util.*
import javax.sql.DataSource

class VedtaksvurderingRepository(private val datasource: DataSource) {

    private val logger = LoggerFactory.getLogger(VedtaksvurderingRepository::class.java)
    private val connection get() = datasource.connection

    companion object {
        fun using(datasource: DataSource): VedtaksvurderingRepository = VedtaksvurderingRepository(datasource)
    }

    fun opprettVedtak(
        behandlingsId: UUID,
        sakid: Long,
        fnr: String,
        saktype: SakType,
        behandlingtype: BehandlingType,
        virkningsDato: LocalDate,
        beregningsresultat: Beregningsresultat?,
        vilkaarsresultat: VilkaarsvurderingDto
    ) = connection
        .also { logger.info("Oppretter vedtak behandlingid: $behandlingsId sakid: $sakid") }
        .use {
            val opprettVedtak =
                "INSERT INTO vedtak(behandlingId, sakid, fnr, behandlingtype, saktype, vedtakstatus, datovirkfom,  beregningsresultat, vilkaarsresultat) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)" // ktlint-disable max-line-length
            it.prepareStatement(opprettVedtak).run {
                setUUID(1, behandlingsId)
                setLong(2, sakid)
                setString(3, fnr)
                setString(4, behandlingtype.name)
                setString(5, saktype.toString())
                setVedtakstatus(6, VedtakStatus.BEREGNET)
                setDate(7, Date.valueOf(virkningsDato))
                setJSONString(8, beregningsresultat)
                setJSONString(9, vilkaarsresultat)
                execute()
            }
        }

    fun oppdaterVedtak(
        behandlingsId: UUID,
        beregningsresultat: Beregningsresultat?,
        vilkaarsresultat: VilkaarsvurderingDto,
        virkningsDato: LocalDate
    ) = connection
        .also { logger.info("Oppdaterer vedtak behandlingid: $behandlingsId ") }
        .use {
            val oppdaterVedtak =
                "UPDATE vedtak SET datovirkfom = ?, beregningsresultat = ?, vilkaarsresultat = ? WHERE behandlingId = ?" // ktlint-disable max-line-length
            it.prepareStatement(oppdaterVedtak).run {
                setDate(1, Date.valueOf(virkningsDato))
                setJSONString(2, beregningsresultat)
                setJSONString(3, vilkaarsresultat)
                setUUID(4, behandlingsId)
                require(executeUpdate() == 1)
            }
        }

    // Kan det finnes flere vedtak for en behandling? Hør med Henrik
    fun lagreIverksattVedtak(
        behandlingsId: UUID
    ) = connection
        .also { logger.info("Lagrer iverksatt vedtak") }
        .use {
            it.prepareStatement("UPDATE vedtak SET vedtakstatus = ? WHERE behandlingId = ?").run {
                setVedtakstatus(1, VedtakStatus.IVERKSATT)
                setUUID(2, behandlingsId)
                require(executeUpdate() == 1)
            }
        }

    fun hentVedtakBolk(behandlingsidenter: List<UUID>): List<Vedtak> = connection
        .also { logger.info("Henter alle vedtak") }
        .use {
            val hentVedtakBolk =
                "SELECT sakid, behandlingId, saksbehandlerId, beregningsresultat, vilkaarsresultat," +
                    "vedtakfattet, id, fnr, datoFattet, datoattestert, attestant," +
                    "datoVirkFom, vedtakstatus, saktype, behandlingtype FROM vedtak where behandlingId = ANY(?)"
            val identer = it.createArrayOf("uuid", behandlingsidenter.toTypedArray())
            it.prepareStatement(hentVedtakBolk).run {
                setArray(1, identer)
                executeQuery().toList { toVedtak() }
            }
        }
    fun hentVedtak(behandlingsId: UUID): Vedtak? {
        val hentVedtak =
            "SELECT sakid, behandlingId, saksbehandlerId, beregningsresultat, vilkaarsresultat, vedtakfattet, id, fnr, datoFattet, datoattestert, attestant, datoVirkFom, vedtakstatus, saktype, behandlingtype FROM vedtak WHERE behandlingId = :behandlingId" // ktlint-disable max-line-length
        return hentMedKotliquery(query = hentVedtak, params = mapOf("behandlingId" to behandlingsId)) { it.toVedtak() }
    }

    private fun <T> hentMedKotliquery(
        query: String,
        params: Map<String, Any>,
        converter: (r: Row) -> T
    ) = kotliquery.using(sessionOf(datasource)) { session ->
        queryOf(statement = query, paramMap = params)
            .let { query ->
                session.run(
                    query.map { row -> converter.invoke(row) }
                        .asSingle
                )
            }
    }

    private fun Row.toVedtak() = Vedtak(
        sakId = longOrNull("sakid"),
        behandlingId = uuid("behandlingid"),
        saksbehandlerId = stringOrNull("saksbehandlerid"),
        beregningsResultat = stringOrNull("beregningsresultat").let {
            objectMapper.readValue(
                it,
                Beregningsresultat::class.java
            )
        },
        vilkaarsResultat = stringOrNull("vilkaarsresultat")?.toJsonNode(),
        vedtakFattet = boolean("vedtakfattet"),
        id = long("id"),
        fnr = stringOrNull("fnr"),
        datoFattet = sqlTimestampOrNull("datofattet")?.toInstant(),
        datoattestert = sqlTimestampOrNull("datoattestert")?.toInstant(),
        attestant = stringOrNull("attestant"),
        virkningsDato = sqlDateOrNull("datovirkfom")?.toLocalDate(),
        vedtakStatus = stringOrNull("vedtakstatus")?.let { VedtakStatus.valueOf(it) },
        sakType = SakType.valueOf(string("saktype")),
        behandlingType = BehandlingType.valueOf(string("behandlingtype"))
    )

    fun hentUtbetalingsPerioder(vedtakId: Long): List<Utbetalingsperiode> = connection.use {
        val hentUtbetalingsperiode = "SELECT * FROM utbetalingsperiode WHERE vedtakid = ?"
        val statement = it.prepareStatement(hentUtbetalingsperiode)
        statement.setLong(1, vedtakId)
        statement.executeQuery().toList {
            Utbetalingsperiode(
                getLong("id"),
                Periode(
                    YearMonth.from(getDate("datoFom").toLocalDate()),
                    getDate("datoTom")?.toLocalDate()?.let(YearMonth::from)
                ),
                getBigDecimal("beloep"),
                UtbetalingsperiodeType.valueOf(getString("type"))
            )
        }
    }

    private inline fun <reified T> ResultSet.getJsonObject(c: Int): T? = getString(c)?.let {
        try {
            objectMapper.readValue(it)
        } catch (ex: Exception) {
            logger.warn("vedtak ${getLong("id")} kan ikke lese kolonne $c")
            null
        }
    }

    fun fattVedtak(saksbehandlerId: String, behandlingsId: UUID) {
        connection.use {
            val fattVedtak =
                "UPDATE vedtak SET saksbehandlerId = ?, vedtakfattet = ?, datoFattet = now(), vedtakstatus = ?  WHERE behandlingId = ?" // ktlint-disable max-line-length
            it.prepareStatement(fattVedtak).run {
                setString(1, saksbehandlerId)
                setBoolean(2, true)
                setVedtakstatus(3, VedtakStatus.FATTET_VEDTAK)
                setUUID(4, behandlingsId)
                execute()
            }
        }
    }

    fun attesterVedtak(
        saksbehandlerId: String,
        behandlingsId: UUID,
        vedtakId: Long,
        utbetalingsperioder: List<Utbetalingsperiode>
    ) {
        connection.use {
            val lagreUtbetalingsperiode =
                "INSERT INTO utbetalingsperiode(vedtakid, datofom, datotom, type, beloep) VALUES (?, ?, ?, ?, ?)"
            val insertPersoderStatement = it.prepareStatement(lagreUtbetalingsperiode)
            utbetalingsperioder.forEach {
                insertPersoderStatement.setLong(1, vedtakId)
                insertPersoderStatement.setDate(2, it.periode.fom.atDay(1).let(Date::valueOf))
                insertPersoderStatement.setDate(3, it.periode.tom?.atEndOfMonth()?.let(Date::valueOf))
                insertPersoderStatement.setString(4, it.type.name)
                insertPersoderStatement.setBigDecimal(5, it.beloep)

                insertPersoderStatement.addBatch()
            }
            if (utbetalingsperioder.isNotEmpty()) {
                insertPersoderStatement.executeBatch().forEach {
                    require(it == 1)
                }
            }

            val statement =
                it.prepareStatement(
                    "UPDATE vedtak SET attestant = ?, datoAttestert = now(), vedtakstatus = ? WHERE behandlingId = ?"
                )
            statement.run {
                setString(1, saksbehandlerId)
                setVedtakstatus(2, VedtakStatus.ATTESTERT)
                setUUID(3, behandlingsId)
                execute()
            }
        }
    }

    fun underkjennVedtak(
        behandlingsId: UUID
    ) {
        connection.use {
            val underkjennVedtak =
                "UPDATE vedtak SET attestant = null, datoAttestert = null, saksbehandlerId = null, vedtakfattet = false, datoFattet = null, vedtakstatus = ? WHERE behandlingId = ?" // ktlint-disable max-line-length
            it.prepareStatement(underkjennVedtak).run {
                setVedtakstatus(1, VedtakStatus.RETURNERT)
                setUUID(2, behandlingsId)
                execute()
            }
        }
    }

    private fun ResultSet.toVedtak() = Vedtak(
        sakId = getLong(1),
        behandlingId = getObject(2) as UUID,
        saksbehandlerId = getString(3),
        beregningsResultat = getJsonObject(4),
        vilkaarsResultat = getJsonObject(5),
        vedtakFattet = getBoolean(6),
        id = getLong(7),
        fnr = getString(8),
        datoFattet = getTimestamp(9)?.toInstant(),
        datoattestert = getTimestamp(10)?.toInstant(),
        attestant = getString(11),
        virkningsDato = getDate(12)?.toLocalDate(),
        vedtakStatus = getString(13)?.let { VedtakStatus.valueOf(it) },
        sakType = SakType.valueOf(getString(14)),
        behandlingType = BehandlingType.valueOf(getString(15))
    )
}

private fun PreparedStatement.setUUID(index: Int, id: UUID) = setObject(index, id)

private fun PreparedStatement.setVedtakstatus(index: Int, vedtakStatus: VedtakStatus) =
    setString(index, vedtakStatus.name)

private fun <T> PreparedStatement.setJSONString(index: Int, obj: T) =
    setString(index, objectMapper.writeValueAsString(obj))

private fun <T> ResultSet.singleOrNull(block: ResultSet.() -> T): T? = if (next()) {
    block().also {
        require(!next()) { "Skal være unik" }
    }
} else {
    null
}

fun <T> ResultSet.toList(block: ResultSet.() -> T): List<T> = generateSequence {
    if (next()) {
        block()
    } else {
        null
    }
}.toList()