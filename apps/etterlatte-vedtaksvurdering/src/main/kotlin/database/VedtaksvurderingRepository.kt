package no.nav.etterlatte.database

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.etterlatte.domene.vedtak.Periode
import no.nav.etterlatte.domene.vedtak.Utbetalingsperiode
import no.nav.etterlatte.domene.vedtak.UtbetalingsperiodeType
import no.nav.etterlatte.libs.common.avkorting.AvkortingsResultat
import no.nav.etterlatte.libs.common.behandling.VedtakStatus
import no.nav.etterlatte.libs.common.beregning.BeregningsResultat
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.vikaar.KommerSoekerTilgode
import no.nav.etterlatte.libs.common.vikaar.VilkaarResultat
import org.slf4j.LoggerFactory
import java.sql.Connection
import java.sql.Date
import java.sql.ResultSet
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.util.*
import javax.sql.DataSource

class VedtaksvurderingRepository(private val datasource: DataSource) {

    private val logger = LoggerFactory.getLogger(VedtaksvurderingRepository::class.java)
    private val connection get() = datasource.connection
    private val postgresTimeZone = ZoneId.of("UTC")

    companion object {
        fun using(datasource: DataSource): VedtaksvurderingRepository {
            return VedtaksvurderingRepository(datasource)
        }
    }

    fun lagreVilkaarsresultat(
        sakId: String,
        behandlingsId: UUID,
        fnr: String,
        vilkaarsresultat: VilkaarResultat,
        virkningsDato: LocalDate
    ) {
        logger.info("Lagrer vilkaarsresultat")
        connection.use {
            val statement = it.prepareStatement(Queries.lagreVilkaarResultat)
            statement.setLong(1, sakId.toLong())
            statement.setObject(2, behandlingsId)
            statement.setString(3, objectMapper.writeValueAsString(vilkaarsresultat))
            statement.setString(4, fnr)
            statement.setDate(5, Date.valueOf(virkningsDato))
            statement.setString(6, VedtakStatus.VILKAARSVURDERT.name)
            statement.execute()
        }
    }

    fun oppdaterVilkaarsresultat(sakId: String, behandlingsId: UUID, vilkaarsresultat: VilkaarResultat) {
        logger.info("Lagrer vilkaarsresultat")
        connection.use {
            val statement = it.prepareStatement(Queries.oppdaterVilkaarResultat)
            statement.setString(1, objectMapper.writeValueAsString(vilkaarsresultat))
            statement.setString(2, VedtakStatus.VILKAARSVURDERT.name)
            statement.setLong(3, sakId.toLong())
            statement.setObject(4, behandlingsId)
            statement.execute()
        }
    }

