package no.nav.etterlatte.vedtaksvurdering

import com.fasterxml.jackson.module.kotlin.readValue
import kotliquery.Row
import kotliquery.TransactionalSession
import kotliquery.queryOf
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.Revurderingaarsak
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
import no.nav.etterlatte.libs.database.opprett
import no.nav.etterlatte.libs.database.transaction
import java.sql.Date
import java.time.YearMonth
import java.util.UUID
import javax.sql.DataSource

private val sakeneMedMismatchIAntallPerioder =
    arrayOf<Long>(
        17340,
        16311,
        12904,
        14854,
        16221,
        11871,
        11913,
        12298,
        17281,
        17201,
        17170,
        17206,
        17059,
        17042,
        16889,
        16735,
        16664,
        16259,
        16232,
        16261,
        16223,
        16275,
        16265,
        16332,
        16195,
        16187,
        16183,
        16087,
        16179,
        16117,
        16104,
        16156,
        16076,
        16059,
        16044,
        16056,
        16036,
        15976,
        15969,
        16015,
        16008,
        15986,
        15966,
        15943,
        15982,
        15896,
        15909,
        15891,
        15895,
        15850,
        15861,
        15823,
        15799,
        15856,
        15834,
        15839,
        15838,
        16039,
        16160,
        16167,
        16164,
        16065,
        16054,
        15758,
        15746,
        15727,
        15709,
        15745,
        15711,
        15658,
        15641,
        15625,
        15628,
        15577,
        15598,
        15591,
        15578,
        15511,
        15501,
        15498,
        15494,
        15448,
        15445,
        15477,
        15461,
        15450,
        15438,
        15421,
        15455,
        15410,
        15356,
        15388,
        15366,
        15378,
        15546,
        15392,
        15371,
        15336,
        15298,
        15347,
        15280,
        15252,
        15265,
        15218,
        15246,
        15237,
        15206,
        15169,
        15104,
        15098,
        15066,
        15058,
        15080,
        15065,
        15131,
        15123,
        15047,
        15029,
        14975,
        14910,
        14901,
        14879,
        14945,
        15017,
        14965,
        14875,
        14877,
        14831,
        14811,
        14757,
        14723,
        14828,
        14776,
        14840,
        14706,
        14702,
        14726,
        14708,
        14730,
        14684,
        14643,
        14944,
        14902,
        14793,
        14539,
        14537,
        14623,
        14555,
        14620,
        14608,
        14565,
        14541,
        14454,
        14451,
        14428,
        14532,
        14404,
        14384,
        14344,
        14378,
        14367,
        14356,
        14352,
        14373,
        14399,
        14386,
        14272,
        14280,
        14346,
        14301,
        14369,
        14299,
        14229,
        14221,
        14235,
        14231,
        14205,
        14186,
        14153,
        14146,
        14119,
        14097,
        13987,
        13965,
        13980,
        13925,
        13959,
        13992,
        13913,
        13878,
        13872,
        13866,
        13921,
        13819,
        13927,
        14402,
        13768,
        14246,
        13817,
        13797,
        13835,
        13847,
        13794,
        13760,
        13818,
        14185,
        13975,
        13856,
        14033,
        13851,
        13574,
        13706,
        13727,
        13576,
        13769,
        13492,
        13697,
        13609,
        13668,
        13664,
        13522,
        14484,
        14171,
        13495,
        13699,
        13654,
        13544,
        13560,
        13615,
        13497,
        13712,
        13504,
        13485,
        13483,
        13555,
        13508,
        13533,
        13455,
        13399,
        13465,
        13452,
        13400,
        13429,
        13415,
        13358,
        13351,
        13362,
        13170,
        13189,
        13250,
        13124,
        13047,
        12891,
        12831,
        12592,
        12608,
        12547,
        12565,
        12394,
        12576,
        12253,
        12262,
        11944,
        11937,
        11633,
        11474,
        12541,
        17258,
    )

