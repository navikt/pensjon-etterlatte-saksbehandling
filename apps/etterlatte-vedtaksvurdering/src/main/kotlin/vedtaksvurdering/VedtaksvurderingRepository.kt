package no.nav.etterlatte.vedtaksvurdering

import com.fasterxml.jackson.module.kotlin.readValue
import kotliquery.Row
import kotliquery.TransactionalSession
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.person.Foedselsnummer
import no.nav.etterlatte.libs.common.tidspunkt.toTidspunkt
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.libs.common.vedtak.Attestasjon
import no.nav.etterlatte.libs.common.vedtak.Periode
import no.nav.etterlatte.libs.common.vedtak.Utbetalingsperiode
import no.nav.etterlatte.libs.common.vedtak.UtbetalingsperiodeType
import no.nav.etterlatte.libs.common.vedtak.VedtakFattet
import no.nav.etterlatte.libs.common.vedtak.VedtakStatus
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import org.slf4j.LoggerFactory
import java.sql.Date
import java.time.YearMonth
import java.util.*
import javax.sql.DataSource

class VedtaksvurderingRepository(private val datasource: DataSource) {
    private val logger = LoggerFactory.getLogger(VedtaksvurderingRepository::class.java)

    companion object {
        fun using(datasource: DataSource): VedtaksvurderingRepository = VedtaksvurderingRepository(datasource)
    }

    fun opprettVedtak(opprettVedtak: OpprettVedtak): Vedtak =
        using(sessionOf(datasource, returnGeneratedKey = true)) { session ->
            session.transaction { tx ->
                queryOf(
                    statement = """
                        INSERT INTO vedtak(
                            behandlingId, sakid, fnr, behandlingtype, saktype, vedtakstatus, type, datovirkfom, 
                            beregningsresultat, vilkaarsresultat)
                        VALUES (:behandlingId, :sakid, :fnr, :behandlingtype, :saktype, :vedtakstatus, :type, 
                            :datovirkfom, :beregningsresultat, :vilkaarsresultat)
                        RETURNING id
                        """,
                    mapOf(
                        "behandlingId" to opprettVedtak.behandlingId,
                        "behandlingtype" to opprettVedtak.behandlingType.name,
                        "sakid" to opprettVedtak.sakId,
                        "saktype" to opprettVedtak.sakType.name,
                        "fnr" to opprettVedtak.soeker.value,
                        "vedtakstatus" to opprettVedtak.status.name,
                        "type" to opprettVedtak.type.name,
                        "datovirkfom" to opprettVedtak.virkningstidspunkt.atDay(1),
                        "beregningsresultat" to opprettVedtak.beregning?.toJson(),
                        "vilkaarsresultat" to opprettVedtak.vilkaarsvurdering?.toJson()
                    )
                )
                    .let { query -> tx.run(query.asUpdateAndReturnGeneratedKey) }
                    ?.let { vedtakId ->
                        opprettUtbetalingsperioder(vedtakId, opprettVedtak.utbetalingsperioder, tx)
                    } ?: throw Exception("Kunne ikke opprette vedtak for behandling ${opprettVedtak.behandlingId}")
            }.let {
                hentVedtak(opprettVedtak.behandlingId)
                    ?: throw Exception("Kunne ikke opprette vedtak for behandling ${opprettVedtak.behandlingId}")
            }
        }

    fun oppdaterVedtak(oppdatertVedtak: Vedtak): Vedtak =
        using(sessionOf(datasource)) { session ->
            session.transaction { tx ->
                queryOf(
                    statement = """
                        UPDATE vedtak 
                        SET datovirkfom = :datovirkfom, type = :type, 
                            beregningsresultat = :beregningsresultat, vilkaarsresultat = :vilkaarsresultat 
                        WHERE behandlingId = :behandlingid
                        """,
                    mapOf(
                        "datovirkfom" to oppdatertVedtak.virkningstidspunkt.atDay(1),
                        "type" to oppdatertVedtak.type.name,
                        "beregningsresultat" to oppdatertVedtak.beregning?.toJson(),
                        "vilkaarsresultat" to oppdatertVedtak.vilkaarsvurdering?.toJson(),
                        "behandlingid" to oppdatertVedtak.behandlingId
                    )
                ).let { query -> tx.run(query.asUpdate) }

                slettUtbetalingsperioder(oppdatertVedtak.id, tx)
                opprettUtbetalingsperioder(oppdatertVedtak.id, oppdatertVedtak.utbetalingsperioder, tx)
            }.let {
                hentVedtak(oppdatertVedtak.behandlingId)
                    ?: throw Exception("Kunne ikke oppdatere vedtak for behandling ${oppdatertVedtak.behandlingId}")
            }
        }

    private fun slettUtbetalingsperioder(vedtakId: Long, tx: TransactionalSession) =
        queryOf(
            statement = """
                DELETE FROM utbetalingsperiode
                WHERE vedtakid = :vedtakid
                """,
            paramMap = mapOf(
                "vedtakid" to vedtakId
            )
        ).let { query -> tx.run(query.asUpdate) }

