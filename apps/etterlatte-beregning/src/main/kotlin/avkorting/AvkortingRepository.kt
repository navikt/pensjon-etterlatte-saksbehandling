package no.nav.etterlatte.avkorting

import com.fasterxml.jackson.module.kotlin.readValue
import kotliquery.Row
import kotliquery.TransactionalSession
import kotliquery.queryOf
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.periode.Periode
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
    fun hentAvkorting(behandlingId: UUID): Avkorting? =
        dataSource.transaction { tx ->
            val alleAarsoppgjoer =
                queryOf(
                    "SELECT * FROM avkorting_aarsoppgjoer WHERE behandling_id = ?",
                    behandlingId,
                ).let { query ->
                    tx.run(
                        query.map { row ->
                            Aarsoppgjoer(
                                id = row.uuid("id"),
                                aar = row.int("aar"),
                            )
                        }.asList,
                    )
                }

            if (alleAarsoppgjoer.isEmpty()) {
                null
            } else {
                val aarsoppgjoerUtfylt =
                    alleAarsoppgjoer.map { aarsoppgjoer ->

                        val avkortingGrunnlag =
                            queryOf(
                                "SELECT * FROM avkortingsgrunnlag WHERE aarsoppgjoer_id = ?",
                                aarsoppgjoer.id,
                            ).let { query -> tx.run(query.map { row -> row.toAvkortingsgrunnlag() }.asList) }

                        val ytelseFoerAvkorting =
                            queryOf(
                                "SELECT * FROM avkorting_aarsoppgjoer_ytelse_foer_avkorting WHERE aarsoppgjoer_id = ?",
                                aarsoppgjoer.id,
                            ).let { query -> tx.run(query.map { row -> row.toYtelseFoerAvkorting() }.asList) }

                        val restanse =
                            queryOf(
                                "SELECT * FROM avkorting_aarsoppgjoer_restanse WHERE aarsoppgjoer_id = ?",
                                aarsoppgjoer.id,
                            ).let { query -> tx.run(query.map { row -> row.toRestanse() }.asList) }

                        val inntektsavkorting =
                            avkortingGrunnlag.map {
                                val avkortingsperioder =
                                    queryOf(
                                        "SELECT * FROM avkortingsperioder WHERE inntektsgrunnlag = ?",
                                        it.id,
                                    ).let { query -> tx.run(query.map { row -> row.toAvkortingsperiode() }.asList) }

                                val avkortetYtelse =
                                    queryOf(
                                        "SELECT * FROM avkortet_ytelse WHERE inntektsgrunnlag = ?",
                                        it.id,
                                    ).let { query -> tx.run(query.map { row -> row.toAvkortetYtelse(restanse) }.asList) }

                                Inntektsavkorting(
                                    grunnlag = it,
                                    avkortingsperioder = avkortingsperioder,
                                    avkortetYtelseForventetInntekt = avkortetYtelse,
                                )
                            }

                        val avkortetYtelseAar =
                            queryOf(
                                "SELECT * FROM avkortet_ytelse WHERE aarsoppgjoer_id = ? AND type = ?",
                                aarsoppgjoer.id,
                                AvkortetYtelseType.AARSOPPGJOER.name,
                            ).let { query -> tx.run(query.map { row -> row.toAvkortetYtelse(restanse) }.asList) }

                        Aarsoppgjoer(
                            id = aarsoppgjoer.id,
                            aar = aarsoppgjoer.aar,
                            ytelseFoerAvkorting = ytelseFoerAvkorting,
                            inntektsavkorting = inntektsavkorting,
                            avkortetYtelseAar = avkortetYtelseAar,
                        )
                    }

                Avkorting(aarsoppgjoer = aarsoppgjoerUtfylt)
            }
        }

    fun lagreAvkorting(
        behandlingId: UUID,
        sakId: Long,
        avkorting: Avkorting,
    ) {
        dataSource.transaction { tx ->
            slettAvkorting(behandlingId, tx)
            avkorting.aarsoppgjoer.forEach { aarsoppgjoer ->
                with(aarsoppgjoer) {
                    lagreAarsoppgjoer(behandlingId, sakId, aarsoppgjoer, tx)
                    lagreYtelseFoerAvkorting(behandlingId, aarsoppgjoer.id, ytelseFoerAvkorting, tx)
                    inntektsavkorting.forEach {
                        lagreAvkortingGrunnlag(behandlingId, aarsoppgjoer.id, it.grunnlag, tx)
                        lagreAvkortingsperioder(behandlingId, aarsoppgjoer.id, it.avkortingsperioder, tx)
                        lagreAvkortetYtelse(behandlingId, aarsoppgjoer.id, it.avkortetYtelseForventetInntekt, tx)
                    }
                    lagreAvkortetYtelse(behandlingId, aarsoppgjoer.id, avkortetYtelseAar, tx)
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
            "DELETE FROM avkortingsgrunnlag WHERE behandling_id = ?",
            behandlingId,
        ).let { query ->
            tx.run(query.asUpdate)
        }
    }

    private fun lagreAarsoppgjoer(
        behandlingId: UUID,
        sakId: Long,
        aarsoppgjoer: Aarsoppgjoer,
        tx: TransactionalSession,
    ) = queryOf(
        statement =
            """
            INSERT INTO avkorting_aarsoppgjoer(
            	id, behandling_id, sak_id, aar
            ) VALUES (
            	:id, :behandling_id, :sak_id, :aar
            )
            """.trimIndent(),
        paramMap =
            mapOf(
                "id" to aarsoppgjoer.id,
                "behandling_id" to behandlingId,
                "sak_id" to sakId,
                "aar" to aarsoppgjoer.aar,
            ),
    ).let { query -> tx.run(query.asUpdate) }

    private fun lagreAvkortingGrunnlag(
        behandlingId: UUID,
        aarsoppgjoerId: UUID,
        avkortingsgrunnlag: AvkortingGrunnlag,
        tx: TransactionalSession,
    ) = queryOf(
        statement =
            """
            INSERT INTO avkortingsgrunnlag(
                id, behandling_id, fom, tom, aarsinntekt, fratrekk_inn_ut, inntekt_utland, fratrekk_inn_aar_utland,
                relevante_maaneder, spesifikasjon, kilde, aarsoppgjoer_id
            ) VALUES (
                :id, :behandlingId, :fom, :tom, :aarsinntekt, :fratrekkInnAar, :inntektUtland, :fratrekkInnAarUtland,
                :relevanteMaanederInnAar, :spesifikasjon, :kilde, :aarsoppgjoerId
            )
            """.trimIndent(),
        paramMap =
            mapOf(
                "id" to avkortingsgrunnlag.id,
                "behandlingId" to behandlingId,
                "aarsoppgjoerId" to aarsoppgjoerId,
                "fom" to avkortingsgrunnlag.periode.fom.atDay(1),
                "tom" to avkortingsgrunnlag.periode.tom?.atDay(1),
                "aarsinntekt" to avkortingsgrunnlag.aarsinntekt,
                "fratrekkInnAar" to avkortingsgrunnlag.fratrekkInnAar,
                "inntektUtland" to avkortingsgrunnlag.inntektUtland,
                "fratrekkInnAarUtland" to avkortingsgrunnlag.fratrekkInnAarUtland,
                "relevanteMaanederInnAar" to avkortingsgrunnlag.relevanteMaanederInnAar,
                "spesifikasjon" to avkortingsgrunnlag.spesifikasjon,
                "kilde" to avkortingsgrunnlag.kilde.toJson(),
            ),
    ).let { query -> tx.run(query.asUpdate) }

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
                    inntektsgrunnlag, aarsoppgjoer_id
                ) VALUES (
                    :id, :behandlingId, :type, :fom, :tom,:ytelseEtterAvkorting, :avkortingsbeloep, :restanse,
                    :ytelseFoerAvkorting, :ytelseEtterAvkortingFoerRestanse, :tidspunkt, :regel_resultat, :kilde,
                    :inntektsgrunnlag, :aarsoppgjoerId
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
                ),
        ).let { query -> tx.run(query.asUpdate) }
    }

    private fun Row.toAvkortingsgrunnlag() =
        AvkortingGrunnlag(
            id = uuid("id"),
            periode =
                Periode(
                    fom = sqlDate("fom").let { YearMonth.from(it.toLocalDate()) },
                    tom = sqlDateOrNull("tom")?.let { YearMonth.from(it.toLocalDate()) },
                ),
            aarsinntekt = int("aarsinntekt"),
            fratrekkInnAar = int("fratrekk_inn_ut"),
            relevanteMaanederInnAar = int("relevante_maaneder"),
            inntektUtland = int("inntekt_utland"),
            fratrekkInnAarUtland = int("fratrekk_inn_aar_utland"),
            spesifikasjon = string("spesifikasjon"),
            kilde = string("kilde").let { objectMapper.readValue(it) },
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

    private fun Row.toAvkortetYtelse(allRestanse: List<Restanse>) =
        AvkortetYtelse(
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
        )
}
