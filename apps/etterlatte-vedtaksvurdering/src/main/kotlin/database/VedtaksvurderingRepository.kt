package no.nav.etterlatte.database

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.etterlatte.domene.vedtak.Behandling
import no.nav.etterlatte.domene.vedtak.Periode
import no.nav.etterlatte.domene.vedtak.Utbetalingsperiode
import no.nav.etterlatte.domene.vedtak.UtbetalingsperiodeType
import no.nav.etterlatte.libs.common.avkorting.AvkortingsResultat
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.VedtakStatus
import no.nav.etterlatte.libs.common.beregning.BeregningsResultat
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.vikaar.KommerSoekerTilgode
import no.nav.etterlatte.libs.common.vikaar.VilkaarResultat
import org.slf4j.LoggerFactory
import java.sql.Date
import java.sql.ResultSet
import java.time.Instant
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

    fun lagreVilkaarsresultat(
        sakId: String,
        saktype: String,
        behandling: Behandling,
        fnr: String,
        vilkaarsresultat: VilkaarResultat,
        virkningsDato: LocalDate?
    ) {
        logger.info("Lagrer vilkaarsresultat")
        connection.use {
            val statement = it.prepareStatement(Queries.lagreVilkaarResultat)
            statement.setLong(1, sakId.toLong())
            statement.setObject(2, behandling.id)
            statement.setString(3, objectMapper.writeValueAsString(vilkaarsresultat))
            statement.setString(4, fnr)
            statement.setDate(5, virkningsDato?.let { Date.valueOf(virkningsDato) })
            statement.setString(6, VedtakStatus.VILKAARSVURDERT.name)
            statement.setString(7, saktype)
            statement.setString(8, behandling.type.name)
            statement.execute()
        }
    }

    fun oppdaterVilkaarsresultat(
        sakId: String,
        saktype: String,
        behandlingsId: UUID,
        vilkaarsresultat: VilkaarResultat
    ) {
        logger.info("Lagrer vilkaarsresultat")
        connection.use {
            val statement = it.prepareStatement(Queries.oppdaterVilkaarResultat)
            statement.setString(1, objectMapper.writeValueAsString(vilkaarsresultat))
            statement.setObject(3, saktype)
            statement.setString(2, VedtakStatus.VILKAARSVURDERT.name)
            statement.setLong(4, sakId.toLong())
            statement.setObject(5, behandlingsId)
            statement.execute()
        }
    }

    fun lagreKommerSoekerTilgodeResultat(
        sakId: String,
        behandling: Behandling,
        fnr: String,
        kommerSoekerTilgodeResultat: KommerSoekerTilgode
    ) {
        logger.info("Lagrer kommerSoekerTilgodeResultat")
        connection.use {
            val statement = it.prepareStatement(Queries.lagreKommerSoekerTilgodeResultat)
            statement.setLong(1, sakId.toLong())
            statement.setObject(2, behandling.id)
            statement.setString(3, objectMapper.writeValueAsString(kommerSoekerTilgodeResultat))
            statement.setString(4, fnr)
            statement.setString(5, behandling.type.name)
            statement.execute()
        }
    }

    fun oppdaterKommerSoekerTilgodeResultat(
        sakId: String,
        behandlingsId: UUID,
        kommerSoekerTilgodeResultat: KommerSoekerTilgode
    ) {
        logger.info("Lagrer kommerSoekerTilgodeResultat")
        connection.use {
            val statement = it.prepareStatement(Queries.oppdatereKommerSoekerTilgodeResultat)
            statement.setString(1, objectMapper.writeValueAsString(kommerSoekerTilgodeResultat))
            statement.setLong(2, sakId.toLong())
            statement.setObject(3, behandlingsId)
            statement.execute()
        }
    }

    // Kan det finnes flere vedtak for en behandling? Hør med Henrik
    fun lagreIverksattVedtak(
        behandlingsId: UUID
    ) {
        logger.info("Lagrer iverksatt vedtak")
        connection.use {
            val statement = it.prepareStatement(Queries.lagreIverksattVedtak).run {
                setString(1, VedtakStatus.IVERKSATT.name)
                setObject(2, behandlingsId)
                require(executeUpdate() == 1)
            }
        }
    }

    fun lagreBeregningsresultat(
        sakId: String,
        behandling: Behandling,
        fnr: String,
        beregningsresultat: BeregningsResultat
    ) {
        logger.info("Lagrer beregningsresultat")

        connection.use {
            val statement = it.prepareStatement(Queries.lagreBeregningsresultat)
            statement.setLong(1, sakId.toLong())
            statement.setObject(2, behandling.id)
            statement.setString(3, objectMapper.writeValueAsString(beregningsresultat))
            statement.setString(4, fnr)
            statement.setString(5, VedtakStatus.BEREGNET.name)
            statement.setString(6, behandling.type.name)
            statement.execute()
        }
    }

    fun oppdaterBeregningsgrunnlag(sakId: String, behandlingsId: UUID, beregningsresultat: BeregningsResultat) {
        logger.info("Lagrer beregningsresultat")
        connection.use {
            val statement = it.prepareStatement(Queries.oppdaterBeregningsresultat)
            statement.setString(1, objectMapper.writeValueAsString(beregningsresultat))
            statement.setString(2, VedtakStatus.BEREGNET.name)
            statement.setLong(3, sakId.toLong())
            statement.setObject(4, behandlingsId)
            statement.execute()
        }
    }

    fun lagreAvkorting(sakId: String, behandling: Behandling, fnr: String, avkortingsResultat: AvkortingsResultat) {
        logger.info("Lagrer avkorting")
        connection.use {
            val statement = it.prepareStatement(Queries.lagreAvkortingsresultat)
            statement.setLong(1, sakId.toLong())
            statement.setObject(2, behandling.id)
            statement.setString(3, objectMapper.writeValueAsString(avkortingsResultat))
            statement.setString(4, fnr)
            statement.setString(5, VedtakStatus.AVKORTET.name)
            statement.setString(6, behandling.type.name)
            statement.execute()
        }
    }

    fun oppdaterAvkorting(sakId: String, behandlingsId: UUID, avkortingsResultat: AvkortingsResultat) {
        logger.info("Lagrer avkorting")
        connection.use {
            val statement = it.prepareStatement(Queries.oppdaterAvkortingsresultat)
            statement.setString(1, objectMapper.writeValueAsString(avkortingsResultat))
            statement.setString(2, VedtakStatus.AVKORTET.name)
            statement.setLong(3, sakId.toLong())
            statement.setObject(4, behandlingsId)
            statement.execute()
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

    fun hentVedtak(sakId: String, behandlingsId: UUID): Vedtak? {
        val resultat = connection.use { it ->
            val statement = it.prepareStatement(Queries.hentVedtak)
            statement.setLong(1, sakId.toLong())
            statement.setObject(2, behandlingsId)
            statement.executeQuery().singleOrNull {
                toVedtak()
            }
        }
        return resultat
    }

    fun hentVedtak(behandlingsId: UUID): Vedtak? {
        val resultat = connection.use { it ->
            val statement = it.prepareStatement(Queries.hentVedtakForBehandling)
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

    fun fattVedtak(saksbehandlerId: String, sakId: String, behandlingsId: UUID) {
        connection.use {
            val statement = it.prepareStatement(Queries.fattVedtak)
            statement.setString(1, saksbehandlerId)
            statement.setBoolean(2, true)
            statement.setString(3, VedtakStatus.FATTET_VEDTAK.name)
            statement.setLong(4, sakId.toLong())
            statement.setObject(5, behandlingsId)
            statement.execute()
        }
    }

    fun attesterVedtak(
        saksbehandlerId: String,
        sakId: String,
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
            statement.setLong(3, sakId.toLong())
            statement.setObject(4, behandlingsId)
            statement.execute()
        }
    }

    fun underkjennVedtak(
        sakId: String,
        behandlingsId: UUID
    ) {
        connection.use {
            val statement = it.prepareStatement(Queries.underkjennVedtak)
            statement.setString(1, VedtakStatus.RETURNERT.name)
            statement.setLong(2, sakId.toLong())
            statement.setObject(3, behandlingsId)
            statement.execute()
        }
    }

    fun lagreFnr(sakId: String, behandlingId: UUID, fnr: String) {
        connection.use {
            val statement = it.prepareStatement(Queries.lagreFnr)
            statement.setString(1, fnr)
            statement.setLong(2, sakId.toLong())
            statement.setObject(3, behandlingId)
            statement.execute()
        }
    }

    fun lagreDatoVirk(sakId: String, behandlingId: UUID, datoVirk: LocalDate?) {
        connection.use {
            val statement = it.prepareStatement(Queries.lagreDatoVirkFom)
            statement.setDate(1, datoVirk?.let { Date.valueOf(datoVirk) })
            statement.setLong(2, sakId.toLong())
            statement.setObject(3, behandlingId)
            statement.execute()
        }
    }

    fun slettSak(sakId: Long) {
        connection.use {
            it.prepareStatement(Queries.slettUtbetalingsperioderISak).apply {
                setLong(1, sakId)
                execute()
            }
            it.prepareStatement(Queries.slettVedtakISak).apply {
                setLong(1, sakId)
                execute()
            }
        }
    }

    private fun ResultSet.toVedtak() = Vedtak(
        id = getLong(9),
        sakId = getString(1),
        sakType = getString(16),
        behandlingId = getObject(2) as UUID,
        saksbehandlerId = getString(3),
        avkortingsResultat = getString(4)?.let {
            try {
                objectMapper.readValue(it)
            } catch (ex: Exception) {
                null
            }
        },
        beregningsResultat = getJsonObject(5),
        vilkaarsResultat = getJsonObject(6),
        kommerSoekerTilgodeResultat = getJsonObject(7),
        vedtakFattet = getBoolean(8),
        fnr = getString(10),
        datoFattet = getTimestamp(11)?.toInstant(),
        datoattestert = getTimestamp(12)?.toInstant(),
        attestant = getString(13),
        virkningsDato = getDate(14)?.toLocalDate(),
        vedtakStatus = getString(15)?.let { VedtakStatus.valueOf(it) },
        behandlingType = getString(17)?.let { BehandlingType.valueOf(it) }
            ?: BehandlingType.FØRSTEGANGSBEHANDLING // TODO: Hacky å defaulte her, må ses på
    )
}

data class Vedtak(
    val id: Long,
    val sakId: String,
    val sakType: String?,
    val behandlingId: UUID,
    val saksbehandlerId: String?,
    val avkortingsResultat: AvkortingsResultat?,
    val beregningsResultat: BeregningsResultat?,
    val vilkaarsResultat: VilkaarResultat?,
    val kommerSoekerTilgodeResultat: KommerSoekerTilgode?,
    val vedtakFattet: Boolean?,
    val fnr: String?,
    val datoFattet: Instant?,
    val datoattestert: Instant?,
    val attestant: String?,
    val virkningsDato: LocalDate?,
    val vedtakStatus: VedtakStatus?,
    val behandlingType: BehandlingType
)

private object Queries {
    val lagreBeregningsresultat =
        "INSERT INTO vedtak(sakId, behandlingId, beregningsresultat, fnr, vedtakstatus, behandlingtype) VALUES (?, ?, ?, ?, ?, ?)" // ktlint-disable max-line-length
    val oppdaterBeregningsresultat =
        "UPDATE vedtak SET beregningsresultat = ?, vedtakstatus = ? WHERE sakId = ? AND behandlingId = ?"

    val lagreVilkaarResultat =
        "INSERT INTO vedtak(sakId, behandlingId, vilkaarsresultat, fnr, datoVirkFom, vedtakstatus, saktype, behandlingtype) VALUES (?, ?, ?, ?, ?, ?, ?, ?)" // ktlint-disable max-line-length

    val oppdaterVilkaarResultat =
        "UPDATE vedtak SET vilkaarsresultat = ?, vedtakstatus = ?, saktype = ? WHERE sakId = ? AND behandlingId = ?"

    val lagreKommerSoekerTilgodeResultat =
        "INSERT INTO vedtak(sakId, behandlingId, kommersoekertilgoderesultat, fnr, behandlingtype) VALUES (?, ?, ?, ?, ?)" // ktlint-disable max-line-length

    val oppdatereKommerSoekerTilgodeResultat =
        "UPDATE vedtak SET kommersoekertilgoderesultat = ? WHERE sakId = ? AND behandlingId = ?"

    val lagreAvkortingsresultat =
        "INSERT INTO vedtak(sakId, behandlingId, avkortingsresultat, fnr, vedtakstatus, behandlingtype ) VALUES (?, ?, ?, ?, ?, ?)" // ktlint-disable max-line-length
    val oppdaterAvkortingsresultat =
        "UPDATE vedtak SET avkortingsresultat = ?, vedtakstatus = ? WHERE sakId = ? AND behandlingId = ?"

    val fattVedtak =
        "UPDATE vedtak SET saksbehandlerId = ?, vedtakfattet = ?, datoFattet = now(), vedtakstatus = ?  WHERE sakId = ? AND behandlingId = ?" // ktlint-disable max-line-length
    val attesterVedtak =
        "UPDATE vedtak SET attestant = ?, datoAttestert = now(), vedtakstatus = ? WHERE sakId = ? AND behandlingId = ?"
    val underkjennVedtak =
        "UPDATE vedtak SET attestant = null, datoAttestert = null, saksbehandlerId = null, vedtakfattet = false, datoFattet = null, vedtakstatus = ? WHERE sakId = ? AND behandlingId = ?" // ktlint-disable max-line-length

    val hentVedtakBolk =
        "SELECT sakId, behandlingId, saksbehandlerId, avkortingsresultat, beregningsresultat, vilkaarsresultat," +
            "kommersoekertilgoderesultat, vedtakfattet, id, fnr, datoFattet, datoattestert, attestant," +
            "datoVirkFom, vedtakstatus, saktype, behandlingtype FROM vedtak where behandlingId = ANY(?)"
    val hentVedtak =
        "SELECT sakId, behandlingId, saksbehandlerId, avkortingsresultat, beregningsresultat, vilkaarsresultat, kommersoekertilgoderesultat, vedtakfattet, id, fnr, datoFattet, datoattestert, attestant, datoVirkFom, vedtakstatus, saktype, behandlingtype FROM vedtak WHERE sakId = ? AND behandlingId = ?" // ktlint-disable max-line-length
    val hentVedtakForBehandling =
        "SELECT sakId, behandlingId, saksbehandlerId, avkortingsresultat, beregningsresultat, vilkaarsresultat, kommersoekertilgoderesultat, vedtakfattet, id, fnr, datoFattet, datoattestert, attestant, datoVirkFom, vedtakstatus, saktype, behandlingtype FROM vedtak WHERE behandlingId = ?" // ktlint-disable max-line-length

    val lagreFnr = "UPDATE vedtak SET fnr = ? WHERE sakId = ? AND behandlingId = ?"
    val lagreDatoVirkFom = "UPDATE vedtak SET datoVirkFom = ? WHERE sakId = ? AND behandlingId = ?"

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