    private fun opprettUtbetalingsperioder(
        vedtakId: Long,
        utbetalingsperioder: List<Utbetalingsperiode>,
        tx: TransactionalSession
    ) =
        utbetalingsperioder.forEach {
            queryOf(
                statement = """
                    INSERT INTO utbetalingsperiode(vedtakid, datofom, datotom, type, beloep) 
                    VALUES (:vedtakid, :datofom, :datotom, :type, :beloep)
                    """,
                paramMap = mapOf(
                    "vedtakid" to vedtakId,
                    "datofom" to it.periode.fom.atDay(1).let(Date::valueOf),
                    "datotom" to it.periode.tom?.atEndOfMonth()?.let(Date::valueOf),
                    "type" to it.type.name,
                    "beloep" to it.beloep
                )
            ).let { query -> tx.run(query.asUpdate) }
        }

    fun hentVedtak(behandlingId: UUID): Vedtak? =
        using(sessionOf(datasource)) { session ->
            queryOf(
                statement = """
                    SELECT sakid, behandlingId, saksbehandlerId, beregningsresultat, vilkaarsresultat, id, fnr, 
                        datoFattet, datoattestert, attestant, datoVirkFom, vedtakstatus, saktype, behandlingtype, 
                        attestertVedtakEnhet, fattetVedtakEnhet, type  
                    FROM vedtak 
                    WHERE behandlingId = :behandlingId
                """,
                paramMap = mapOf("behandlingId" to behandlingId)
            ).let { query ->
                session.run(query.map { row -> row.toVedtak(hentUtbetalingsPerioder(row.long("id"))) }.asSingle)
            }
        }

    private fun hentVedtakNonNull(behandlingId: UUID): Vedtak =
        requireNotNull(hentVedtak(behandlingId)) { "Fant ikke vedtak for behandling $behandlingId" }

    fun hentVedtakForSak(sakId: Long): List<Vedtak> {
        val hentVedtak = """
            SELECT sakid, behandlingId, saksbehandlerId, beregningsresultat, vilkaarsresultat, id, fnr, 
                datoFattet, datoattestert, attestant, datoVirkFom, vedtakstatus, saktype, behandlingtype, 
                attestertVedtakEnhet, fattetVedtakEnhet, type  
            FROM vedtak  
            WHERE sakId = :sakId
            """
        return using(sessionOf(datasource)) { session ->
            queryOf(
                statement = hentVedtak,
                paramMap = mapOf("sakId" to sakId)
            ).let { query ->
                session.run(
                    query.map { row ->
                        val utbetalingsperioder = hentUtbetalingsPerioder(row.long("id"))
                        row.toVedtak(utbetalingsperioder)
                    }.asList
                )
            }
        }
    }

    private fun hentUtbetalingsPerioder(vedtakId: Long): List<Utbetalingsperiode> =
        using(sessionOf(datasource)) { session ->
            queryOf(
                statement = "SELECT * FROM utbetalingsperiode WHERE vedtakid = :vedtakid",
                paramMap = mapOf("vedtakid" to vedtakId)
            ).let { query ->
                session.run(query.map { row -> row.toUtbetalingsperiode() }.asList)
            }
        }

    fun fattVedtak(behandlingId: UUID, vedtakFattet: VedtakFattet): Vedtak =
        using(sessionOf(datasource)) { session ->
            session.transaction { tx ->
                queryOf(
                    statement = """
                        UPDATE vedtak 
                        SET saksbehandlerId = :saksbehandlerId, fattetVedtakEnhet = :saksbehandlerEnhet, datoFattet = now(), 
                            vedtakstatus = :vedtakstatus  
                        WHERE behandlingId = :behandlingId
                    """,
                    paramMap = mapOf<String, Any>(
                        "saksbehandlerId" to vedtakFattet.ansvarligSaksbehandler,
                        "saksbehandlerEnhet" to vedtakFattet.ansvarligEnhet,
                        "vedtakstatus" to VedtakStatus.FATTET_VEDTAK.name,
                        "behandlingId" to behandlingId
                    )
                )
                    .also { logger.info("Fatter vedtok for behandling $behandlingId") }
                    .let { tx.run(it.asUpdate) }
            }
        }
            .also { require(it == 1) }
            .let { hentVedtakNonNull(behandlingId) }

    fun attesterVedtak(behandlingId: UUID, attestasjon: Attestasjon): Vedtak =
        using(sessionOf(datasource)) { session ->
            session.transaction { tx ->
                queryOf(
                    statement = """
                        UPDATE vedtak 
                        SET attestant = :attestant, attestertVedtakEnhet = :attestertVedtakEnhet, datoAttestert = now(), 
                            vedtakstatus = :vedtakstatus 
                        WHERE behandlingId = :behandlingId
                    """,
                    paramMap = mapOf<String, Any>(
                        "attestant" to attestasjon.attestant,
                        "attestertVedtakEnhet" to attestasjon.attesterendeEnhet,
                        "vedtakstatus" to VedtakStatus.ATTESTERT.name,
                        "behandlingId" to behandlingId
                    )
                )
                    .also { logger.info("Attesterer vedtak $behandlingId") }
                    .let { tx.run(it.asUpdate) }
            }
        }
            .also { require(it == 1) }
            .let { hentVedtakNonNull(behandlingId) }

