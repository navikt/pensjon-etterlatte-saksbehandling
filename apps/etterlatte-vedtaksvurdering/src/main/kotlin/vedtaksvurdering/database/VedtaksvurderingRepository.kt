package no.nav.etterlatte.vedtaksvurdering.database

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.behandling.VedtakStatus
import no.nav.etterlatte.libs.common.beregning.BeregningsResultat
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.vedtak.Periode
import no.nav.etterlatte.libs.common.vedtak.Utbetalingsperiode
import no.nav.etterlatte.libs.common.vedtak.UtbetalingsperiodeType
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingDto
import no.nav.etterlatte.vedtaksvurdering.Vedtak
import org.slf4j.LoggerFactory
import java.sql.Date
import java.sql.ResultSet
import java.time.LocalDate
import java.time.YearMonth
import java.util.*
import javax.sql.DataSource

class VedtaksvurderingRepository(private val datasource: DataSource) {

    private val logger = LoggerFactory.getLogger(VedtaksvurderingRepository::class.java)
    private val connection get() = datasource.connection

    companion object {
        fun using(datasource: DataSource): VedtaksvurderingRepository {
            return VedtaksvurderingRepository(datasource)
        }
    }

    fun opprettVedtak(
        behandlingsId: UUID,
        sakid: Long,
        fnr: String,
        saktype: SakType,
        behandlingtype: BehandlingType,
        virkningsDato: LocalDate,
        beregningsresultat: BeregningsResultat?,
        vilkaarsresultat: VilkaarsvurderingDto
    ) {
        logger.info("Oppretter vedtak behandlingid: $behandlingsId sakid: $sakid")
        connection.use {
            val statement = it.prepareStatement(Queries.opprettVedtak)
            statement.setObject(1, behandlingsId)
            statement.setLong(2, sakid)
            statement.setString(3, fnr)
            statement.setString(4, behandlingtype.name)
            statement.setString(5, saktype.toString())
            statement.setString(6, VedtakStatus.BEREGNET.toString())
            statement.setDate(7, Date.valueOf(virkningsDato))
            statement.setString(8, objectMapper.writeValueAsString(beregningsresultat))
            statement.setString(9, objectMapper.writeValueAsString(vilkaarsresultat))
            statement.execute()
        }
    }

    fun oppdaterVedtak(
        behandlingsId: UUID,
        beregningsresultat: BeregningsResultat?,
        vilkaarsresultat: VilkaarsvurderingDto,
        virkningsDato: LocalDate
    ) {
        logger.info("Oppdaterer vedtak behandlingid: $behandlingsId ")
        connection.use {
            val statement = it.prepareStatement(Queries.oppdaterVedtak)
            statement.setDate(1, Date.valueOf(virkningsDato))
            statement.setString(2, objectMapper.writeValueAsString(beregningsresultat))
            statement.setString(3, objectMapper.writeValueAsString(vilkaarsresultat))
            statement.setObject(4, behandlingsId)
            require(statement.executeUpdate() == 1)
        }
    }

    // Kan det finnes flere vedtak for en behandling? Hør med Henrik
    fun lagreIverksattVedtak(
        behandlingsId: UUID
    ) {
        logger.info("Lagrer iverksatt vedtak")
        connection.use {
            it.prepareStatement(Queries.lagreIverksattVedtak).run {
                setString(1, VedtakStatus.IVERKSATT.name)
                setObject(2, behandlingsId)
                require(executeUpdate() == 1)
            }
        }
    }

    fun hentVedtakBolk(behandlingsidenter: List<UUID>): List<Vedtak> {
        logger.info("Henter alle vedtak")

        return connection.use {
            val identer = it.createArrayOf("uuid", behandlingsidenter.toTypedArray())
            val statement = it.prepareStatement(Queries.hentVedtakBolk)
            statement.setArray(1, identer)
            statement.executeQuery().toList { toVedtak() }
        }
    }

    fun hentVedtak(behandlingsId: UUID): Vedtak? {
        val resultat = connection.use {
            val statement = it.prepareStatement(Queries.hentVedtak)
            statement.setObject(1, behandlingsId)
            statement.executeQuery().singleOrNull {
                toVedtak()
            }
        }
        return resultat
    }

