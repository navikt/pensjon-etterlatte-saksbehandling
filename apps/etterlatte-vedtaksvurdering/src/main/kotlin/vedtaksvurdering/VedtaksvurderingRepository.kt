package no.nav.etterlatte.vedtaksvurdering

import com.fasterxml.jackson.module.kotlin.readValue
import kotliquery.Row
import kotliquery.TransactionalSession
import kotliquery.queryOf
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.RevurderingAarsak
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.tidspunkt.toTidspunkt
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.libs.common.vedtak.Attestasjon
import no.nav.etterlatte.libs.common.vedtak.Periode
import no.nav.etterlatte.libs.common.vedtak.Utbetalingsperiode
import no.nav.etterlatte.libs.common.vedtak.UtbetalingsperiodeType
import no.nav.etterlatte.libs.common.vedtak.VedtakFattet
import no.nav.etterlatte.libs.common.vedtak.VedtakStatus
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import no.nav.etterlatte.libs.database.Transactions
import no.nav.etterlatte.libs.database.hent
import no.nav.etterlatte.libs.database.hentListe
import no.nav.etterlatte.libs.database.oppdater
import no.nav.etterlatte.libs.database.transaction
import java.sql.Date
import java.time.LocalDate
import java.time.YearMonth
import java.util.*
import javax.sql.DataSource

class VedtaksvurderingRepository(private val datasource: DataSource) : Transactions<VedtaksvurderingRepository> {
    companion object {
        fun using(datasource: DataSource): VedtaksvurderingRepository = VedtaksvurderingRepository(datasource)
    }

    override fun <T> inTransaction(block: VedtaksvurderingRepository.(TransactionalSession) -> T): T {
        return datasource.transaction(true) {
            this.block(it)
        }
    }