    fun underkjennVedtak(behandlingId: UUID): Vedtak =
        using(sessionOf(datasource)) { session ->
            session.transaction { tx ->
                queryOf(
                    statement = """
                        UPDATE vedtak 
                        SET attestant = null, datoAttestert = null, attestertVedtakEnhet = null, saksbehandlerId = null, 
                            datoFattet = null, fattetVedtakEnhet = null, vedtakstatus = :vedtakstatus 
                        WHERE behandlingId = :behandlingId
                    """.trimMargin(),
                    paramMap = mapOf<String, Any>(
                        "vedtakstatus" to VedtakStatus.RETURNERT.name,
                        "behandlingId" to behandlingId
                    )
                ).let {
                    logger.info("Underkjenner vedtak for behandling $behandlingId")
                    tx.run(it.asUpdate)
                }
            }
        }
            .also { require(it == 1) }
            .let { hentVedtakNonNull(behandlingId) }

    fun iverksattVedtak(behandlingId: UUID): Vedtak = using(sessionOf(datasource)) { session ->
        session.transaction { tx ->
            queryOf(
                statement = "UPDATE vedtak SET vedtakstatus = :vedtakstatus WHERE behandlingId = :behandlingId",
                paramMap = mapOf<String, Any>(
                    "vedtakstatus" to VedtakStatus.IVERKSATT.name,
                    "behandlingId" to behandlingId
                )
            ).let {
                logger.info("Lagrer iverksatt vedtak")
                tx.run(it.asUpdate)
            }
        }
    }
        .also { require(it == 1) }
        .let { hentVedtakNonNull(behandlingId) }

    private fun Row.toVedtak(utbetalingsperioder: List<Utbetalingsperiode>) = Vedtak(
        id = long("id"),
        sakId = long("sakid"),
        sakType = SakType.valueOf(string("saktype")),
        behandlingId = uuid("behandlingid"),
        behandlingType = BehandlingType.valueOf(string("behandlingtype")),
        soeker = string("fnr").let { Foedselsnummer.of(it) },
        status = string("vedtakstatus").let { VedtakStatus.valueOf(it) },
        type = string("type").let { VedtakType.valueOf(it) },
        virkningstidspunkt = sqlDate("datovirkfom").toLocalDate().let { YearMonth.from(it) },
        vilkaarsvurdering = stringOrNull("vilkaarsresultat")?.let { objectMapper.readValue(it) },
        beregning = stringOrNull("beregningsresultat")?.let { objectMapper.readValue(it) },
        vedtakFattet = stringOrNull("saksbehandlerid")?.let {
            VedtakFattet(
                ansvarligSaksbehandler = string("saksbehandlerid"),
                ansvarligEnhet = string("fattetVedtakEnhet"),
                tidspunkt = sqlTimestamp("datofattet").toTidspunkt()
            )
        },
        attestasjon = stringOrNull("attestant")?.let {
            Attestasjon(
                attestant = string("attestant"),
                attesterendeEnhet = string("attestertVedtakEnhet"),
                tidspunkt = sqlTimestamp("datoattestert").toTidspunkt()
            )
        },
        utbetalingsperioder = utbetalingsperioder
    )

    private fun Row.toUtbetalingsperiode() =
        Utbetalingsperiode(
            id = long("id"),
            periode = Periode(
                fom = YearMonth.from(localDate("datoFom")),
                tom = localDateOrNull("datoTom")?.let(YearMonth::from)
            ),
            beloep = bigDecimalOrNull("beloep"),
            type = UtbetalingsperiodeType.valueOf(string("type"))
        )

    fun tilbakestillIkkeIverksatteVedtak(behandlingId: UUID): Vedtak? {
        val hentVedtak = hentVedtak(behandlingId)
        if (hentVedtak?.status != VedtakStatus.FATTET_VEDTAK) {
            return null
        }
        return using(sessionOf(datasource)) { session ->
            session.transaction { tx ->
                queryOf(
                    statement = """
                        UPDATE vedtak 
                        SET vedtakstatus = :vedtakstatus 
                        WHERE behandlingId = :behandlingId
                    """,
                    paramMap = mapOf<String, Any>(
                        "vedtakstatus" to VedtakStatus.RETURNERT.name,
                        "behandlingId" to behandlingId
                    )
                )
                    .also { logger.info("Returnerer vedtak $behandlingId") }
                    .let { tx.run(it.asUpdate) }
            }
        }
            .also { require(it == 1) }
            .let { hentVedtakNonNull(behandlingId) }
    }
}