    fun lagreKommerSoekerTilgodeResultat(
        sakId: String,
        behandlingsId: UUID,
        fnr: String,
        kommerSoekerTilgodeResultat: KommerSoekerTilgode
    ) {
        logger.info("Lagrer kommerSoekerTilgodeResultat")
        connection.use {
            val statement = it.prepareStatement(Queries.lagreKommerSoekerTilgodeResultat)
            statement.setLong(1, sakId.toLong())
            statement.setObject(2, behandlingsId)
            statement.setString(3, objectMapper.writeValueAsString(kommerSoekerTilgodeResultat))
            statement.setString(4, fnr)
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

    fun lagreBeregningsresultat(
        sakId: String,
        behandlingsId: UUID,
        fnr: String,
        beregningsresultat: BeregningsResultat
    ) {
        logger.info("Lagrer beregningsresultat")

        connection.use {
            val statement = it.prepareStatement(Queries.lagreBeregningsresultat)
            statement.setLong(1, sakId.toLong())
            statement.setObject(2, behandlingsId)
            statement.setString(3, objectMapper.writeValueAsString(beregningsresultat))
            statement.setString(4, fnr)
            statement.setString(5, VedtakStatus.BEREGNET.name)
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

    fun lagreAvkorting(sakId: String, behandlingsId: UUID, fnr: String, avkortingsResultat: AvkortingsResultat) {
        logger.info("Lagrer avkorting")
        connection.use {
            val statement = it.prepareStatement(Queries.lagreAvkortingsresultat)
            statement.setLong(1, sakId.toLong())
            statement.setObject(2, behandlingsId)
            statement.setString(3, objectMapper.writeValueAsString(avkortingsResultat))
            statement.setString(4, fnr)
            statement.setString(5, VedtakStatus.AVKORTET.name)
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

    fun hentVedtak(sakId: String, behandlingsId: UUID): Vedtak? {
        val resultat = connection.use { it ->
            val statement = it.prepareStatement(Queries.hentVedtak)
            statement.setLong(1, sakId.toLong())
            statement.setObject(2, behandlingsId)
            statement.executeQuery().singleOrNull {
                Vedtak(
                    getLong(9),
                    getString(1),
                    getObject(2) as UUID,
                    getString(3),
                    getString(4)?.let {
                        try {
                            objectMapper.readValue(it)
                        } catch (ex: Exception) {
                            null
                        }
                    },
                    getJsonObject(5),
                    getJsonObject(6),
                    getJsonObject(7),
                    getBoolean(8),
                    getString(10),
                    getTimestamp(11)?.toInstant(),
                    getTimestamp(12)?.toInstant(),
                    getString(13),
                    getDate(14)?.toLocalDate(),
                    getString(15)?.let { VedtakStatus.valueOf(it) },
                )
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

    fun fattVedtak(saksbehandlerId: String, sakId: String, vedtakId: Long, behandlingsId: UUID) {
        connection.use {
            val statement = it.prepareStatement(Queries.fattVedtak)
            statement.setString(1, saksbehandlerId)
            statement.setBoolean(2, true)
            statement.setString(3, VedtakStatus.FATTET_VEDTAK.name)
            statement.setLong(4, sakId.toLong())
            statement.setObject(5, behandlingsId)
            statement.execute()

            loggAttesteringsHendelse(
                AttesteringsHendelseType.SENDT_TIL_ATTESTERING,
                vedtakId,
                saksbehandlerId,
                null,
                null,
                it
            )
        }
    }

    private fun loggAttesteringsHendelse(
        hendelse: AttesteringsHendelseType,
        vedtakId: Long,
        saksbehandlerId: String,
        kommentar: String?,
        valgtBegrunnelse: String?,
        connection: Connection
    ) {
        connection.prepareStatement(Queries.lagreAttesteringsHendelse).apply {
            setLong(1, vedtakId)
            setString(2, hendelse.name)
            setString(3, saksbehandlerId)
            setString(4, kommentar)
            setString(5, valgtBegrunnelse)
            executeUpdate().also { require(it == 1) }
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
            if (utbetalingsperioder.isNotEmpty()) insertPersoderStatement.executeBatch().forEach {
                require(it == 1)
            }

            val statement = it.prepareStatement(Queries.attesterVedtak)
            statement.setString(1, saksbehandlerId)
            statement.setString(2, VedtakStatus.ATTESTERT.name)
            statement.setLong(3, sakId.toLong())
            statement.setObject(4, behandlingsId)
            statement.execute()

            loggAttesteringsHendelse(AttesteringsHendelseType.ATTESTERT, vedtakId, saksbehandlerId, null, null, it)
        }
    }

    fun underkjennVedtak(
        saksbehandlerId: String,
        sakId: String,
        behandlingsId: UUID,
        vedtakId: Long,
        kommentar: String,
        valgtBegrunnelse: String
    ) {
        connection.use {
            val statement = it.prepareStatement(Queries.underkjennVedtak)
            statement.setString(1, VedtakStatus.RETURNERT.name)
            statement.setLong(2, sakId.toLong())
            statement.setObject(3, behandlingsId)
            statement.execute()

            loggAttesteringsHendelse(
                AttesteringsHendelseType.UNDERKJENT,
                vedtakId,
                saksbehandlerId,
                kommentar,
                valgtBegrunnelse,
                it
            )
        }
    }

    fun hentAttesteringHendelser(vedtakId: Long): List<AttesteringsHendelse> {
        return connection.use {
            val statement = it.prepareStatement(Queries.hentAttesteringsHendelse)
            statement.setLong(1, vedtakId)
            statement.executeQuery().toList {
                AttesteringsHendelse(
                    AttesteringsHendelseType.valueOf(getString("hendelse")),
                    Tidspunkt(getTimestamp("opprettet").toInstant()),
                    getString("ident"),
                    getString("kommentar"),
                    getString("valgtbegrunnelse")
                )
            }
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

    fun lagreDatoVirk(sakId: String, behandlingId: UUID, datoVirk: LocalDate) {
        connection.use {
            val statement = it.prepareStatement(Queries.lagreDatoVirkFom)
            statement.setDate(1, Date.valueOf(datoVirk))
            statement.setLong(2, sakId.toLong())
            statement.setObject(3, behandlingId)
            statement.execute()
        }
    }
}

data class Vedtak(
    val id: Long,
    val sakId: String,
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
)

data class AttesteringsHendelse(
    val hendelse: AttesteringsHendelseType,
    val opprettet: Tidspunkt,
    val ident: String,
    val kommentar: String?,
    val valgtbegrunnelse: String?
)

enum class AttesteringsHendelseType {
    SENDT_TIL_ATTESTERING, UNDERKJENT, ATTESTERT
}

private object Queries {
    val lagreBeregningsresultat =
        "INSERT INTO vedtak(sakId, behandlingId, beregningsresultat, fnr, vedtakstatus  ) VALUES (?, ?, ?, ?, ?)"
    val oppdaterBeregningsresultat =
        "UPDATE vedtak SET beregningsresultat = ?, vedtakstatus = ? WHERE sakId = ? AND behandlingId = ?"

    val lagreVilkaarResultat =
        "INSERT INTO vedtak(sakId, behandlingId, vilkaarsresultat, datoVirkFom, vedtakstatus ) VALUES (?, ?, ?, ?, ?, ?) "
    val oppdaterVilkaarResultat =
        "UPDATE vedtak SET vilkaarsresultat = ?, vedtakstatus = ? WHERE sakId = ? AND behandlingId = ?"

    val lagreKommerSoekerTilgodeResultat =
        "INSERT INTO vedtak(sakId, behandlingId, kommersoekertilgoderesultat, fnr) VALUES (?, ?, ?, ?)"
    val oppdatereKommerSoekerTilgodeResultat =
        "UPDATE vedtak SET kommersoekertilgoderesultat = ? WHERE sakId = ? AND behandlingId = ?"

    val lagreAvkortingsresultat =
        "INSERT INTO vedtak(sakId, behandlingId, avkortingsresultat, fnr, vedtakstatus ) VALUES (?, ?, ?, ?, ?)"
    val oppdaterAvkortingsresultat =
        "UPDATE vedtak SET avkortingsresultat = ?, vedtakstatus = ? WHERE sakId = ? AND behandlingId = ?"

    val fattVedtak =
        "UPDATE vedtak SET saksbehandlerId = ?, vedtakfattet = ?, datoFattet = now(), vedtakstatus = ?  WHERE sakId = ? AND behandlingId = ?"
    val attesterVedtak =
        "UPDATE vedtak SET attestant = ?, datoAttestert = now(), vedtakstatus = ? WHERE sakId = ? AND behandlingId = ?"
    val underkjennVedtak =
        "UPDATE vedtak SET attestant = null, datoAttestert = null, saksbehandlerId = null, vedtakfattet = false, datoFattet = null, vedtakstatus = ? WHERE sakId = ? AND behandlingId = ?"

    val hentVedtak =
        "SELECT sakId, behandlingId, saksbehandlerId, avkortingsresultat, beregningsresultat, vilkaarsresultat, kommersoekertilgoderesultat, vedtakfattet, id, fnr, datoFattet, datoattestert, attestant, datoVirkFom, vedtakstatus FROM vedtak WHERE sakId = ? AND behandlingId = ?"

    val lagreFnr = "UPDATE vedtak SET fnr = ? WHERE sakId = ? AND behandlingId = ?"
    val lagreDatoVirkFom = "UPDATE vedtak SET datoVirkFom = ? WHERE sakId = ? AND behandlingId = ?"

    val lagreUtbetalingsperiode =
        "INSERT INTO utbetalingsperiode(vedtakid, datofom, datotom, type, beloep) VALUES (?, ?, ?, ?, ?)"
    val hentUtbetalingsperiode = "SELECT * FROM utbetalingsperiode WHERE vedtakid = ?"

    val lagreAttesteringsHendelse =
        "INSERT INTO attesteringshendelse(vedtakid, hendelse, ident, kommentar, valgtbegrunnelse) VALUES (?, ?, ?, ?, ?)"
    val hentAttesteringsHendelse =
        "SELECT hendelse, ident, kommentar, valgtbegrunnelse, opprettet FROM  attesteringshendelse WHERE vedtakid = ?"
}

fun <T> ResultSet.singleOrNull(block: ResultSet.() -> T): T? {
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
        if (next()) block()
        else null
    }.toList()
}