class VedtaksvurderingRepository(
    private val datasource: DataSource,
) : Transactions<VedtaksvurderingRepository> {
    companion object {
        fun using(datasource: DataSource): VedtaksvurderingRepository = VedtaksvurderingRepository(datasource)
    }

    override fun <T> inTransaction(block: VedtaksvurderingRepository.(TransactionalSession) -> T): T =
        datasource.transaction(true) {
            this.block(it)
        }

    fun opprettVedtak(
        opprettVedtak: OpprettVedtak,
        tx: TransactionalSession? = null,
    ): Vedtak =
        tx.session {
            val innholdParams =
                when (opprettVedtak.innhold) {
                    is VedtakInnhold.Tilbakekreving -> mapOf("tilbakekreving" to opprettVedtak.innhold.tilbakekreving.toJson())
                    is VedtakInnhold.Klage -> mapOf("klage" to opprettVedtak.innhold.klage.toJson())
                    is VedtakInnhold.Behandling ->
                        opprettVedtak.innhold.let {
                            mapOf(
                                "behandlingtype" to it.behandlingType.name,
                                "datovirkfom" to it.virkningstidspunkt.atDay(1),
                                "beregningsresultat" to it.beregning?.toJson(),
                                "avkorting" to it.avkorting?.toJson(),
                                "vilkaarsresultat" to it.vilkaarsvurdering?.toJson(),
                                "revurderingsaarsak" to it.revurderingAarsak?.name,
                                "revurderinginfo" to it.revurderingInfo?.toJson(),
                                "opphoer_fom" to it.opphoerFraOgMed?.atDay(1),
                            )
                        }
                }
            queryOf(
                statement = """
                        INSERT INTO vedtak(
                            behandlingId, sakid, fnr, behandlingtype, saktype, vedtakstatus, type, datovirkfom, 
                            beregningsresultat, avkorting, vilkaarsresultat, revurderingsaarsak, revurderinginfo,
                            tilbakekreving, klage, opphoer_fom)
                        VALUES (:behandlingId, :sakid, :fnr, :behandlingtype, :saktype, :vedtakstatus, :type, 
                            :datovirkfom, :beregningsresultat, :avkorting, :vilkaarsresultat, :revurderingsaarsak,
                            :revurderinginfo, :tilbakekreving, :klage, :opphoer_fom)
                        RETURNING id
                        """,
                mapOf(
                    "behandlingId" to opprettVedtak.behandlingId,
                    "sakid" to opprettVedtak.sakId,
                    "saktype" to opprettVedtak.sakType.name,
                    "fnr" to opprettVedtak.soeker.value,
                    "vedtakstatus" to opprettVedtak.status.name,
                    "type" to opprettVedtak.type.name,
                ) + innholdParams,
            ).let { query -> this.run(query.asUpdateAndReturnGeneratedKey) }
                ?.let { vedtakId ->
                    if (opprettVedtak.innhold is VedtakInnhold.Behandling) {
                        opprettUtbetalingsperioder(vedtakId, opprettVedtak.innhold.utbetalingsperioder, this)
                    }
                } ?: throw Exception("Kunne ikke opprette vedtak for behandling ${opprettVedtak.behandlingId}")
            return@session hentVedtak(opprettVedtak.behandlingId, this)
                ?: throw Exception("Kunne ikke opprette vedtak for behandling ${opprettVedtak.behandlingId}")
        }

    fun oppdaterVedtak(
        oppdatertVedtak: Vedtak,
        tx: TransactionalSession? = null,
    ): Vedtak =
        tx.session {
            val paramMap =
                when (oppdatertVedtak.innhold) {
                    is VedtakInnhold.Behandling ->
                        mapOf(
                            "datovirkfom" to oppdatertVedtak.innhold.virkningstidspunkt.atDay(1),
                            "beregningsresultat" to oppdatertVedtak.innhold.beregning?.toJson(),
                            "avkorting" to oppdatertVedtak.innhold.avkorting?.toJson(),
                            "vilkaarsresultat" to oppdatertVedtak.innhold.vilkaarsvurdering?.toJson(),
                            "revurderinginfo" to oppdatertVedtak.innhold.revurderingInfo?.toJson(),
                            "opphoer_fom" to oppdatertVedtak.innhold.opphoerFraOgMed?.atDay(1),
                        )
                    is VedtakInnhold.Tilbakekreving ->
                        mapOf(
                            "tilbakekreving" to oppdatertVedtak.innhold.tilbakekreving.toJson(),
                        )
                    is VedtakInnhold.Klage ->
                        mapOf(
                            "klage" to oppdatertVedtak.innhold.klage.toJson(),
                        )
                } +
                    mapOf(
                        "behandlingid" to oppdatertVedtak.behandlingId,
                        "type" to oppdatertVedtak.type.name,
                    )

            queryOf(
                statement = """
                        UPDATE vedtak 
                        SET datovirkfom = :datovirkfom, type = :type, 
                            beregningsresultat = :beregningsresultat, avkorting = :avkorting,
                            vilkaarsresultat = :vilkaarsresultat, revurderinginfo = :revurderinginfo,
                            opphoer_fom = :opphoer_fom, tilbakekreving = :tilbakekreving,
                            klage = :klage
                        WHERE behandlingId = :behandlingid
                        """,
                paramMap = paramMap,
            ).let { query -> this.run(query.asUpdate) }

            if (oppdatertVedtak.innhold is VedtakInnhold.Behandling) {
                slettUtbetalingsperioder(oppdatertVedtak.id, this)
                opprettUtbetalingsperioder(oppdatertVedtak.id, oppdatertVedtak.innhold.utbetalingsperioder, this)
            }
            return@session hentVedtak(oppdatertVedtak.behandlingId, this)
                ?: throw Exception("Kunne ikke oppdatere vedtak for behandling ${oppdatertVedtak.behandlingId}")
        }

    private fun slettUtbetalingsperioder(
        vedtakId: Long,
        tx: TransactionalSession,
    ) = queryOf(
        statement = """
                DELETE FROM utbetalingsperiode
                WHERE vedtakid = :vedtakid
                """,
        paramMap =
            mapOf(
                "vedtakid" to vedtakId,
            ),
    ).let { query -> tx.run(query.asUpdate) }

    private fun opprettUtbetalingsperioder(
        vedtakId: Long,
        utbetalingsperioder: List<Utbetalingsperiode>,
        tx: TransactionalSession,
    ) = utbetalingsperioder.forEach {
        queryOf(
            statement = """
                    INSERT INTO utbetalingsperiode(vedtakid, datofom, datotom, type, beloep) 
                    VALUES (:vedtakid, :datofom, :datotom, :type, :beloep)
                    """,
            paramMap =
                mapOf(
                    "vedtakid" to vedtakId,
                    "datofom" to
                        it.periode.fom
                            .atDay(1)
                            .let(Date::valueOf),
                    "datotom" to
                        it.periode.tom
                            ?.atEndOfMonth()
                            ?.let(Date::valueOf),
                    "type" to it.type.name,
                    "beloep" to it.beloep,
                ),
        ).let { query -> tx.run(query.asUpdate) }
    }

    fun hentVedtak(
        vedtakId: Long,
        tx: TransactionalSession? = null,
    ): Vedtak? =
        tx.session {
            hent(
                queryString = """
            SELECT sakid, behandlingId, saksbehandlerId, beregningsresultat, avkorting, vilkaarsresultat, id, fnr, 
                datoFattet, datoattestert, attestant, datoVirkFom, vedtakstatus, saktype, behandlingtype, 
                attestertVedtakEnhet, fattetVedtakEnhet, type, revurderingsaarsak, revurderinginfo, opphoer_fom,
                tilbakekreving, klage 
            FROM vedtak 
            WHERE id = :vedtakId
            """,
                params = mapOf("vedtakId" to vedtakId),
            ) {
                it.toVedtak(emptyList())
            }
        }

    fun hentVedtak(
        behandlingId: UUID,
        tx: TransactionalSession? = null,
    ): Vedtak? =
        tx.session {
            hent(
                queryString = """
            SELECT sakid, behandlingId, saksbehandlerId, beregningsresultat, avkorting, vilkaarsresultat, id, fnr, 
                datoFattet, datoattestert, attestant, datoVirkFom, vedtakstatus, saktype, behandlingtype, 
                attestertVedtakEnhet, fattetVedtakEnhet, type, revurderingsaarsak, revurderinginfo, opphoer_fom,
                tilbakekreving, klage 
            FROM vedtak 
            WHERE behandlingId = :behandlingId
            """,
                params = mapOf("behandlingId" to behandlingId),
            ) {
                val utbetalingsperioder = hentUtbetalingsPerioder(it.long("id"), this)
                it.toVedtak(utbetalingsperioder)
            }
        }

    private fun hentVedtakNonNull(
        behandlingId: UUID,
        tx: TransactionalSession? = null,
    ): Vedtak = requireNotNull(hentVedtak(behandlingId, tx)) { "Fant ikke vedtak for behandling $behandlingId" }

    fun hentVedtakForSak(
        sakId: Long,
        tx: TransactionalSession? = null,
    ): List<Vedtak> {
        val hentVedtak = """
            SELECT sakid, behandlingId, saksbehandlerId, beregningsresultat, avkorting, vilkaarsresultat, id, fnr, 
                datoFattet, datoattestert, attestant, datoVirkFom, vedtakstatus, saktype, behandlingtype, 
                attestertVedtakEnhet, fattetVedtakEnhet, type, revurderingsaarsak, revurderinginfo, opphoer_fom,
                tilbakekreving, klage 
            FROM vedtak  
            WHERE sakId = :sakId
            """
        return tx.session {
            hentListe(
                queryString = hentVedtak,
                params = { mapOf("sakId" to sakId) },
            ) {
                it.toVedtak(emptyList())
            }
        }
    }

    fun hentFerdigstilteVedtak(
        fnr: Folkeregisteridentifikator,
        sakType: SakType,
        tx: TransactionalSession? = null,
    ): List<Vedtak> {
        val hentVedtak = """
            SELECT sakid, behandlingId, saksbehandlerId, beregningsresultat, avkorting, vilkaarsresultat, id, fnr, 
                datoFattet, datoattestert, attestant, datoVirkFom, vedtakstatus, saktype, behandlingtype, 
                attestertVedtakEnhet, fattetVedtakEnhet, type, revurderingsaarsak, revurderinginfo, opphoer_fom
            FROM vedtak  
            WHERE fnr = :fnr 
            AND saktype = :saktype   
            AND vedtakstatus in ('TIL_SAMORDNING', 'SAMORDNET', 'IVERKSATT')   
            """
        return tx.session {
            hentListe(
                queryString = hentVedtak,
                params = {
                    mapOf(
                        "fnr" to fnr.value,
                        "saktype" to sakType.name,
                    )
                },
            ) {
                val utbetalingsperioder = hentUtbetalingsPerioder(it.long("id"), this)
                it.toVedtak(utbetalingsperioder)
            }
        }
    }

    private fun hentUtbetalingsPerioder(
        vedtakId: Long,
        tx: TransactionalSession? = null,
    ): List<Utbetalingsperiode> =
        tx.session {
            hentListe(
                queryString = "SELECT * FROM utbetalingsperiode WHERE vedtakid = :vedtakid",
                params = { mapOf("vedtakid" to vedtakId) },
            ) { it.toUtbetalingsperiode() }
        }

    fun fattVedtak(
        behandlingId: UUID,
        vedtakFattet: VedtakFattet,
        tx: TransactionalSession? = null,
    ): Vedtak =
        tx.session {
            oppdater(
                query = """
                UPDATE vedtak 
                SET saksbehandlerId = :saksbehandlerId, fattetVedtakEnhet = :saksbehandlerEnhet, datoFattet = now(), 
                    vedtakstatus = :vedtakstatus  
                WHERE behandlingId = :behandlingId
                """,
                params =
                    mapOf(
                        "saksbehandlerId" to vedtakFattet.ansvarligSaksbehandler,
                        "saksbehandlerEnhet" to vedtakFattet.ansvarligEnhet,
                        "vedtakstatus" to VedtakStatus.FATTET_VEDTAK.name,
                        "behandlingId" to behandlingId,
                    ),
                loggtekst = "Fatter vedtak for behandling $behandlingId",
            ).also { require(it == 1) }
                .let { hentVedtakNonNull(behandlingId, this) }
        }

    fun attesterVedtak(
        behandlingId: UUID,
        attestasjon: Attestasjon,
        tx: TransactionalSession? = null,
    ): Vedtak =
        tx.session {
            oppdater(
                query = """
                UPDATE vedtak 
                SET attestant = :attestant, attestertVedtakEnhet = :attestertVedtakEnhet, datoAttestert = now(), 
                    vedtakstatus = :vedtakstatus 
                WHERE behandlingId = :behandlingId
                """,
                params =
                    mapOf(
                        "attestant" to attestasjon.attestant,
                        "attestertVedtakEnhet" to attestasjon.attesterendeEnhet,
                        "vedtakstatus" to VedtakStatus.ATTESTERT.name,
                        "behandlingId" to behandlingId,
                    ),
                loggtekst = "Attesterer vedtak $behandlingId",
            ).also {
                require(it == 1)
            }

            opprett(
                query = """
                    INSERT INTO outbox_vedtakshendelse (vedtakId, type) 
                    SELECT v.id, 'ATTESTERT'
                    FROM vedtak v
                    WHERE v.behandlingId = :behandlingId
                    """,
                params = mapOf("behandlingId" to behandlingId),
                loggtekst = "Lagt til innslag for attestert vedtak i outbox",
            )

            return@session hentVedtakNonNull(behandlingId, this)
        }

    fun underkjennVedtak(
        behandlingId: UUID,
        tx: TransactionalSession? = null,
    ): Vedtak =
        tx.session {
            oppdater(
                """
            UPDATE vedtak 
            SET attestant = null, datoAttestert = null, attestertVedtakEnhet = null, saksbehandlerId = null, 
                datoFattet = null, fattetVedtakEnhet = null, vedtakstatus = :vedtakstatus 
            WHERE behandlingId = :behandlingId
            """,
                params = mapOf("vedtakstatus" to VedtakStatus.RETURNERT.name, "behandlingId" to behandlingId),
                loggtekst = "Underkjenner vedtak for behandling $behandlingId",
            ).also { require(it == 1) }
            return@session hentVedtakNonNull(behandlingId, this)
        }

    fun tilSamordningVedtak(
        behandlingId: UUID,
        tx: TransactionalSession? = null,
    ): Vedtak =
        tx.session {
            oppdater(
                query = "UPDATE vedtak SET vedtakstatus = :vedtakstatus WHERE behandlingId = :behandlingId",
                params = mapOf("vedtakstatus" to VedtakStatus.TIL_SAMORDNING.name, "behandlingId" to behandlingId),
                loggtekst = "Lagrer til_samordning vedtak",
            ).also { require(it == 1) }
            return@session hentVedtakNonNull(behandlingId, this)
        }

    fun samordnetVedtak(
        behandlingId: UUID,
        tx: TransactionalSession? = null,
    ): Vedtak =
        tx.session {
            oppdater(
                query = "UPDATE vedtak SET vedtakstatus = :vedtakstatus WHERE behandlingId = :behandlingId",
                params = mapOf("vedtakstatus" to VedtakStatus.SAMORDNET.name, "behandlingId" to behandlingId),
                loggtekst = "Lagrer samordnet vedtak",
            ).also { require(it == 1) }
            return@session hentVedtakNonNull(behandlingId, this)
        }

    fun iverksattVedtak(
        behandlingId: UUID,
        tx: TransactionalSession? = null,
    ): Vedtak =
        tx.session {
            oppdater(
                query = "UPDATE vedtak SET vedtakstatus = :vedtakstatus WHERE behandlingId = :behandlingId",
                params = mapOf("vedtakstatus" to VedtakStatus.IVERKSATT.name, "behandlingId" to behandlingId),
                loggtekst = "Lagrer iverksatt vedtak",
            ).also { require(it == 1) }
            return@session hentVedtakNonNull(behandlingId, this)
        }

    private fun Row.toVedtak(utbetalingsperioder: List<Utbetalingsperiode>) =
        Vedtak(
            id = long("id"),
            sakId = long("sakid"),
            sakType = SakType.valueOf(string("saktype")),
            behandlingId = uuid("behandlingid"),
            soeker = string("fnr").let { Folkeregisteridentifikator.of(it) },
            status = string("vedtakstatus").let { VedtakStatus.valueOf(it) },
            type = string("type").let { VedtakType.valueOf(it) },
            vedtakFattet =
                stringOrNull("saksbehandlerid")?.let {
                    VedtakFattet(
                        ansvarligSaksbehandler = string("saksbehandlerid"),
                        ansvarligEnhet = string("fattetVedtakEnhet"),
                        tidspunkt = sqlTimestamp("datofattet").toTidspunkt(),
                    )
                },
            attestasjon =
                stringOrNull("attestant")?.let {
                    Attestasjon(
                        attestant = string("attestant"),
                        attesterendeEnhet = string("attestertVedtakEnhet"),
                        tidspunkt = sqlTimestamp("datoattestert").toTidspunkt(),
                    )
                },
            innhold =
                when (string("type").let { VedtakType.valueOf(it) }) {
                    VedtakType.OPPHOER,
                    VedtakType.AVSLAG,
                    VedtakType.ENDRING,
                    VedtakType.INNVILGELSE,
                    ->
                        VedtakInnhold.Behandling(
                            behandlingType = BehandlingType.valueOf(string("behandlingtype")),
                            virkningstidspunkt = sqlDate("datovirkfom").toLocalDate().let { YearMonth.from(it) },
                            vilkaarsvurdering = stringOrNull("vilkaarsresultat")?.let { objectMapper.readValue(it) },
                            beregning = stringOrNull("beregningsresultat")?.let { objectMapper.readValue(it) },
                            avkorting = stringOrNull("avkorting")?.let { objectMapper.readValue(it) },
                            utbetalingsperioder = utbetalingsperioder,
                            revurderingAarsak = stringOrNull("revurderingsaarsak")?.let { Revurderingaarsak.valueOf(it) },
                            revurderingInfo = stringOrNull("revurderinginfo")?.let { objectMapper.readValue(it) },
                            opphoerFraOgMed = sqlDateOrNull("opphoer_fom")?.toLocalDate()?.let { YearMonth.from(it) },
                        )

                    VedtakType.TILBAKEKREVING ->
                        VedtakInnhold.Tilbakekreving(
                            tilbakekreving = string("tilbakekreving").let { objectMapper.readValue(it) },
                        )

                    VedtakType.AVVIST_KLAGE ->
                        VedtakInnhold.Klage(
                            klage = string("klage").let { objectMapper.readValue(it) },
                        )
                },
        )

    private fun Row.toUtbetalingsperiode() =
        Utbetalingsperiode(
            id = long("id"),
            periode =
                Periode(
                    fom = YearMonth.from(localDate("datoFom")),
                    tom = localDateOrNull("datoTom")?.let(YearMonth::from),
                ),
            beloep = bigDecimalOrNull("beloep"),
            type = UtbetalingsperiodeType.valueOf(string("type")),
        )

    fun tilbakestillIkkeIverksatteVedtak(
        behandlingId: UUID,
        tx: TransactionalSession? = null,
    ): Vedtak? {
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
                params =
                    mapOf(
                        "vedtakstatus" to VedtakStatus.RETURNERT.name,
                        "behandlingId" to behandlingId,
                    ),
                loggtekst = "Returnerer vedtak $behandlingId",
            ).also { require(it == 1) }
            return@session hentVedtakNonNull(behandlingId, this)
        }
    }

    fun hentAlleLoependeVedtak(tx: TransactionalSession? = null) =
        tx.session {
            hentListe(
                """
                select behandlingId, saktype, sakid from vedtak 
                where vedtakstatus='${VedtakStatus.IVERKSATT.name}'
                and type!='${VedtakType.OPPHOER.name}'
                """.trimIndent(),
            ) { row ->
                val sakid = row.long("sakid")
                if (sakeneMedMismatchIAntallPerioder.contains(sakid)) {
                    BehandlingOgSaktype(
                        behandlingId = row.string("behandlingId").let { UUID.fromString(it) },
                        sakType = SakType.valueOf(row.string("saktype")),
                    )
                } else {
                    null
                }
            }.filterNotNull()
        }
}

data class BehandlingOgSaktype(
    val behandlingId: UUID,
    val sakType: SakType,
)
