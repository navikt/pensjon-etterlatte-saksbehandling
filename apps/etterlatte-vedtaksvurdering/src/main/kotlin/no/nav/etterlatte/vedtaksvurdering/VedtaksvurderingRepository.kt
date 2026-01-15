package no.nav.etterlatte.vedtaksvurdering

import com.fasterxml.jackson.module.kotlin.readValue
import kotliquery.Row
import kotliquery.TransactionalSession
import kotliquery.queryOf
import no.nav.etterlatte.libs.common.Enhetsnummer
import no.nav.etterlatte.libs.common.Regelverk
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.Revurderingaarsak
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.beregning.AvkortetYtelseDto
import no.nav.etterlatte.libs.common.beregning.AvkortingDto
import no.nav.etterlatte.libs.common.deserialize
import no.nav.etterlatte.libs.common.feilhaandtering.krev
import no.nav.etterlatte.libs.common.feilhaandtering.krevIkkeNull
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.toNorskTid
import no.nav.etterlatte.libs.common.tidspunkt.toTidspunkt
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.libs.common.vedtak.Attestasjon
import no.nav.etterlatte.libs.common.vedtak.AvkortetYtelsePeriode
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
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import java.sql.Date
import java.time.YearMonth
import java.util.UUID
import javax.sql.DataSource

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
                    is VedtakInnhold.Tilbakekreving -> {
                        mapOf("tilbakekreving" to opprettVedtak.innhold.tilbakekreving.toJson())
                    }

                    is VedtakInnhold.Klage -> {
                        mapOf("klage" to opprettVedtak.innhold.klage.toJson())
                    }

                    is VedtakInnhold.Behandling -> {
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
                    "sakid" to opprettVedtak.sakId.sakId,
                    "saktype" to opprettVedtak.sakType.name,
                    "fnr" to opprettVedtak.soeker.value,
                    "vedtakstatus" to opprettVedtak.status.name,
                    "type" to opprettVedtak.type.name,
                ) + innholdParams,
            ).let { query -> this.run(query.asUpdateAndReturnGeneratedKey) }
                ?.let { vedtakId ->
                    if (opprettVedtak.innhold is VedtakInnhold.Behandling) {
                        opprettUtbetalingsperioder(vedtakId, opprettVedtak.innhold.utbetalingsperioder, this)
                        opprettVedtak.innhold.avkorting?.let {
                            opprettAvkortetYtelsePerioder(
                                vedtakId,
                                deserialize<AvkortingDto>(it.toString()).avkortetYtelse,
                                this,
                            )
                        }
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
                    is VedtakInnhold.Behandling -> {
                        mapOf(
                            "datovirkfom" to oppdatertVedtak.innhold.virkningstidspunkt.atDay(1),
                            "beregningsresultat" to oppdatertVedtak.innhold.beregning?.toJson(),
                            "avkorting" to oppdatertVedtak.innhold.avkorting?.toJson(),
                            "vilkaarsresultat" to oppdatertVedtak.innhold.vilkaarsvurdering?.toJson(),
                            "revurderinginfo" to oppdatertVedtak.innhold.revurderingInfo?.toJson(),
                            "opphoer_fom" to oppdatertVedtak.innhold.opphoerFraOgMed?.atDay(1),
                        )
                    }

                    is VedtakInnhold.Tilbakekreving -> {
                        mapOf(
                            "tilbakekreving" to oppdatertVedtak.innhold.tilbakekreving.toJson(),
                        )
                    }

                    is VedtakInnhold.Klage -> {
                        mapOf(
                            "klage" to oppdatertVedtak.innhold.klage.toJson(),
                        )
                    }
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
                slettAvkortetYtelsePerioder(oppdatertVedtak.id, this)
                oppdatertVedtak.innhold.avkorting?.let {
                    opprettAvkortetYtelsePerioder(
                        oppdatertVedtak.id,
                        deserialize<AvkortingDto>(it.toString()).avkortetYtelse,
                        this,
                    )
                }
            }
            return@session hentVedtak(oppdatertVedtak.behandlingId, this)
                ?: throw Exception("Kunne ikke oppdatere vedtak for behandling ${oppdatertVedtak.behandlingId}")
        }

    fun hentVedtak(
        vedtakId: Long,
        tx: TransactionalSession? = null,
    ): Vedtak? =
        tx.session {
            hent(
                queryString = """
            SELECT sakid, behandlingId, saksbehandlerId, beregningsresultat, avkorting, vilkaarsresultat, id, fnr, 
                datoFattet, datoattestert, datoiverksatt, attestant, datoVirkFom, vedtakstatus, saktype, behandlingtype, 
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
                datoFattet, datoattestert, datoiverksatt, attestant, datoVirkFom, vedtakstatus, saktype, behandlingtype, 
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

    fun hentVedtakForSak(
        sakId: SakId,
        tx: TransactionalSession? = null,
    ): List<Vedtak> {
        val hentVedtak = """
            SELECT sakid, behandlingId, saksbehandlerId, beregningsresultat, avkorting, vilkaarsresultat, id, fnr, 
                datoFattet, datoattestert, datoiverksatt, attestant, datoVirkFom, vedtakstatus, saktype, behandlingtype, 
                attestertVedtakEnhet, fattetVedtakEnhet, type, revurderingsaarsak, revurderinginfo, opphoer_fom,
                tilbakekreving, klage 
            FROM vedtak  
            WHERE sakId = :sakId
            """
        return tx.session {
            hentListe(
                queryString = hentVedtak,
                params = { mapOf("sakId" to sakId.sakId) },
            ) {
                it.toVedtak(emptyList())
            }
        }
    }

    fun hentSakIdMedUtbetalingForInntektsaar(
        inntektsaar: Int,
        sakType: SakType? = null,
        tx: TransactionalSession? = null,
    ): List<SakId> {
        val hentVedtak =
            """
            SELECT DISTINCT v.sakid FROM vedtak v
            JOIN utbetalingsperiode u ON v.id = u.vedtakid
            WHERE EXTRACT(YEAR FROM u.datofom) = :aar
            ${if (sakType == null) "" else "AND saktype = :saktype"}
            AND vedtakstatus = :vedtakStatus
            """.trimIndent()

        return tx.session {
            hentListe(
                queryString = hentVedtak,
                params = {
                    mapOf(
                        "aar" to inntektsaar,
                        "saktype" to sakType?.name,
                        "vedtakStatus" to VedtakStatus.IVERKSATT.name,
                    )
                },
            ) {
                it.toSakId()
            }
        }
    }

    fun hentFerdigstilteVedtak(
        fnr: Folkeregisteridentifikator,
        sakType: SakType? = null,
        tx: TransactionalSession? = null,
    ): List<Vedtak> {
        val hentVedtak = """
            SELECT sakid, behandlingId, saksbehandlerId, beregningsresultat, avkorting, vilkaarsresultat, id, fnr, 
                   datoFattet, datoattestert, datoiverksatt, attestant, datoVirkFom, vedtakstatus, saktype, behandlingtype, 
                   attestertVedtakEnhet, fattetVedtakEnhet, type, revurderingsaarsak, revurderinginfo, opphoer_fom
            FROM vedtak  
            WHERE fnr = :fnr 
              AND (
                   vedtakstatus IN ('TIL_SAMORDNING', 'SAMORDNET', 'IVERKSATT', 'AVSLAG') 
                   OR (type = 'AVSLAG' AND vedtakstatus = 'ATTESTERT')
              )
            ${if (sakType == null) "" else "AND saktype = :saktype"}
            """
        return tx.session {
            hentListe(
                queryString = hentVedtak,
                params = {
                    mapOf(
                        "fnr" to fnr.value,
                        "saktype" to sakType?.name,
                    )
                },
            ) {
                val utbetalingsperioder = hentUtbetalingsPerioder(it.long("id"), this)
                it.toVedtak(utbetalingsperioder)
            }
        }
    }

    fun hentAvkortetYtelsePerioder(
        vedtakIds: Set<Long>,
        tx: TransactionalSession? = null,
    ): List<AvkortetYtelsePeriode> =
        tx.session {
            val idArray = this.connection.underlying.createArrayOf("bigint", vedtakIds.toTypedArray())
            hentListe(
                queryString = "SELECT * FROM avkortet_ytelse_periode WHERE vedtakid = ANY (:vedtakIds)",
                params = { mapOf("vedtakIds" to idArray) },
            ) { it.toAvkortetYtelsePeriode() }
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
                        "saksbehandlerEnhet" to vedtakFattet.ansvarligEnhet.enhetNr,
                        "vedtakstatus" to VedtakStatus.FATTET_VEDTAK.name,
                        "behandlingId" to behandlingId,
                    ),
                loggtekst = "Fatter vedtak for behandling $behandlingId",
            ).also { krev(it == 1) { "Vedtak ble ikke oppdatert etter fatting behandlingid: $behandlingId" } }
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
                        "attestertVedtakEnhet" to attestasjon.attesterendeEnhet.enhetNr,
                        "vedtakstatus" to VedtakStatus.ATTESTERT.name,
                        "behandlingId" to behandlingId,
                    ),
                loggtekst = "Attesterer vedtak $behandlingId",
            ).also {
                krev(it == 1) { "Vedtak ble ikke oppdatert etter attestering behandlingid: $behandlingId" }
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
            ).also {
                krev(it == 1) { "Vedtak ble ikke oppdatert etter underkjenning behandlingid: $behandlingId" }
            }
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
            ).also {
                krev(it == 1) { "Vedtak ble ikke oppdatert etter satt til samordning behandlingid: $behandlingId" }
            }
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
            ).also {
                krev(it == 1) { "Vedtak ble ikke oppdatert etter samordnet behandlingid: $behandlingId" }
            }
            return@session hentVedtakNonNull(behandlingId, this)
        }

    fun iverksattVedtak(
        behandlingId: UUID,
        tx: TransactionalSession? = null,
    ): Vedtak =
        tx.session {
            oppdater(
                query = "UPDATE vedtak SET vedtakstatus = :vedtakstatus, datoiverksatt = :datoiverksatt WHERE behandlingId = :behandlingId",
                params =
                    mapOf(
                        "vedtakstatus" to VedtakStatus.IVERKSATT.name,
                        "behandlingId" to behandlingId,
                        "datoiverksatt" to Tidspunkt.now().toNorskTid(),
                    ),
                loggtekst = "Lagrer iverksatt vedtak",
            ).also {
                krev(it == 1) { "Vedtak ble ikke oppdatert etter iverksatt behandlingid: $behandlingId" }
            }
            return@session hentVedtakNonNull(behandlingId, this)
        }

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
            ).also {
                krev(it == 1) { "Vedtak ble ikke oppdatert returnert/tilbakestilt behandlingid: $behandlingId" }
            }
            return@session hentVedtakNonNull(behandlingId, this)
        }
    }

    fun lagreManuellBehandlingSamordningsmelding(
        oppdatering: OppdaterSamordningsmelding,
        brukerTokenInfo: BrukerTokenInfo,
        tx: TransactionalSession? = null,
    ) = tx.session {
        opprett(
            query = """
                    INSERT INTO samordning_manuell (opprettet_av, vedtakId, samId, refusjonskrav, kommentar) 
                    VALUES (:opprettetAv, :vedtakId, :samId, :refusjonskrav, :kommentar)
                    """,
            params =
                mapOf(
                    "opprettetAv" to brukerTokenInfo.ident(),
                    "vedtakId" to oppdatering.vedtakId,
                    "samId" to oppdatering.samId,
                    "refusjonskrav" to oppdatering.refusjonskrav,
                    "kommentar" to oppdatering.kommentar,
                ),
            loggtekst = "Lagt til innslag for manuell behandling av samordningsmelding",
        )
    }

    fun slettManuellBehandlingSamordningsmelding(
        samId: Long,
        tx: TransactionalSession? = null,
    ) = tx.session {
        oppdater(
            query = "DELETE FROM samordning_manuell WHERE samId = :samId",
            params = mapOf("samId" to samId),
            loggtekst = "Sletter innslag for manuell samordning pga feil ved ekstern oppdatering",
        )
    }

    fun tilbakestillTilbakekrevingsvedtak(
        tilbakekrevingId: UUID,
        tx: TransactionalSession? = null,
    ) = tx.session {
        oppdater(
            query =
                """
                UPDATE vedtak 
                SET vedtakstatus = 'FATTET_VEDTAK', attestertvedtakenhet = null, attestant = null, datoattestert = null 
                WHERE behandlingId = :behandlingId AND type = 'TILBAKEKREVING' AND vedtakstatus = 'ATTESTERT'
                """.trimIndent(),
            params = mapOf("behandlingId" to tilbakekrevingId),
            loggtekst = "Tilbakestiller tilbakerkreving fra attestert siden den feilet mot tilbakekrevingskomponenten",
        )
    }

    private fun hentVedtakNonNull(
        behandlingId: UUID,
        tx: TransactionalSession? = null,
    ): Vedtak = krevIkkeNull(hentVedtak(behandlingId, tx)) { "Fant ikke vedtak for behandling $behandlingId" }

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
            regelverk = string("regelverk").let { Regelverk.valueOf(it) },
        )

    private fun Row.toAvkortetYtelsePeriode() =
        AvkortetYtelsePeriode(
            id = uuid("id"),
            vedtakId = long("vedtakid"),
            fom = YearMonth.from(localDate("datofom")),
            tom = localDateOrNull("datotom")?.let(YearMonth::from),
            type = string("type"),
            ytelseFoerAvkorting = int("ytelseFoer"),
            ytelseEtterAvkorting = int("ytelseEtter"),
        )

    private fun Row.toSakId() = SakId(long("sakid"))

    private fun Row.toVedtak(utbetalingsperioder: List<Utbetalingsperiode>) =
        Vedtak(
            id = long("id"),
            sakId = SakId(long("sakid")),
            sakType = SakType.valueOf(string("saktype")),
            behandlingId = uuid("behandlingid"),
            soeker = string("fnr").let { Folkeregisteridentifikator.of(it) },
            status = string("vedtakstatus").let { VedtakStatus.valueOf(it) },
            type = string("type").let { VedtakType.valueOf(it) },
            iverksettelsesTidspunkt = sqlTimestampOrNull("datoiverksatt")?.toTidspunkt(),
            vedtakFattet =
                stringOrNull("saksbehandlerid")?.let {
                    VedtakFattet(
                        ansvarligSaksbehandler = string("saksbehandlerid"),
                        ansvarligEnhet = Enhetsnummer(string("fattetVedtakEnhet")),
                        tidspunkt = sqlTimestamp("datofattet").toTidspunkt(),
                    )
                },
            attestasjon =
                stringOrNull("attestant")?.let {
                    Attestasjon(
                        attestant = string("attestant"),
                        attesterendeEnhet = Enhetsnummer(string("attestertVedtakEnhet")),
                        tidspunkt = sqlTimestamp("datoattestert").toTidspunkt(),
                    )
                },
            innhold =
                when (string("type").let { VedtakType.valueOf(it) }) {
                    VedtakType.OPPHOER,
                    VedtakType.AVSLAG,
                    VedtakType.ENDRING,
                    VedtakType.INNVILGELSE,
                    VedtakType.INGEN_ENDRING,
                    -> {
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
                    }

                    VedtakType.TILBAKEKREVING -> {
                        VedtakInnhold.Tilbakekreving(
                            tilbakekreving = string("tilbakekreving").let { objectMapper.readValue(it) },
                        )
                    }

                    VedtakType.AVVIST_KLAGE -> {
                        VedtakInnhold.Klage(
                            klage = string("klage").let { objectMapper.readValue(it) },
                        )
                    }
                },
        )

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

    private fun slettAvkortetYtelsePerioder(
        vedtakId: Long,
        tx: TransactionalSession,
    ) = queryOf(
        statement = """
                DELETE FROM avkortet_ytelse_periode
                WHERE vedtakid = :vedtakid
                """,
        paramMap =
            mapOf(
                "vedtakid" to vedtakId,
            ),
    ).let { query -> tx.run(query.asUpdate) }

    private fun opprettAvkortetYtelsePerioder(
        vedtakId: Long,
        avkortetYtelse: List<AvkortetYtelseDto>,
        tx: TransactionalSession,
    ) = tx.batchPreparedNamedStatement(
        statement = """
                    INSERT INTO avkortet_ytelse_periode(vedtakid, datofom, datotom, type, ytelseFoer, ytelseEtter) 
                    VALUES (:vedtakid, :datofom, :datotom, :type, :ytelseFoer, :ytelseEtter)
                    """,
        params =
            avkortetYtelse.map {
                mapOf(
                    "vedtakid" to vedtakId,
                    "datofom" to it.fom.atDay(1).let(Date::valueOf),
                    "datotom" to it.tom?.atEndOfMonth()?.let(Date::valueOf),
                    "type" to it.type,
                    "ytelseFoer" to it.ytelseFoerAvkorting,
                    "ytelseEtter" to it.ytelseEtterAvkorting,
                )
            },
    )

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
                    INSERT INTO utbetalingsperiode(vedtakid, datofom, datotom, type, beloep, regelverk) 
                    VALUES (:vedtakid, :datofom, :datotom, :type, :beloep, :regelverk)
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
                    "regelverk" to it.regelverk.name,
                ),
        ).let { query -> tx.run(query.asUpdate) }
    }
}