    fun hentUtbetalingsPerioder(vedtakId: Long): List<Utbetalingsperiode> {
        val resultat = connection.use { it ->
            val statement = it.prepareStatement(Queries.hentUtbetalingsperiode)
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
        return resultat
    }

    private inline fun <reified T> ResultSet.getJsonObject(c: Int): T? {
        return getString(c)?.let {
            try {
                objectMapper.readValue(it)
            } catch (ex: Exception) {
                logger.warn("vedtak ${getLong("id")} kan ikke lese kolonne $c")
                null
            }
        }
    }

    fun fattVedtak(saksbehandlerId: String, behandlingsId: UUID) {
        connection.use {
            val statement = it.prepareStatement(Queries.fattVedtak)
            statement.setString(1, saksbehandlerId)
            statement.setBoolean(2, true)
            statement.setString(3, VedtakStatus.FATTET_VEDTAK.name)
            statement.setObject(4, behandlingsId)
            statement.execute()
        }
    }

    fun attesterVedtak(
        saksbehandlerId: String,
        behandlingsId: UUID,
        vedtakId: Long,
        utbetalingsperioder: List<Utbetalingsperiode>
    ) {
        connection.use {
            val insertPersoderStatement = it.prepareStatement(Queries.lagreUtbetalingsperiode)
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

            val statement = it.prepareStatement(Queries.attesterVedtak)
            statement.setString(1, saksbehandlerId)
            statement.setString(2, VedtakStatus.ATTESTERT.name)
            statement.setObject(3, behandlingsId)
            statement.execute()
        }
    }

    fun underkjennVedtak(
        behandlingsId: UUID
    ) {
        connection.use {
            val statement = it.prepareStatement(Queries.underkjennVedtak)
            statement.setString(1, VedtakStatus.RETURNERT.name)
            statement.setObject(2, behandlingsId)
            statement.execute()
        }
    }

    fun slettSak(sakId: Long) {
        connection.use {
            it.prepareStatement(Queries.slettUtbetalingsperioderISak).apply {
                setLong(1, sakId)
                executeUpdate()
            }
            it.prepareStatement(Queries.slettVedtakISak).apply {
                setLong(1, sakId)
                executeUpdate()
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
        sakType = getString(14),
        behandlingType = BehandlingType.valueOf(getString(15))
    )
}

private object Queries {

    val opprettVedtak = "INSERT INTO vedtak(behandlingId, sakid, fnr, behandlingtype, saktype, vedtakstatus, datovirkfom,  beregningsresultat, vilkaarsresultat) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)" // ktlint-disable max-line-length
    val oppdaterVedtak = "UPDATE vedtak SET datovirkfom = ?, beregningsresultat = ?, vilkaarsresultat = ? WHERE behandlingId = ?" // ktlint-disable max-line-length

    val fattVedtak =
        "UPDATE vedtak SET saksbehandlerId = ?, vedtakfattet = ?, datoFattet = now(), vedtakstatus = ?  WHERE behandlingId = ?" // ktlint-disable max-line-length
    val attesterVedtak =
        "UPDATE vedtak SET attestant = ?, datoAttestert = now(), vedtakstatus = ? WHERE behandlingId = ?"
    val underkjennVedtak =
        "UPDATE vedtak SET attestant = null, datoAttestert = null, saksbehandlerId = null, vedtakfattet = false, datoFattet = null, vedtakstatus = ? WHERE behandlingId = ?" // ktlint-disable max-line-length
    val hentVedtakBolk =
        "SELECT sakid, behandlingId, saksbehandlerId, beregningsresultat, vilkaarsresultat," +
            "vedtakfattet, id, fnr, datoFattet, datoattestert, attestant," +
            "datoVirkFom, vedtakstatus, saktype, behandlingtype FROM vedtak where behandlingId = ANY(?)"
    val hentVedtak =
        "SELECT sakid, behandlingId, saksbehandlerId, beregningsresultat, vilkaarsresultat, vedtakfattet, id, fnr, datoFattet, datoattestert, attestant, datoVirkFom, vedtakstatus, saktype, behandlingtype FROM vedtak WHERE behandlingId = ?" // ktlint-disable max-line-length

    val lagreUtbetalingsperiode =
        "INSERT INTO utbetalingsperiode(vedtakid, datofom, datotom, type, beloep) VALUES (?, ?, ?, ?, ?)"
    val hentUtbetalingsperiode = "SELECT * FROM utbetalingsperiode WHERE vedtakid = ?"
    val slettUtbetalingsperioderISak =
        "DELETE FROM utbetalingsperiode WHERE vedtakid in (SELECT id from vedtak where sakid = ?)"
    val slettVedtakISak = "DELETE FROM vedtak WHERE sakid = ?"
    val lagreIverksattVedtak = "UPDATE vedtak SET vedtakstatus = ? WHERE behandlingId = ?"
}

private fun <T> ResultSet.singleOrNull(block: ResultSet.() -> T): T? {
    return if (next()) {
        block().also {
            require(!next()) { "Skal være unik" }
        }
    } else {
        null
    }
}

fun <T> ResultSet.toList(block: ResultSet.() -> T): List<T> {
    return generateSequence {
        if (next()) {
            block()
        } else {
            null
        }
    }.toList()
}