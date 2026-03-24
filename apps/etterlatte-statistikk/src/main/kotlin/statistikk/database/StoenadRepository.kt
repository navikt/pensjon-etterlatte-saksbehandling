package no.nav.etterlatte.statistikk.database

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.tidspunkt.setTidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.toNorskTidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.toTidspunkt
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import no.nav.etterlatte.libs.database.setNullableLong
import no.nav.etterlatte.libs.database.setSakId
import no.nav.etterlatte.libs.database.toList
import no.nav.etterlatte.statistikk.domain.MaanedStoenadRad
import no.nav.etterlatte.statistikk.domain.SakUtland
import no.nav.etterlatte.statistikk.domain.SakYtelsesgruppe
import no.nav.etterlatte.statistikk.domain.StoenadRad
import org.postgresql.util.PGobject
import java.sql.Date
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Statement
import java.sql.Types
import java.time.LocalTime
import java.time.YearMonth
import java.util.UUID
import javax.sql.DataSource

class StoenadRepository(
    private val datasource: DataSource,
) {
    private val connection get() = datasource.connection

    companion object {
        fun using(datasource: DataSource): StoenadRepository = StoenadRepository(datasource)
    }

    fun hentStoenadRader(): List<StoenadRad> =
        connection.use {
            it
                .prepareStatement(
                    """
                    SELECT id, fnrSoeker, fnrForeldre, 
                        fnrSoesken, anvendtTrygdetid, nettoYtelse, ytelseFoerAvkorting, beregningType, anvendtSats, 
                        behandlingId, sakId, sakNummer, tekniskTid, sakYtelse, versjon, saksbehandler, attestant, 
                        vedtakLoependeFom, vedtakLoependeTom, beregning, avkorting, vedtakType, sak_utland, 
                        virkningstidspunkt, utbetalingsdato, kilde, pesysid, sakYtelsesgruppe, opphoerFom, vedtaksperioder
                    FROM stoenad
                    """.trimIndent(),
                ).executeQuery()
                .toList {
                    asStoenadRad()
                }
        }

    fun hentStoenadRaderInnenforMaaned(maaned: YearMonth): List<StoenadRad> =
        connection.use { conn ->
            conn
                .prepareStatement(
                    """
                    SELECT * FROM stoenad 
                    WHERE vedtakLoependeFom <= ? AND COALESCE(vedtakLoependeTom, ?) >= ? 
                        AND tekniskTid <= ?
                    """.trimIndent(),
                ).apply {
                    setDate(1, Date.valueOf(maaned.atEndOfMonth()))
                    setDate(2, Date.valueOf(maaned.atEndOfMonth()))
                    setDate(3, Date.valueOf(maaned.atEndOfMonth()))
                    setTidspunkt(4, maaned.atEndOfMonth().atTime(LocalTime.MAX).toNorskTidspunkt())
                }.executeQuery()
                .toList { asStoenadRad() }
        }

    fun lagreMaanedStatistikkRad(maanedStatistikkRad: MaanedStoenadRad) =
        connection.use { conn ->
            conn
                .prepareStatement(
                    """
                    INSERT INTO maaned_stoenad(
                        fnrSoeker, fnrForeldre, fnrSoesken, anvendtTrygdetid, nettoYtelse, ytelseFoerAvkorting,
                        beregningType, anvendtSats, 
                        behandlingId, sakId, tekniskTid, sakYtelse, versjon, saksbehandler, attestant, 
                        vedtakLoependeFom, vedtakLoependeTom, statistikkMaaned, sak_utland,
                        virkningstidspunkt, utbetalingsdato, avkortingsbeloep, aarsinntekt, kilde, pesysid, 
                        sakYtelsesgruppe, harAktivitetsplikt, oppfyllerAktivitet, aktivitet, sanksjon,
                        etteroppgjoer_aar, etteroppgjoer_utbetalt, etteroppgjoer_ny_stoenad, etteroppgjoer_differanse,
                        etteroppgjoer_resultat, etterbetalt_beloep, tilbakekrevd_beloep
                    ) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?,
                    ?, ?, ?, ?, ?, ?, ?)
                    """.trimIndent(),
                ).apply {
                    setString(1, maanedStatistikkRad.fnrSoeker)
                    setJsonb(2, maanedStatistikkRad.fnrForeldre)
                    setJsonb(3, maanedStatistikkRad.fnrSoesken)
                    setString(4, maanedStatistikkRad.anvendtTrygdetid)
                    setString(5, maanedStatistikkRad.nettoYtelse)
                    setString(6, maanedStatistikkRad.ytelseFoerAvkorting)
                    setString(7, maanedStatistikkRad.beregningType)
                    setString(8, maanedStatistikkRad.anvendtSats)
                    setObject(9, maanedStatistikkRad.behandlingId)
                    setSakId(10, maanedStatistikkRad.sakId)
                    setTidspunkt(11, maanedStatistikkRad.tekniskTid)
                    setString(12, maanedStatistikkRad.sakYtelse)
                    setString(13, maanedStatistikkRad.versjon)
                    setString(14, maanedStatistikkRad.saksbehandler)
                    setString(15, maanedStatistikkRad.attestant)
                    setDate(16, Date.valueOf(maanedStatistikkRad.vedtakLoependeFom))
                    setDate(17, maanedStatistikkRad.vedtakLoependeTom?.let { Date.valueOf(it) })
                    setString(18, maanedStatistikkRad.statistikkMaaned.toString())
                    setString(19, maanedStatistikkRad.sakUtland.toString())
                    setDate(20, maanedStatistikkRad.virkningstidspunkt?.let { Date.valueOf(it.atDay(1)) })
                    setDate(21, maanedStatistikkRad.utbetalingsdato?.let { Date.valueOf(it) })
                    setString(22, maanedStatistikkRad.avkortingsbeloep)
                    setString(23, maanedStatistikkRad.aarsinntekt)
                    setString(24, maanedStatistikkRad.kilde.name)
                    maanedStatistikkRad.pesysId?.let { setLong(25, it) } ?: setNull(25, Types.BIGINT)
                    setString(26, maanedStatistikkRad.sakYtelsesgruppe?.name)
                    setString(27, maanedStatistikkRad.harAktivitetsplikt)
                    setString(28, maanedStatistikkRad.oppfyllerAktivitet?.toString())
                    setString(29, maanedStatistikkRad.aktivitet)
                    setString(30, maanedStatistikkRad.sanksjon)

                    setString(31, maanedStatistikkRad.etteroppgjoerAar?.toString())
                    setNullableLong(32, maanedStatistikkRad.etteroppgjoerUtbetalt)
                    setNullableLong(33, maanedStatistikkRad.etteroppgjoerNyStoenad)
                    setNullableLong(34, maanedStatistikkRad.etteroppgjoerDifferanse)
                    setString(35, maanedStatistikkRad.etteroppgjoerResultat)
                    setNullableLong(36, maanedStatistikkRad.etterbetaltBeloep)
                    setNullableLong(37, maanedStatistikkRad.tilbakekrevdBeloep)
                }.executeUpdate()
        }

    fun lagreStoenadsrad(stoenadsrad: StoenadRad): StoenadRad? {
        connection.use { conn ->
            val (statement, insertedRows) =
                conn
                    .prepareStatement(
                        """
                        INSERT INTO stoenad(
                            fnrSoeker, fnrForeldre, fnrSoesken, anvendtTrygdetid, nettoYtelse, ytelseFoerAvkorting,
                            beregningType, anvendtSats, 
                            behandlingId, sakId, sakNummer, tekniskTid, sakYtelse, versjon, saksbehandler, attestant, 
                            vedtakLoependeFom, vedtakLoependeTom, beregning, avkorting, vedtakType, sak_utland,
                            virkningstidspunkt, utbetalingsdato, kilde, pesysid, sakYtelsesgruppe, opphoerFom,
                            vedtaksperioder
                        ) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """.trimIndent(),
                        Statement.RETURN_GENERATED_KEYS,
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

    fun kjoertStatusForMaanedsstatistikk(maaned: YearMonth): KjoertStatus =
        connection.use { conn ->
            conn
                .prepareStatement(
                    """
                    SELECT id, statistikkMaaned, kjoertStatus, raderRegistrert, raderMedFeil 
                    FROM maanedsstatistikk_job 
                    WHERE statistikkMaaned = ?
                    """.trimIndent(),
                ).apply {
                    setString(1, maaned.toString())
                }.executeQuery()
                .toList {
                    MaanedstatistikkJobExecution(
                        id = getLong("id"),
                        statistikkMaaned = YearMonth.parse(getString("statistikkMaaned")),
                        kjoertStatus = KjoertStatus.valueOf(getString("kjoertStatus")),
                        raderRegistrert = getLong("raderRegistrert"),
                        raderMedFeil = getLong("raderMedFeil"),
                    )
                }.map { it.kjoertStatus }
                .toSet()
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

    fun lagreMaanedJobUtfoert(
        maaned: YearMonth,
        raderMedFeil: Long,
        raderRegistrert: Long,
    ) {
        val kjoertStatus =
            when (raderMedFeil) {
                0L -> KjoertStatus.INGEN_FEIL
                else -> KjoertStatus.FEIL
            }
        connection.use {
            it
                .prepareStatement(
                    """
                    INSERT INTO maanedsstatistikk_job (
                        statistikkMaaned, kjoertStatus, raderRegistrert, raderMedFeil
                    ) VALUES (?, ?, ?, ?)
                    """.trimIndent(),
                ).apply {
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
    val raderMedFeil: Long,
)

enum class KjoertStatus {
    INGEN_FEIL,
    FEIL,
    IKKE_KJOERT,
}

inline fun <reified T> PreparedStatement.setJsonb(
    parameterIndex: Int,
    jsonb: T,
): PreparedStatement {
    val jsonObject = PGobject()
    jsonObject.type = "json"
    jsonObject.value = objectMapper.writeValueAsString(jsonb)
    this.setObject(parameterIndex, jsonObject)
    return this
}

private fun PreparedStatement.setStoenadRad(stoenadsrad: StoenadRad): PreparedStatement =
    this.apply {
        setString(1, stoenadsrad.fnrSoeker)
        setJsonb(2, stoenadsrad.fnrForeldre)
        setJsonb(3, stoenadsrad.fnrSoesken)
        setString(4, stoenadsrad.anvendtTrygdetid)
        setString(5, stoenadsrad.nettoYtelse)
        setString(6, stoenadsrad.ytelseFoerAvkorting)
        setString(7, stoenadsrad.beregningType)
        setString(8, stoenadsrad.anvendtSats)
        setObject(9, stoenadsrad.behandlingId)
        setSakId(10, stoenadsrad.sakId)
        setSakId(11, stoenadsrad.sakId)
        setTidspunkt(12, stoenadsrad.tekniskTid)
        setString(13, stoenadsrad.sakYtelse)
        setString(14, stoenadsrad.versjon)
        setString(15, stoenadsrad.saksbehandler)
        setString(16, stoenadsrad.attestant)
        setDate(17, Date.valueOf(stoenadsrad.vedtakLoependeFom))
        setDate(18, stoenadsrad.vedtakLoependeTom?.let { Date.valueOf(it) })
        setJsonb(19, stoenadsrad.beregning)
        setJsonb(20, stoenadsrad.avkorting)
        setString(21, stoenadsrad.vedtakType?.toString())
        setString(22, stoenadsrad.sakUtland?.toString())
        setDate(23, Date.valueOf(stoenadsrad.virkningstidspunkt?.atDay(1)))
        setDate(24, stoenadsrad.utbetalingsdato?.let { Date.valueOf(it) })
        setString(25, stoenadsrad.vedtaksloesning.name)
        stoenadsrad.pesysId?.let { setLong(26, it) } ?: setNull(26, Types.BIGINT)
        setString(27, stoenadsrad.sakYtelsesgruppe?.name)
        setDate(28, stoenadsrad.opphoerFom?.let { Date.valueOf(it.atDay(1)) })
        setJsonb(29, stoenadsrad.vedtaksperioder)
    }

private fun ResultSet.asStoenadRad(): StoenadRad =
    StoenadRad(
        id = getLong("id"),
        fnrSoeker = getString("fnrSoeker"),
        fnrForeldre = objectMapper.readValue(getString("fnrForeldre"), Array<String>::class.java).toList(),
        fnrSoesken = objectMapper.readValue(getString("fnrSoesken"), Array<String>::class.java).toList(),
        anvendtTrygdetid = getString("anvendtTrygdetid"),
        nettoYtelse = getString("nettoYtelse"),
        ytelseFoerAvkorting = getString("ytelseFoerAvkorting"),
        beregningType = getString("beregningType"),
        anvendtSats = getString("anvendtSats"),
        behandlingId = getObject("behandlingId") as UUID,
        sakId = SakId(getLong("sakId")),
        sakNummer = getLong("sakNummer"),
        tekniskTid = getTimestamp("tekniskTid").toTidspunkt(),
        sakYtelse = getString("sakYtelse"),
        versjon = getString("versjon"),
        saksbehandler = getString("saksbehandler"),
        attestant = getString("attestant"),
        vedtakLoependeFom = getDate("vedtakLoependeFom").toLocalDate(),
        vedtakLoependeTom = getDate("vedtakLoependeTom")?.toLocalDate(),
        beregning = getString("beregning")?.let { objectMapper.readValue(it) },
        avkorting = getString("avkorting")?.let { objectMapper.readValue(it) },
        vedtakType = getString("vedtakType")?.let { enumValueOf<VedtakType>(it) },
        sakUtland = getString("sak_utland")?.let { enumValueOf<SakUtland>(it) },
        virkningstidspunkt = getDate("virkningstidspunkt")?.toLocalDate()?.let { YearMonth.of(it.year, it.monthValue) },
        utbetalingsdato = getDate("utbetalingsdato")?.toLocalDate(),
        // Feltet er renamet i intern modell, men databasen beholder kilde-navnet for å bevare eksternt
        // grensesnitt til dataprodukt
        vedtaksloesning = getString("kilde").let { Vedtaksloesning.valueOf(it) },
        pesysId = getLong("pesysid"),
        sakYtelsesgruppe = getString("sakYtelsesgruppe")?.let { enumValueOf<SakYtelsesgruppe>(it) },
        opphoerFom =
            getDate("opphoerFom")?.toLocalDate()?.let {
                YearMonth.of(it.year, it.monthValue)
            },
        vedtaksperioder = getString("vedtaksperioder")?.let { objectMapper.readValue(it) },
    )
