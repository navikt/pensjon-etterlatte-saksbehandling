package no.nav.etterlatte.avkorting

import com.fasterxml.jackson.module.kotlin.readValue
import kotliquery.Row
import kotliquery.TransactionalSession
import kotliquery.queryOf
import no.nav.etterlatte.libs.common.beregning.InntektsjusteringAvkortingInfoRequest
import no.nav.etterlatte.libs.common.beregning.SanksjonertYtelse
import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.periode.Periode
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.tidspunkt.toTidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.toTimestamp
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.libs.database.transaction
import java.time.YearMonth
import java.util.UUID
import javax.sql.DataSource

class AvkortingRepository(
    private val dataSource: DataSource,
) {
    fun harSakInntektForAar(harInntektForAarDto: InntektsjusteringAvkortingInfoRequest): Boolean =
        dataSource.transaction { tx ->
            val alleAarsoppgjoer =
                queryOf(
                    "SELECT * FROM avkorting_aarsoppgjoer WHERE behandling_id = ? AND aar = ?",
                    harInntektForAarDto.sisteBehandling,
                    harInntektForAarDto.aar,
                ).let { query ->
                    tx.run(
                        query.map { row -> row.uuid("id") }.asList,
                    )
                }

            alleAarsoppgjoer.isNotEmpty()
        }

    fun hentAvkorting(behandlingId: UUID): Avkorting? =
        dataSource.transaction { tx ->
            val alleAarsoppgjoer =
                queryOf(
                    "SELECT * FROM avkorting_aarsoppgjoer WHERE behandling_id = ? ORDER BY aar ASC",
                    behandlingId,
                ).let { query ->
                    tx.run(
                        query
                            .map { row ->
                                val aarsoppgjoerId = row.uuid("id")
                                val erEtteroppgjoer = row.boolean("er_etteroppgjoer")

                                val ytelseFoerAvkorting = selectYtelseFoerAvkorting(aarsoppgjoerId, tx)

                                if (erEtteroppgjoer) {
                                    val faktiskInntekt =
                                        selectFaktiskInntekt(aarsoppgjoerId, tx)
                                            ?: throw InternfeilException("Etteroppgjør mangler faktisk inntekt")

                                    val avkortingsperioder = selectAvkortingsPerioder(faktiskInntekt.id, tx)
                                    val sanksjonerAvkortetYtelseAar = selectSanksjonerAvkortetYtelse(aarsoppgjoerId, tx)

                                    val avkortetYtelse =
                                        selectAvkortetYtelse(
                                            aarsoppgjoerId,
                                            AvkortetYtelseType.ETTEROPPGJOER,
                                            emptyList(),
                                            sanksjonerAvkortetYtelseAar,
                                            tx,
                                        )

                                    Etteroppgjoer(
                                        id = aarsoppgjoerId,
                                        aar = row.int("aar"),
                                        fom = row.sqlDate("fom").let { YearMonth.from(it.toLocalDate()) },
                                        ytelseFoerAvkorting = ytelseFoerAvkorting,
                                        inntekt = faktiskInntekt,
                                        avkortingsperioder = avkortingsperioder,
                                        avkortetYtelse = avkortetYtelse,
                                    )
                                } else {
                                    val forventetInntekt = selectForventetInntekt(aarsoppgjoerId, tx)

                                    val restanse = selectRestanse(aarsoppgjoerId, tx)

                                    val inntektsavkorting =
                                        forventetInntekt.map {
                                            val avkortingsperioder = selectAvkortingsPerioder(it.id, tx)
                                            val sanksjoner = selectSanksjonerAvkortetYtelse(it.id, tx)
                                            val avkortetYtelse = selectAvkortetYtelseForForventetInntekt(it.id, restanse, sanksjoner, tx)
                                            Inntektsavkorting(
                                                grunnlag = it,
                                                avkortingsperioder = avkortingsperioder,
                                                avkortetYtelseForventetInntekt = avkortetYtelse,
                                            )
                                        }
                                    val sanksjonerAvkortetYtelse = selectSanksjonerAvkortetYtelse(aarsoppgjoerId, tx)
                                    val avkortetYtelse =
                                        selectAvkortetYtelse(
                                            aarsoppgjoerId,
                                            AvkortetYtelseType.AARSOPPGJOER,
                                            restanse,
                                            sanksjonerAvkortetYtelse,
                                            tx,
                                        )

                                    AarsoppgjoerLoepende(
                                        id = aarsoppgjoerId,
                                        aar = row.int("aar"),
                                        fom = row.sqlDate("fom").let { YearMonth.from(it.toLocalDate()) },
                                        ytelseFoerAvkorting = ytelseFoerAvkorting,
                                        inntektsavkorting = inntektsavkorting,
                                        avkortetYtelse = avkortetYtelse,
                                    )
                                }
                            }.asList,
                    )
                }

            if (alleAarsoppgjoer.isEmpty()) {
                null
            } else {
                Avkorting(aarsoppgjoer = alleAarsoppgjoer)
            }
        }

    private fun selectYtelseFoerAvkorting(
        aarsoppgjoerId: UUID,
        tx: TransactionalSession,
    ) = queryOf(
        "SELECT * FROM avkorting_aarsoppgjoer_ytelse_foer_avkorting WHERE aarsoppgjoer_id = ? ORDER BY fom ASC",
        aarsoppgjoerId,
    ).let { query -> tx.run(query.map { row -> row.toYtelseFoerAvkorting() }.asList) }

    private fun selectForventetInntekt(
        aarsoppgjoerId: UUID,
        tx: TransactionalSession,
    ) = queryOf(
        "SELECT * FROM avkortingsgrunnlag_forventet WHERE aarsoppgjoer_id = ? ORDER BY fom ASC",
        aarsoppgjoerId,
    ).let { query ->
        tx.run(
            query
                .map { row ->
                    val avkortingGrunnlagId = row.uuid("id")
                    val inntektInnvilgetPeriode =
                        selectInntektInnvilgetPeriode(avkortingGrunnlagId, tx)
                            ?: IngenInntektInnvilgetPeriode

                    row.toForventetInntekt(avkortingGrunnlagId, inntektInnvilgetPeriode)
                }.asList,
        )
    }

    fun hentFaktiskInntekt(aarsoppgjoerId: UUID) = dataSource.transaction { tx -> selectFaktiskInntekt(aarsoppgjoerId, tx) }

    private fun selectFaktiskInntekt(
        aarsoppgjoerId: UUID,
        tx: TransactionalSession,
    ) = queryOf(
        "SELECT * FROM avkortingsgrunnlag_faktisk WHERE aarsoppgjoer_id = ? ORDER BY fom ASC",
        aarsoppgjoerId,
    ).let { query ->
        tx.run(
            query
                .map { row ->
                    val avkortingGrunnlagId = row.uuid("id")
                    val inntektInnvilgetPeriode =
                        selectInntektInnvilgetPeriode(avkortingGrunnlagId, tx)
                            ?: throw InternfeilException("Grunnlag for etteroppgjør mangler inntekt innvilgede måneder")

                    row.toFaktiskInntekt(avkortingGrunnlagId, inntektInnvilgetPeriode)
                }.asSingle,
        )
    }

    private fun selectInntektInnvilgetPeriode(
        avkortingGrunnlagId: UUID,
        tx: TransactionalSession,
    ) = queryOf(
        "SELECT * FROM inntekt_innvilget WHERE grunnlag_id = ?",
        avkortingGrunnlagId,
    ).let { query ->
        tx.run(query.map { row -> row.toInntektInnvilgetPeriode() }.asSingle)
    }

    private fun selectRestanse(
        aarsoppgjoerId: UUID,
        tx: TransactionalSession,
    ) = queryOf(
        "SELECT * FROM avkorting_aarsoppgjoer_restanse WHERE aarsoppgjoer_id = ?",
        aarsoppgjoerId,
    ).let { query -> tx.run(query.map { row -> row.toRestanse() }.asList) }

    private fun selectAvkortingsPerioder(
        inntektId: UUID,
        tx: TransactionalSession,
    ) = queryOf(
        "SELECT * FROM avkortingsperioder WHERE inntektsgrunnlag = ? ORDER BY fom ASC",
        inntektId,
    ).let { query -> tx.run(query.map { row -> row.toAvkortingsperiode() }.asList) }

    // TODO sanksjon skal ut av avkorting
    private fun selectSanksjonerAvkortetYtelse(
        aarsoppgjoerId: UUID,
        tx: TransactionalSession,
    ) = queryOf(
        """
        SELECT y.sanksjon_id, s.sanksjon_type 
        FROM avkortet_ytelse y 
        INNER JOIN sanksjon s 
        ON y.sanksjon_id = s.id 
        WHERE y.aarsoppgjoer_id = ? 
        ORDER BY y.fom ASC
        """.trimIndent(),
        aarsoppgjoerId,
    ).let { query -> tx.run(query.map { row -> row.toSanksjonerYtelse() }.asList) }

    private fun selectAvkortetYtelse(
        aarsoppgjoerId: UUID,
        type: AvkortetYtelseType,
        restanse: List<Restanse>,
        sanksjoner: List<SanksjonertYtelse>,
        tx: TransactionalSession,
    ) = queryOf(
        "SELECT * FROM avkortet_ytelse WHERE aarsoppgjoer_id = ? AND type = ? ORDER BY fom ASC",
        aarsoppgjoerId,
        type.name,
    ).let { query ->
        tx.run(
            query
                .map { row ->
                    row.toAvkortetYtelse(
                        restanse,
                        sanksjoner,
                    )
                }.asList,
        )
    }

    private fun selectAvkortetYtelseForForventetInntekt(
        inntektId: UUID,
        restanse: List<Restanse>,
        sanksjoner: List<SanksjonertYtelse>,
        tx: TransactionalSession,
    ) = queryOf(
        "SELECT * FROM avkortet_ytelse WHERE inntektsgrunnlag = ? AND type = ? ORDER BY fom ASC",
        inntektId,
        AvkortetYtelseType.FORVENTET_INNTEKT.name,
    ).let { query ->
        tx.run(
            query
                .map { row ->
                    row.toAvkortetYtelse(
                        restanse,
                        sanksjoner,
                    )
                }.asList,
        )
    }

    fun lagreAvkorting(
        behandlingId: UUID,
        sakId: SakId,
        avkorting: Avkorting,
    ) {
        dataSource.transaction { tx ->
            slettAvkorting(behandlingId, tx)
            avkorting.aarsoppgjoer.forEach { aarsoppgjoer ->
                with(aarsoppgjoer) {
                    when (this) {
                        is AarsoppgjoerLoepende -> {
                            lagreAarsoppgjoer(behandlingId, sakId, this, false, tx)
                            lagreYtelseFoerAvkorting(behandlingId, aarsoppgjoer.id, ytelseFoerAvkorting, tx)
                            inntektsavkorting.forEach {
                                (it.grunnlag.inntektInnvilgetPeriode as? BenyttetInntektInnvilgetPeriode)?.let { inntektInnvilgetPeriode ->
                                    lagreInntektInnvilgetPeriode(behandlingId, it.grunnlag.id, inntektInnvilgetPeriode, tx)
                                }
                                lagreAvkortingGrunnlagForventet(behandlingId, aarsoppgjoer.id, it.grunnlag, tx)
                                lagreAvkortingsperioder(behandlingId, aarsoppgjoer.id, it.avkortingsperioder, tx)
                                lagreAvkortetYtelse(behandlingId, aarsoppgjoer.id, it.avkortetYtelseForventetInntekt, tx)
                            }
                            lagreAvkortetYtelse(behandlingId, aarsoppgjoer.id, avkortetYtelse, tx)
                        }

                        is Etteroppgjoer -> {
                            lagreAarsoppgjoer(behandlingId, sakId, this, true, tx)
                            lagreYtelseFoerAvkorting(behandlingId, aarsoppgjoer.id, ytelseFoerAvkorting, tx)
                            lagreInntektInnvilgetPeriode(behandlingId, inntekt.id, inntekt.inntektInnvilgetPeriode, tx)
                            lagreAvkortingGrunnlagFaktisk(behandlingId, aarsoppgjoer.id, inntekt, tx)
                            lagreAvkortingsperioder(behandlingId, aarsoppgjoer.id, avkortingsperioder, tx)
                            lagreAvkortetYtelse(behandlingId, aarsoppgjoer.id, avkortetYtelse, tx)
                        }
                    }
                }
            }
        }
    }

    fun slettForBehandling(behandling: UUID) {
        dataSource.transaction { tx ->
            slettAvkorting(behandling, tx)
        }
    }

    private fun slettAvkorting(
        behandlingId: UUID,
        tx: TransactionalSession,
    ) {
        queryOf(
            "DELETE FROM avkorting_aarsoppgjoer WHERE behandling_id  = ?",
            behandlingId,
        ).let { query ->
            tx.run(query.asUpdate)
        }
        queryOf(
            "DELETE FROM avkorting_aarsoppgjoer_ytelse_foer_avkorting WHERE behandling_id  = ?",
            behandlingId,
        ).let { query ->
            tx.run(query.asUpdate)
        }
        queryOf(
            "DELETE FROM avkortingsperioder WHERE behandling_id = ?",
            behandlingId,
        ).let { query ->
            tx.run(query.asUpdate)
        }
        queryOf(
            "DELETE FROM avkorting_aarsoppgjoer_restanse WHERE behandling_id  = ?",
            behandlingId,
        ).let { query ->
            tx.run(query.asUpdate)
        }
        queryOf(
            "DELETE FROM avkortet_ytelse WHERE behandling_id  = ?",
            behandlingId,
        ).let { query ->
            tx.run(query.asUpdate)
        }
        queryOf(
            "DELETE FROM avkortingsgrunnlag_forventet WHERE behandling_id = ?",
            behandlingId,
        ).let { query ->
            tx.run(query.asUpdate)
        }
        queryOf(
            "DELETE FROM avkortingsgrunnlag_faktisk WHERE behandling_id = ?",
            behandlingId,
        ).let { query ->
            tx.run(query.asUpdate)
        }
        queryOf(
            "DELETE FROM inntekt_innvilget WHERE behandling_id = ?",
            behandlingId,
        ).let { query ->
            tx.run(query.asUpdate)
        }
    }

    private fun lagreAarsoppgjoer(
        behandlingId: UUID,
        sakId: SakId,
        aarsoppgjoer: Aarsoppgjoer,
        erEtteroppgjoer: Boolean,
        tx: TransactionalSession,
    ) = queryOf(
        statement =
            """
            INSERT INTO avkorting_aarsoppgjoer(
            	id, behandling_id, sak_id, aar, fom, er_etteroppgjoer
            ) VALUES (
            	:id, :behandling_id, :sak_id, :aar, :fom, :erEtteroppgjoer
            )
            """.trimIndent(),
        paramMap =
            mapOf(
                "id" to aarsoppgjoer.id,
                "behandling_id" to behandlingId,
                "sak_id" to sakId.sakId,
                "aar" to aarsoppgjoer.aar,
                "fom" to aarsoppgjoer.fom.atDay(1),
                "erEtteroppgjoer" to erEtteroppgjoer,
            ),
    ).let { query -> tx.run(query.asUpdate) }

    private fun lagreAvkortingGrunnlagForventet(
        behandlingId: UUID,
        aarsoppgjoerId: UUID,
        avkortingsgrunnlag: ForventetInntekt,
        tx: TransactionalSession,
    ) = queryOf(
        statement =
            """
            INSERT INTO avkortingsgrunnlag_forventet(
                id, behandling_id, fom, tom, inntekt_tom, fratrekk_inn_ut, inntekt_utland_tom, fratrekk_inn_aar_utland,
                spesifikasjon, kilde, aarsoppgjoer_id, relevante_maaneder,
                overstyrt_innvilga_maaneder_aarsak, overstyrt_innvilga_maaneder_begrunnelse, maaneder_innvilget,
                maaneder_innvilget_regel_resultat
            ) VALUES (
                :id, :behandlingId, :fom, :tom, :inntektTom, :fratrekkInnAar, :inntektUtlandTom, :fratrekkInnAarUtland,
                :spesifikasjon, :kilde, :aarsoppgjoerId, :relevanteMaaneder,
                :overstyrtInnvilgaMaanederAarsak, :overstyrtInnvilgaMaanederBegrunnelse, :maanederInnvilget::jsonb,
                :maanederInnvilgetRegelResultat
            )
            """.trimIndent(),
        paramMap =
            mapOf(
                "id" to avkortingsgrunnlag.id,
                "behandlingId" to behandlingId,
                "aarsoppgjoerId" to aarsoppgjoerId,
                "fom" to avkortingsgrunnlag.periode.fom.atDay(1),
                "tom" to avkortingsgrunnlag.periode.tom?.atEndOfMonth(),
                "inntektTom" to avkortingsgrunnlag.inntektTom,
                "fratrekkInnAar" to avkortingsgrunnlag.fratrekkInnAar,
                "inntektUtlandTom" to avkortingsgrunnlag.inntektUtlandTom,
                "fratrekkInnAarUtland" to avkortingsgrunnlag.fratrekkInnAarUtland,
                "relevanteMaaneder" to avkortingsgrunnlag.innvilgaMaaneder,
                "spesifikasjon" to avkortingsgrunnlag.spesifikasjon,
                "kilde" to avkortingsgrunnlag.kilde.toJson(),
                "overstyrtInnvilgaMaanederAarsak" to avkortingsgrunnlag.overstyrtInnvilgaMaanederAarsak?.name,
                "overstyrtInnvilgaMaanederBegrunnelse" to avkortingsgrunnlag.overstyrtInnvilgaMaanederBegrunnelse,
                "maanederInnvilget" to avkortingsgrunnlag.maanederInnvilget?.toJson(),
                "maanederInnvilgetRegelResultat" to avkortingsgrunnlag.maanederInnvilgetRegelResultat,
            ),
    ).let { query -> tx.run(query.asUpdate) }

    private fun lagreAvkortingGrunnlagFaktisk(
        behandlingId: UUID,
        aarsoppgjoerId: UUID,
        faktisk: FaktiskInntekt,
        tx: TransactionalSession,
    ) = queryOf(
        statement =
            """
            INSERT INTO avkortingsgrunnlag_faktisk(
                id, behandling_id, aarsoppgjoer_id, fom, tom, innvilgede_maaneder, loennsinntekt, naeringsinntekt, afp, utlandsinntekt, spesifikasjon, kilde, maaneder_innvilget, maaneder_innvilget_regel_resultat
            ) VALUES (
                :id, :behandlingId, :aarsoppgjoerId, :fom, :tom, :innvilgedeMaaneder, :loennsinntekt, :naeringsinntekt, :afp, :utlandsinntekt, :spesifikasjon, :kilde, :maanederInnvilget::jsonb, :maanederInnvilgetRegelResultat
            )
            """.trimIndent(),
        paramMap =
            mapOf(
                "id" to faktisk.id,
                "behandlingId" to behandlingId,
                "aarsoppgjoerId" to aarsoppgjoerId,
                "fom" to faktisk.periode.fom.atDay(1),
                "tom" to faktisk.periode.tom?.atEndOfMonth(),
                "innvilgedeMaaneder" to faktisk.innvilgaMaaneder,
                "loennsinntekt" to faktisk.loennsinntekt,
                "naeringsinntekt" to faktisk.naeringsinntekt,
                "afp" to faktisk.afp,
                "utlandsinntekt" to faktisk.utlandsinntekt,
                "spesifikasjon" to faktisk.spesifikasjon,
                "kilde" to faktisk.kilde.toJson(),
                "maanederInnvilget" to faktisk.maanederInnvilget?.toJson(),
                "maanederInnvilgetRegelResultat" to faktisk.maanederInnvilgetRegelResultat,
            ),
    ).let { query -> tx.run(query.asUpdate) }

    private fun lagreInntektInnvilgetPeriode(
        behandlingId: UUID,
        avkortingsgrunnlagId: UUID,
        inntektInnvilgetPeriode: BenyttetInntektInnvilgetPeriode,
        tx: TransactionalSession,
    ) {
        queryOf(
            statement =
                """
                INSERT INTO inntekt_innvilget(
                    grunnlag_id, behandling_id, inntekt, regel_resultat, kilde, tidspunkt
                ) VALUES (
                    :grunnlagId, :behandlingId, :inntekt, :regelResultat, :kilde, :tidspunkt
                )
                """.trimIndent(),
            paramMap =
                mapOf(
                    "grunnlagId" to avkortingsgrunnlagId,
                    "behandlingId" to behandlingId,
                    "inntekt" to inntektInnvilgetPeriode.verdi,
                    "regelResultat" to inntektInnvilgetPeriode.regelResultat.toJson(),
                    "kilde" to inntektInnvilgetPeriode.kilde.toJson(),
                    "tidspunkt" to inntektInnvilgetPeriode.tidspunkt.toTimestamp(),
                ),
        ).let { query -> tx.run(query.asUpdate) }
    }

    private fun lagreYtelseFoerAvkorting(
        behandlingId: UUID,
        aarsoppgjoerId: UUID,
        avkortingsperioder: List<YtelseFoerAvkorting>,
        tx: TransactionalSession,
    ) = avkortingsperioder.forEach {
        queryOf(
            statement =
                """
                INSERT INTO avkorting_aarsoppgjoer_ytelse_foer_avkorting(
                    id, behandling_id, beregning, fom, tom, beregningsreferanse, aarsoppgjoer_id
                ) VALUES (
                    :id, :behandlingId, :beregning, :fom, :tom, :beregningsreferanse, :aarsoppgjoerId
                )
                """.trimIndent(),
            paramMap =
                mapOf(
                    "id" to UUID.randomUUID(),
                    "behandlingId" to behandlingId,
                    "aarsoppgjoerId" to aarsoppgjoerId,
                    "beregning" to it.beregning,
                    "fom" to it.periode.fom.atDay(1),
                    "tom" to it.periode.tom?.atDay(1),
                    "beregningsreferanse" to it.beregningsreferanse,
                ),
        ).let { query -> tx.run(query.asUpdate) }
    }

    private fun lagreAvkortingsperioder(
        behandlingId: UUID,
        aarsoppgjoerId: UUID,
        avkortingsperioder: List<Avkortingsperiode>,
        tx: TransactionalSession,
    ) = avkortingsperioder.forEach {
        queryOf(
            statement =
                """
                INSERT INTO avkortingsperioder(
                    id, behandling_id, fom, tom, avkorting, tidspunkt, regel_resultat, kilde, inntektsgrunnlag, aarsoppgjoer_id
                ) VALUES (
                    :id, :behandlingId, :fom, :tom, :avkorting,:tidspunkt, :regel_resultat, :kilde, :inntektsgrunnlag, :aarsoppgjoerId
                )
                """.trimIndent(),
            paramMap =
                mapOf(
                    "id" to it.id,
                    "behandlingId" to behandlingId,
                    "aarsoppgjoerId" to aarsoppgjoerId,
                    "fom" to it.periode.fom.atDay(1),
                    "tom" to it.periode.tom?.atDay(1),
                    "avkorting" to it.avkorting,
                    "tidspunkt" to it.tidspunkt.toTimestamp(),
                    "regel_resultat" to it.regelResultat.toJson(),
                    "kilde" to it.kilde.toJson(),
                    "inntektsgrunnlag" to it.inntektsgrunnlag,
                ),
        ).let { query -> tx.run(query.asUpdate) }
    }

    private fun lagreRestanse(
        behandlingId: UUID,
        aarsoppgjoerId: UUID,
        restanse: Restanse,
        tx: TransactionalSession,
    ) = queryOf(
        statement =
            """
            INSERT INTO avkorting_aarsoppgjoer_restanse(
                id, behandling_id, total_restanse, fordelt_restanse, tidspunkt, regel_resultat, kilde, aarsoppgjoer_id
            ) VALUES (
                :id, :behandlingId, :total_restanse, :fordelt_restanse, :tidspunkt, :regel_resultat, :kilde, :aarsoppgjoerId
            )
            """.trimIndent(),
        paramMap =
            mapOf(
                "id" to restanse.id,
                "behandlingId" to behandlingId,
                "aarsoppgjoerId" to aarsoppgjoerId,
                "total_restanse" to restanse.totalRestanse,
                "fordelt_restanse" to restanse.fordeltRestanse,
                "tidspunkt" to restanse.tidspunkt.toTimestamp(),
                "regel_resultat" to restanse.regelResultat?.toJson(),
                "kilde" to restanse.kilde.toJson(),
            ),
    ).let { query -> tx.run(query.asUpdate) }

    private fun lagreAvkortetYtelse(
        behandlingId: UUID,
        aarsoppgjoerId: UUID,
        avkortetYtelse: List<AvkortetYtelse>,
        tx: TransactionalSession,
    ) = avkortetYtelse.forEach {
        it.restanse?.let { restanse -> lagreRestanse(behandlingId, aarsoppgjoerId, restanse, tx) }
        queryOf(
            statement =
                """
                INSERT INTO avkortet_ytelse(
                    id, behandling_id, type, fom, tom, ytelse_etter_avkorting, avkortingsbeloep, restanse, 
                    ytelse_foer_avkorting, ytelse_etter_avkorting_uten_restanse, tidspunkt, regel_resultat, kilde,
                    inntektsgrunnlag, aarsoppgjoer_id, sanksjon_id
                ) VALUES (
                    :id, :behandlingId, :type, :fom, :tom,:ytelseEtterAvkorting, :avkortingsbeloep, :restanse,
                    :ytelseFoerAvkorting, :ytelseEtterAvkortingFoerRestanse, :tidspunkt, :regel_resultat, :kilde,
                    :inntektsgrunnlag, :aarsoppgjoerId, :sanksjon_id
                )
                """.trimIndent(),
            paramMap =
                mapOf(
                    "id" to it.id,
                    "type" to it.type.name,
                    "behandlingId" to behandlingId,
                    "aarsoppgjoerId" to aarsoppgjoerId,
                    "fom" to it.periode.fom.atDay(1),
                    "tom" to it.periode.tom?.atDay(1),
                    "ytelseEtterAvkorting" to it.ytelseEtterAvkorting,
                    "avkortingsbeloep" to it.avkortingsbeloep,
                    "restanse" to it.restanse?.id,
                    "ytelseFoerAvkorting" to it.ytelseFoerAvkorting,
                    "ytelseEtterAvkortingFoerRestanse" to it.ytelseEtterAvkortingFoerRestanse,
                    "tidspunkt" to it.tidspunkt.toTimestamp(),
                    "regel_resultat" to it.regelResultat.toJson(),
                    "kilde" to it.kilde.toJson(),
                    "inntektsgrunnlag" to it.inntektsgrunnlag,
                    "sanksjon_id" to it.sanksjon?.sanksjonId,
                ),
        ).let { query -> tx.run(query.asUpdate) }
    }

    private fun Row.toInntektInnvilgetPeriode() =
        BenyttetInntektInnvilgetPeriode(
            verdi = int("inntekt"),
            regelResultat = objectMapper.readTree(string("regel_resultat")),
            kilde = string("kilde").let { objectMapper.readValue(it) },
            tidspunkt = sqlTimestamp("tidspunkt").toTidspunkt(),
        )

    private fun Row.toForventetInntekt(
        id: UUID,
        inntektInnvilgetPeriode: InntektInnvilgetPeriode,
    ) = ForventetInntekt(
        id = id,
        periode =
            Periode(
                fom = sqlDate("fom").let { YearMonth.from(it.toLocalDate()) },
                tom = sqlDateOrNull("tom")?.let { YearMonth.from(it.toLocalDate()) },
            ),
        inntektTom = int("inntekt_tom"),
        fratrekkInnAar = int("fratrekk_inn_ut"),
        inntektUtlandTom = int("inntekt_utland_tom"),
        fratrekkInnAarUtland = int("fratrekk_inn_aar_utland"),
        innvilgaMaaneder = int("relevante_maaneder"),
        spesifikasjon = string("spesifikasjon"),
        kilde = string("kilde").let { objectMapper.readValue(it) },
        overstyrtInnvilgaMaanederAarsak =
            stringOrNull("overstyrt_innvilga_maaneder_aarsak")?.let {
                OverstyrtInnvilgaMaanederAarsak.valueOf(it)
            },
        overstyrtInnvilgaMaanederBegrunnelse = stringOrNull("overstyrt_innvilga_maaneder_begrunnelse"),
        inntektInnvilgetPeriode = inntektInnvilgetPeriode,
        maanederInnvilget = stringOrNull("maaneder_innvilget")?.let { objectMapper.readValue(it) },
        maanederInnvilgetRegelResultat = null, // Vi trenger ikke hente regelresultatet når vi skal bruke verdien
    )

    private fun Row.toFaktiskInntekt(
        id: UUID,
        inntektInnvilgetPeriode: BenyttetInntektInnvilgetPeriode,
    ) = FaktiskInntekt(
        id = id,
        periode =
            Periode(
                fom = sqlDate("fom").let { YearMonth.from(it.toLocalDate()) },
                tom = sqlDate("tom").let { YearMonth.from(it.toLocalDate()) },
            ),
        innvilgaMaaneder = int("innvilgede_maaneder"),
        loennsinntekt = int("loennsinntekt"),
        naeringsinntekt = int("naeringsinntekt"),
        afp = int("afp"),
        utlandsinntekt = int("utlandsinntekt"),
        kilde =
            string("kilde").let {
                objectMapper.readValue(it)
            },
        inntektInnvilgetPeriode = inntektInnvilgetPeriode,
        spesifikasjon = stringOrNull("spesifikasjon") ?: "", // TODO fjern denne når feltet har blitt satt til NOT NULL,
        maanederInnvilget = stringOrNull("maaneder_innvilget")?.let { objectMapper.readValue(it) },
        maanederInnvilgetRegelResultat = null, // Vi trenger ikke hente regelresultatet når vi skal bruke verdien
    )

    private fun Row.toYtelseFoerAvkorting() =
        YtelseFoerAvkorting(
            beregning = int("beregning"),
            periode =
                Periode(
                    fom = sqlDate("fom").let { YearMonth.from(it.toLocalDate()) },
                    tom = sqlDateOrNull("tom")?.let { YearMonth.from(it.toLocalDate()) },
                ),
            beregningsreferanse = uuid("beregningsreferanse"),
        )

    private fun Row.toAvkortingsperiode() =
        Avkortingsperiode(
            id = uuid("id"),
            periode =
                Periode(
                    fom = sqlDate("fom").let { YearMonth.from(it.toLocalDate()) },
                    tom = sqlDateOrNull("tom")?.let { YearMonth.from(it.toLocalDate()) },
                ),
            avkorting = int("avkorting"),
            tidspunkt = sqlTimestamp("tidspunkt").toTidspunkt(),
            regelResultat = objectMapper.readTree(string("regel_resultat")),
            kilde = string("kilde").let { objectMapper.readValue(it) },
            inntektsgrunnlag = uuid("inntektsgrunnlag"),
        )

    private fun Row.toRestanse() =
        Restanse(
            id = uuid("id"),
            totalRestanse = int("total_restanse"),
            fordeltRestanse = int("fordelt_restanse"),
            tidspunkt = sqlTimestamp("tidspunkt").toTidspunkt(),
            regelResultat = string("regel_resultat").let { objectMapper.readTree(it) },
            kilde = string("kilde").let { objectMapper.readValue(it) },
        )

    private fun Row.toAvkortetYtelse(
        allRestanse: List<Restanse>,
        sanksjonertYtelse: List<SanksjonertYtelse>,
    ) = AvkortetYtelse(
        id = uuid("id"),
        type = string("type").let { AvkortetYtelseType.valueOf(it) },
        periode =
            Periode(
                fom = sqlDate("fom").let { YearMonth.from(it.toLocalDate()) },
                tom = sqlDateOrNull("tom")?.let { YearMonth.from(it.toLocalDate()) },
            ),
        ytelseEtterAvkorting = int("ytelse_etter_avkorting"),
        avkortingsbeloep = int("avkortingsbeloep"),
        restanse = uuidOrNull("restanse").let { restanseUuid -> allRestanse.find { it.id == restanseUuid } },
        ytelseEtterAvkortingFoerRestanse = int("ytelse_etter_avkorting_uten_restanse"),
        ytelseFoerAvkorting = int("ytelse_foer_avkorting"),
        tidspunkt = sqlTimestamp("tidspunkt").toTidspunkt(),
        regelResultat = objectMapper.readTree(string("regel_resultat")),
        kilde = string("kilde").let { objectMapper.readValue(it) },
        inntektsgrunnlag = uuidOrNull("inntektsgrunnlag"),
        sanksjon = uuidOrNull("sanksjon_id").let { sanksjonId -> sanksjonertYtelse.find { it.sanksjonId == sanksjonId } },
    )

    private fun Row.toSanksjonerYtelse(): SanksjonertYtelse =
        SanksjonertYtelse(
            sanksjonId = uuid("sanksjon_id"),
            sanksjonType = enumValueOf(string("sanksjon_type")),
        )

    fun hentAlleAarsoppgjoer(sakId: SakId): List<AarsoppgjoerLoepende> =
        dataSource.transaction { tx ->
            queryOf(
                "SELECT * FROM avkorting_aarsoppgjoer WHERE sak_id= ? ORDER BY aar ASC",
                sakId.sakId,
            ).let { query ->
                tx.run(
                    query
                        .map { row ->
                            AarsoppgjoerLoepende(
                                id = row.uuid("id"),
                                aar = row.int("aar"),
                                fom = row.sqlDate("fom").let { YearMonth.from(it.toLocalDate()) },
                            )
                        }.asList,
                )
            }
        }

    fun hentAlleBehandlingerMedAarsoppgjoer(sakId: SakId): List<UUID> =
        dataSource.transaction { tx ->
            queryOf(
                "SELECT * FROM avkorting_aarsoppgjoer WHERE sak_id= ? ORDER BY aar DESC",
                sakId.sakId,
            ).let { query ->
                tx.run(
                    query
                        .map { row ->
                            row.uuid("behandling_id")
                        }.asList,
                )
            }
        }

    fun hentAlleAarsoppgjoer(behandlinger: List<UUID>): List<AarsoppgjoerLoepende> =
        dataSource.transaction { tx ->
            queryOf(
                "SELECT * FROM avkorting_aarsoppgjoer WHERE behandling_id = ANY (?) ORDER BY aar ASC",
                behandlinger.toTypedArray(),
            ).let { query ->
                tx.run(
                    query
                        .map { row ->
                            AarsoppgjoerLoepende(
                                id = row.uuid("id"),
                                aar = row.int("aar"),
                                fom = row.sqlDate("fom").let { YearMonth.from(it.toLocalDate()) },
                            )
                        }.asList,
                )
            }
        }
}