    fun opprettVedtak(opprettVedtak: OpprettVedtak, tx: TransactionalSession? = null): Vedtak =
        tx.session {
            queryOf(
                statement = """
                        INSERT INTO vedtak(
                            behandlingId, sakid, fnr, behandlingtype, saktype, vedtakstatus, type, datovirkfom, 
                            beregningsresultat, avkorting, vilkaarsresultat, revurderingsaarsak, revurderinginfo)
                        VALUES (:behandlingId, :sakid, :fnr, :behandlingtype, :saktype, :vedtakstatus, :type, 
                            :datovirkfom, :beregningsresultat, :avkorting, :vilkaarsresultat, :revurderingsaarsak,
                            :revurderinginfo)
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
                    "avkorting" to opprettVedtak.beregning?.toJson(),
                    "vilkaarsresultat" to opprettVedtak.vilkaarsvurdering?.toJson(),
                    "revurderingsaarsak" to opprettVedtak.revurderingsaarsak?.name,
                    "revurderinginfo" to opprettVedtak.revurderingInfo?.toJson()
                )
            )
                .let { query -> this.run(query.asUpdateAndReturnGeneratedKey) }
                ?.let { vedtakId ->
                    opprettUtbetalingsperioder(vedtakId, opprettVedtak.utbetalingsperioder, this)
                } ?: throw Exception("Kunne ikke opprette vedtak for behandling ${opprettVedtak.behandlingId}")
            return@session hentVedtak(opprettVedtak.behandlingId, this)
                ?: throw Exception("Kunne ikke opprette vedtak for behandling ${opprettVedtak.behandlingId}")
        }

    fun oppdaterVedtak(oppdatertVedtak: Vedtak, tx: TransactionalSession? = null): Vedtak =
        tx.session {
            queryOf(
                statement = """
                        UPDATE vedtak 
                        SET datovirkfom = :datovirkfom, type = :type, 
                            beregningsresultat = :beregningsresultat, avkorting = :avkorting,
                             vilkaarsresultat = :vilkaarsresultat, revurderinginfo = :revurderinginfo
                        WHERE behandlingId = :behandlingid
                        """,
                mapOf(
                    "datovirkfom" to oppdatertVedtak.virkningstidspunkt.atDay(1),
                    "type" to oppdatertVedtak.type.name,
                    "beregningsresultat" to oppdatertVedtak.beregning?.toJson(),
                    "avkorting" to oppdatertVedtak.beregning?.toJson(),
                    "vilkaarsresultat" to oppdatertVedtak.vilkaarsvurdering?.toJson(),
                    "behandlingid" to oppdatertVedtak.behandlingId,
                    "revurderinginfo" to oppdatertVedtak.revurderingInfo?.toJson()
                )
            ).let { query -> this.run(query.asUpdate) }

            slettUtbetalingsperioder(oppdatertVedtak.id, this)
            opprettUtbetalingsperioder(oppdatertVedtak.id, oppdatertVedtak.utbetalingsperioder, this)

            return@session hentVedtak(oppdatertVedtak.behandlingId, this)
                ?: throw Exception("Kunne ikke oppdatere vedtak for behandling ${oppdatertVedtak.behandlingId}")
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

    fun hentVedtak(
        vedtakId: Long,
        tx: TransactionalSession? = null
    ): Vedtak? =
        tx.session {
            hent(
                queryString = """
            SELECT sakid, behandlingId, saksbehandlerId, beregningsresultat, avkorting, vilkaarsresultat, id, fnr, 
                datoFattet, datoattestert, attestant, datoVirkFom, vedtakstatus, saktype, behandlingtype, 
                attestertVedtakEnhet, fattetVedtakEnhet, type, revurderingsaarsak, revurderinginfo
            FROM vedtak 
            WHERE id = :vedtakId
            """,
                params = mapOf("vedtakId" to vedtakId)
            ) {
                val utbetalingsperioder = hentUtbetalingsPerioder(it.long("id"), this)
                it.toVedtak(utbetalingsperioder)
            }
        }

    fun hentVedtak(behandlingId: UUID, tx: TransactionalSession? = null): Vedtak? =
        tx.session {
            hent(
                queryString = """
            SELECT sakid, behandlingId, saksbehandlerId, beregningsresultat, avkorting, vilkaarsresultat, id, fnr, 
                datoFattet, datoattestert, attestant, datoVirkFom, vedtakstatus, saktype, behandlingtype, 
                attestertVedtakEnhet, fattetVedtakEnhet, type, revurderingsaarsak, revurderinginfo
            FROM vedtak 
            WHERE behandlingId = :behandlingId
            """,
                params = mapOf("behandlingId" to behandlingId)
            ) {
                val utbetalingsperioder = hentUtbetalingsPerioder(it.long("id"), this)
                it.toVedtak(utbetalingsperioder)
            }
        }

    private fun hentVedtakNonNull(behandlingId: UUID, tx: TransactionalSession? = null): Vedtak =
        requireNotNull(hentVedtak(behandlingId, tx)) { "Fant ikke vedtak for behandling $behandlingId" }

    fun hentVedtakForSak(sakId: Long, tx: TransactionalSession? = null): List<Vedtak> {
        val hentVedtak = """
            SELECT sakid, behandlingId, saksbehandlerId, beregningsresultat, avkorting, vilkaarsresultat, id, fnr, 
                datoFattet, datoattestert, attestant, datoVirkFom, vedtakstatus, saktype, behandlingtype, 
                attestertVedtakEnhet, fattetVedtakEnhet, type, revurderingsaarsak, revurderinginfo
            FROM vedtak  
            WHERE sakId = :sakId
            """
        return tx.session {
            hentListe(
                queryString = hentVedtak,
                params = { mapOf("sakId" to sakId) }
            ) {
                val utbetalingsperioder = hentUtbetalingsPerioder(it.long("id"), this)
                it.toVedtak(utbetalingsperioder)
            }
        }
    }

    /**
     * TODO vedtakstatus SAMORDNET e.l.?
     */
    fun hentFerdigstilteVedtak(
        fnr: Folkeregisteridentifikator,
        virkFom: LocalDate,
        tx: TransactionalSession? = null
    ): List<Vedtak> {
        val hentVedtak = """
            SELECT sakid, behandlingId, saksbehandlerId, beregningsresultat, avkorting, vilkaarsresultat, id, fnr, 
                datoFattet, datoattestert, attestant, datoVirkFom, vedtakstatus, saktype, behandlingtype, 
                attestertVedtakEnhet, fattetVedtakEnhet, type, revurderingsaarsak, revurderinginfo
            FROM vedtak  
            WHERE vedtakstatus = 'IVERKSATT'   
            AND fnr = :fnr
            AND datoVirkFom >= :virkFom
            """
        return tx.session {
            hentListe(
                queryString = hentVedtak,
                params = { mapOf("fnr" to fnr.value, "virkFom" to virkFom) }
            ) {
                val utbetalingsperioder = hentUtbetalingsPerioder(it.long("id"), this)
                it.toVedtak(utbetalingsperioder)
            }
        }
    }

    private fun hentUtbetalingsPerioder(vedtakId: Long, tx: TransactionalSession? = null): List<Utbetalingsperiode> =
        tx.session {
            hentListe(
                queryString = "SELECT * FROM utbetalingsperiode WHERE vedtakid = :vedtakid",
                params = { mapOf("vedtakid" to vedtakId) }
            ) { it.toUtbetalingsperiode() }
        }

    fun fattVedtak(behandlingId: UUID, vedtakFattet: VedtakFattet, tx: TransactionalSession? = null): Vedtak =
        tx.session {
            oppdater(
                query = """
                UPDATE vedtak 
                SET saksbehandlerId = :saksbehandlerId, fattetVedtakEnhet = :saksbehandlerEnhet, datoFattet = now(), 
                    vedtakstatus = :vedtakstatus  
                WHERE behandlingId = :behandlingId
                """,
                params = mapOf(
                    "saksbehandlerId" to vedtakFattet.ansvarligSaksbehandler,
                    "saksbehandlerEnhet" to vedtakFattet.ansvarligEnhet,
                    "vedtakstatus" to VedtakStatus.FATTET_VEDTAK.name,
                    "behandlingId" to behandlingId
                ),
                loggtekst = "Fatter vedtok for behandling $behandlingId"
            )
                .also { require(it == 1) }
                .let { hentVedtakNonNull(behandlingId, this) }
        }

    fun attesterVedtak(behandlingId: UUID, attestasjon: Attestasjon, tx: TransactionalSession? = null): Vedtak =
        tx.session {
            oppdater(
                query = """
                UPDATE vedtak 
                SET attestant = :attestant, attestertVedtakEnhet = :attestertVedtakEnhet, datoAttestert = now(), 
                    vedtakstatus = :vedtakstatus 
                WHERE behandlingId = :behandlingId
                """,
                params = mapOf(
                    "attestant" to attestasjon.attestant,
                    "attestertVedtakEnhet" to attestasjon.attesterendeEnhet,
                    "vedtakstatus" to VedtakStatus.ATTESTERT.name,
                    "behandlingId" to behandlingId
                ),
                loggtekst = "Attesterer vedtak $behandlingId"
            )
                .also { require(it == 1) }
            return@session hentVedtakNonNull(behandlingId, this)
        }

    fun underkjennVedtak(behandlingId: UUID, tx: TransactionalSession? = null): Vedtak =
        tx.session {
            oppdater(
                """
            UPDATE vedtak 
            SET attestant = null, datoAttestert = null, attestertVedtakEnhet = null, saksbehandlerId = null, 
                datoFattet = null, fattetVedtakEnhet = null, vedtakstatus = :vedtakstatus 
            WHERE behandlingId = :behandlingId
            """,
                params = mapOf("vedtakstatus" to VedtakStatus.RETURNERT.name, "behandlingId" to behandlingId),
                loggtekst = "Underkjenner vedtak for behandling $behandlingId"
            )
                .also { require(it == 1) }
            return@session hentVedtakNonNull(behandlingId, this)
        }

    fun iverksattVedtak(behandlingId: UUID, tx: TransactionalSession? = null): Vedtak = tx.session {
        oppdater(
            query = "UPDATE vedtak SET vedtakstatus = :vedtakstatus WHERE behandlingId = :behandlingId",
            params = mapOf("vedtakstatus" to VedtakStatus.IVERKSATT.name, "behandlingId" to behandlingId),
            loggtekst = "Lagrer iverksatt vedtak"
        )
            .also { require(it == 1) }
        return@session hentVedtakNonNull(behandlingId, this)
    }

    private fun Row.toVedtak(utbetalingsperioder: List<Utbetalingsperiode>) = Vedtak(
        id = long("id"),
        sakId = long("sakid"),
        sakType = SakType.valueOf(string("saktype")),
        behandlingId = uuid("behandlingid"),
        behandlingType = BehandlingType.valueOf(string("behandlingtype")),
        soeker = string("fnr").let { Folkeregisteridentifikator.of(it) },
        status = string("vedtakstatus").let { VedtakStatus.valueOf(it) },
        type = string("type").let { VedtakType.valueOf(it) },
        virkningstidspunkt = sqlDate("datovirkfom").toLocalDate().let { YearMonth.from(it) },
        vilkaarsvurdering = stringOrNull("vilkaarsresultat")?.let { objectMapper.readValue(it) },
        beregning = stringOrNull("beregningsresultat")?.let { objectMapper.readValue(it) },
        avkorting = stringOrNull("avkorting")?.let { objectMapper.readValue(it) },
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
        utbetalingsperioder = utbetalingsperioder,
        revurderingAarsak = stringOrNull("revurderingsaarsak")?.let { RevurderingAarsak.valueOf(it) },
        revurderingInfo = stringOrNull("revurderinginfo")?.let { objectMapper.readValue(it) }
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

    fun tilbakestillIkkeIverksatteVedtak(behandlingId: UUID, tx: TransactionalSession? = null): Vedtak? {
        val hentVedtak = hentVedtak(behandlingId, tx)
        if (hentVedtak?.status != VedtakStatus.FATTET_VEDTAK) {
            return null
        }
        return tx.session {
            oppdater(
                query = """
                UPDATE vedtak 
                SET vedtakstatus = :vedtakstatus 
                WHERE behandlingId = :behandlingId
                """,
                params = mapOf(
                    "vedtakstatus" to VedtakStatus.RETURNERT.name,
                    "behandlingId" to behandlingId
                ),
                loggtekst = "Returnerer vedtak $behandlingId"
            )
                .also { require(it == 1) }
            return@session hentVedtakNonNull(behandlingId, this)
        }
    }
}