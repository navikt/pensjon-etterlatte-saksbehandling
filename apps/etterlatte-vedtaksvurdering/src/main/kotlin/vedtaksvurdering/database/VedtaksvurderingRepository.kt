package no.nav.etterlatte.vedtaksvurdering.database

import com.fasterxml.jackson.module.kotlin.readValue
import kotliquery.Row
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
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
    ) = opprett(
        query = "INSERT INTO vedtak(behandlingId, sakid, fnr, behandlingtype, saktype, vedtakstatus, datovirkfom,  beregningsresultat, vilkaarsresultat) " + // ktlint-disable max-line-length
            "VALUES (:behandlingId, :sakid, :fnr, :behandlingtype, :saktype, :vedtakstatus, :datovirkfom, :beregningsresultat, :vilkaarsresultat)", // ktlint-disable max-line-length
        params = mapOf(
            "behandlingId" to behandlingsId,
            "sakid" to sakid,
            "fnr" to fnr,
            "behandlingtype" to behandlingtype.name,
            "saktype" to saktype.name,
            "vedtakstatus" to VedtakStatus.BEREGNET.name,
            "datovirkfom" to Date.valueOf(virkningsDato),
            "beregningsresultat" to beregningsresultat?.let { objectMapper.writeValueAsString(it) },
            "vilkaarsresultat" to vilkaarsresultat.let { objectMapper.writeValueAsString(it) }
        ),
        loggtekst = "Oppretter vedtak behandlingid: $behandlingsId sakid: $sakid"
    )

    fun oppdaterVedtak(
        behandlingsId: UUID,
        beregningsresultat: Beregningsresultat?,
        vilkaarsresultat: VilkaarsvurderingDto,
        virkningsDato: LocalDate
    ) = oppdater(
        query = "UPDATE vedtak SET datovirkfom = :datovirkfom, beregningsresultat = :beregningsresultat, vilkaarsresultat = :vilkaarsresultat WHERE behandlingId = :behandlingid", // ktlint-disable max-line-length
        params = mapOf(
            "datovirkfom" to Date.valueOf(virkningsDato),
            "beregningsresultat" to objectMapper.writeValueAsString(beregningsresultat),
            "vilkaarsresultat" to objectMapper.writeValueAsString(vilkaarsresultat),
            "behandlingid" to behandlingsId
        ),
        loggtekst = "Oppdaterer vedtak behandlingid: $behandlingsId "
    ).also { require(it == 1) }

    private fun oppdater(query: String, params: Map<String, Any>, loggtekst: String) =
        using(sessionOf(datasource)) { session ->
            queryOf(
                statement = query,
                paramMap = params
            )
                .also { logger.info(loggtekst) }
                .let { session.run(it.asUpdate) }
        }

    private fun opprett(query: String, params: Map<String, Any?>, loggtekst: String) =
        using(sessionOf(datasource)) { session ->
            queryOf(
                statement = query,
                paramMap = params
            )
                .also { logger.info(loggtekst) }
                .let { session.run(it.asExecute) }
        }

    // Kan det finnes flere vedtak for en behandling? Hør med Henrik
    fun lagreIverksattVedtak(
        behandlingsId: UUID
    ) = oppdater(
        query = "UPDATE vedtak SET vedtakstatus = :vedtakstatus WHERE behandlingId = :behandlingId",
        params = mapOf("vedtakstatus" to VedtakStatus.IVERKSATT.name, "behandlingId" to behandlingsId),
        loggtekst = "Lagrer iverksatt vedtak"
    ).also { require(it == 1) }

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
    ) = using(sessionOf(datasource)) { session ->
        queryOf(statement = query, paramMap = params)
            .let { query ->
                session.run(
                    query.map { row -> converter.invoke(row) }
                        .asSingle
                )
            }
    }

    private fun <T> hentListeMedKotliquery(
        query: String,
        params: Map<String, Any>,
        converter: (r: Row) -> T
    ): List<T> = using(sessionOf(datasource)) { session ->
        queryOf(statement = query, paramMap = params)
            .let { query ->
                session.run(
                    query.map { row -> converter.invoke(row) }
                        .asList
                )
            }
    }

    private fun Row.toVedtak() = Vedtak(
        sakId = longOrNull("sakid"),
        behandlingId = uuid("behandlingid"),
        saksbehandlerId = stringOrNull("saksbehandlerid"),
        beregningsResultat = stringOrNull("beregningsresultat")?.let {
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

    fun hentUtbetalingsPerioder(vedtakId: Long): List<Utbetalingsperiode> = hentListeMedKotliquery(
        query = "SELECT * FROM utbetalingsperiode WHERE vedtakid = :vedtakid",
        params = mapOf("vedtakid" to vedtakId)
    ) {
        Utbetalingsperiode(
            it.long("id"),
            Periode(
                YearMonth.from(it.localDate("datoFom")),
                it.localDateOrNull("datoTom")?.let(YearMonth::from)
            ),
            it.bigDecimalOrNull("beloep"),
            UtbetalingsperiodeType.valueOf(it.string("type"))
        )
    }

    private inline fun <reified T> ResultSet.getJsonObject(c: Int): T? = getString(c)?.let {
        try {
            objectMapper.readValue(it)
        } catch (ex: Exception) {
            logger.warn("vedtak ${getLong("id")} kan ikke lese kolonne $c")
            null
        }
    }

    fun fattVedtak(saksbehandlerId: String, behandlingsId: UUID) = oppdater(
        query = "UPDATE vedtak SET saksbehandlerId = :saksbehandlerId, vedtakfattet = :vedtakfattet, datoFattet = now(), vedtakstatus = :vedtakstatus  WHERE behandlingId = :behandlingId", // ktlint-disable max-line-lengt
        params = mapOf(
            "saksbehandlerId" to saksbehandlerId,
            "vedtakfattet" to true,
            "vedtakstatus" to VedtakStatus.FATTET_VEDTAK.name,
            "behandlingId" to behandlingsId
        ),
        loggtekst = "Fatter vedtok for behandling $behandlingsId"
    )

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
        oppdater(
            "UPDATE vedtak SET attestant = null, datoAttestert = null, saksbehandlerId = null, vedtakfattet = false, datoFattet = null, vedtakstatus = :vedtakstatus WHERE behandlingId = :behandlingId", // ktlint-disable max-line-length
            params = mapOf("vedtakstatus" to VedtakStatus.RETURNERT.name, "behandlingId" to behandlingsId),
            loggtekst = "Underkjenner vedtak for behandling $behandlingsId"
        )
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

fun <T> ResultSet.toList(block: ResultSet.() -> T): List<T> = generateSequence {
    if (next()) {
        block()
    } else {
        null
    }
}.toList()