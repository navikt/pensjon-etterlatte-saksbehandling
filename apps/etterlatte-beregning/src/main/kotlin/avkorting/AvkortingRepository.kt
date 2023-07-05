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

class AvkortingRepository(private val dataSource: DataSource) {

    fun hentAvkorting(behandlingId: UUID): Avkorting? = dataSource.transaction { tx ->
        val avkortingGrunnlag = queryOf(
            "SELECT * FROM avkortingsgrunnlag WHERE behandling_id = ?",
            behandlingId
        ).let { query -> tx.run(query.map { row -> row.toAvkortingsgrunnlag() }.asList) }

        val ytelseFoerAvkorting = queryOf(
            "SELECT * FROM avkorting_aarsoppgjoer_ytelse_foer_avkorting WHERE behandling_id = ?",
            behandlingId
        ).let { query -> tx.run(query.map { row -> row.toYtelseFoerAvkorting() }.asList) }

        val avkortingsperioder = queryOf(
            "SELECT * FROM avkortingsperioder WHERE behandling_id = ?",
            behandlingId
        ).let { query -> tx.run(query.map { row -> row.toAvkortingsperiode() }.asList) }

        val tidligereAvkortetYtelse = queryOf(
            "SELECT * FROM avkortet_ytelse WHERE behandling_id = ? AND type = ?",
            behandlingId,
            AvkortetYtelseType.TIDLIGERE.name
        ).let { query -> tx.run(query.map { row -> row.toAvkortetYtelse() }.asList) }

        val reberegnetAvkortetYtelse = queryOf(
            "SELECT * FROM avkortet_ytelse WHERE behandling_id = ? AND type = ?",
            behandlingId,
            AvkortetYtelseType.REBEREGNET.name
        ).let { query -> tx.run(query.map { row -> row.toAvkortetYtelse() }.asList) }

        val restanse = queryOf(
            "SELECT * FROM avkorting_aarsoppgjoer_restanse WHERE behandling_id = ?",
            behandlingId
        ).let { query -> tx.run(query.map { row -> row.toRestanse() }.asSingle) }

        val avkortetYtelse = queryOf(
            "SELECT * FROM avkortet_ytelse WHERE behandling_id = ? AND type = ?",
            behandlingId,
            AvkortetYtelseType.NY.name
        ).let { query -> tx.run(query.map { row -> row.toAvkortetYtelse() }.asList) }

        if (avkortingGrunnlag.isEmpty()) {
            null
        } else {
            Avkorting(
                avkortingGrunnlag = avkortingGrunnlag,
                aarsoppgjoer = Aarsoppgjoer(
                    ytelseFoerAvkorting = ytelseFoerAvkorting,
                    avkortingsperioder = avkortingsperioder,
                    tidligereAvkortetYtelse = tidligereAvkortetYtelse,
                    reberegnetAvkortetYtelse = reberegnetAvkortetYtelse,
                    restanse = restanse,
                ),
                avkortetYtelse = avkortetYtelse
            )
        }
    }

    fun hentAvkortingUtenNullable(behandlingId: UUID): Avkorting =
        hentAvkorting(behandlingId) ?: throw Exception("Uthenting av avkorting for behandling $behandlingId feilet")

    fun lagreAvkorting(behandlingId: UUID, avkorting: Avkorting): Avkorting {
        dataSource.transaction { tx ->
            slettAvkorting(behandlingId, tx)
            lagreAvkortingGrunnlag(behandlingId, avkorting.avkortingGrunnlag, tx)
            with(avkorting.aarsoppgjoer) {
                lagreYtelseFoerAvkorting(behandlingId, ytelseFoerAvkorting, tx)
                lagreAvkortingsperioder(behandlingId, avkortingsperioder, tx)
                lagreAvkortetYtelse(behandlingId, tidligereAvkortetYtelse, tx)
                lagreAvkortetYtelse(behandlingId, reberegnetAvkortetYtelse, tx)
                restanse?.let {lagreRestanse(behandlingId, it, tx) }
            }
            lagreAvkortetYtelse(behandlingId, avkorting.avkortetYtelse, tx)
        }
        return hentAvkortingUtenNullable(behandlingId)
    }

    private fun slettAvkorting(behandlingId: UUID, tx: TransactionalSession) {
        queryOf(
            "DELETE FROM avkorting_aarsoppgjoer_ytelse_foer_avkorting WHERE behandling_id  = ?",
            behandlingId
        ).let { query ->
            tx.run(query.asUpdate)
        }
        queryOf(
            "DELETE FROM avkortingsperioder WHERE behandling_id = ?",
            behandlingId
        ).let { query ->
            tx.run(query.asUpdate)
        }
        queryOf(
            "DELETE FROM avkorting_aarsoppgjoer_restanse WHERE behandling_id  = ?",
            behandlingId
        ).let { query ->
            tx.run(query.asUpdate)
        }
        queryOf(
            "DELETE FROM avkortet_ytelse WHERE behandling_id  = ?",
            behandlingId
        ).let { query ->
            tx.run(query.asUpdate)
        }
        queryOf(
            "DELETE FROM avkortingsgrunnlag WHERE behandling_id = ?",
            behandlingId
        ).let { query ->
            tx.run(query.asUpdate)
        }
    }

    private fun lagreAvkortingGrunnlag(
        behandlingId: UUID,
        avkortingsgrunnlag: List<AvkortingGrunnlag>,
        tx: TransactionalSession
    ) = avkortingsgrunnlag.forEach {
        queryOf(
            statement = """
                INSERT INTO avkortingsgrunnlag(
                    id, behandling_id, fom, tom, aarsinntekt, fratrekk_inn_ut, relevante_maaneder, spesifikasjon, kilde
                ) VALUES (
                    :id, :behandlingId, :fom, :tom, :aarsinntekt, :fratrekkInnAar, :relevanteMaanederInnAar,
                     :spesifikasjon, :kilde
                )
            """.trimIndent(),
            paramMap = mapOf(
                "id" to it.id,
                "behandlingId" to behandlingId,
                "fom" to it.periode.fom.atDay(1),
                "tom" to it.periode.tom?.atDay(1),
                "aarsinntekt" to it.aarsinntekt,
                "fratrekkInnAar" to it.fratrekkInnAar,
                "relevanteMaanederInnAar" to it.relevanteMaanederInnAar,
                "spesifikasjon" to it.spesifikasjon,
                "kilde" to it.kilde.toJson()
            )
        ).let { query -> tx.run(query.asUpdate) }
    }

    private fun lagreYtelseFoerAvkorting(
        behandlingId: UUID,
        avkortingsperioder: List<YtelseFoerAvkorting>,
        tx: TransactionalSession
    ) = avkortingsperioder.forEach {
        queryOf(
            statement = """
                INSERT INTO avkorting_aarsoppgjoer_ytelse_foer_avkorting(
                    id, behandling_id, beregning, fom, tom, beregningsreferanse
                ) VALUES (
                    :id, :behandlingId, :beregning, :fom, :tom, :beregningsreferanse
                )
            """.trimIndent(),
            paramMap = mapOf(
                "id" to UUID.randomUUID(),
                "behandlingId" to behandlingId,
                "beregning" to it.beregning,
                "fom" to it.periode.fom.atDay(1),
                "tom" to it.periode.tom?.atDay(1),
                "beregningsreferanse" to it.beregningsreferanse
            )
        ).let { query -> tx.run(query.asUpdate) }
    }

    private fun lagreAvkortingsperioder(
        behandlingId: UUID,
        avkortingsperioder: List<Avkortingsperiode>,
        tx: TransactionalSession
    ) = avkortingsperioder.forEach {
        queryOf(
            statement = """
                INSERT INTO avkortingsperioder(
                    id, behandling_id, fom, tom, avkorting, tidspunkt, regel_resultat, kilde
                ) VALUES (
                    :id, :behandlingId, :fom, :tom, :avkorting,:tidspunkt, :regel_resultat, :kilde
                )
            """.trimIndent(),
            paramMap = mapOf(
                "id" to it.id,
                "behandlingId" to behandlingId,
                "fom" to it.periode.fom.atDay(1),
                "tom" to it.periode.tom?.atDay(1),
                "avkorting" to it.avkorting,
                "tidspunkt" to it.tidspunkt.toTimestamp(),
                "regel_resultat" to it.regelResultat.toJson(),
                "kilde" to it.kilde.toJson()
            )
        ).let { query -> tx.run(query.asUpdate) }
    }

    private fun lagreRestanse(
        behandlingId: UUID,
        restanse: Restanse,
        tx: TransactionalSession
    ) = queryOf(
        statement = """
                INSERT INTO avkorting_aarsoppgjoer_restanse(
                    id, behandling_id, total_restanse, fordelt_restanse, tidspunkt, regel_resultat, kilde
                ) VALUES (
                    :id, :behandlingId, :total_restanse, :fordelt_restanse, :tidspunkt, :regel_resultat, :kilde
                )
            """.trimIndent(),
        paramMap = mapOf(
            "id" to restanse.id,
            "behandlingId" to behandlingId,
            "total_restanse" to restanse.totalRestanse,
            "fordelt_restanse" to restanse.fordeltRestanse,
            "tidspunkt" to restanse.tidspunkt?.toTimestamp(),
            "regel_resultat" to restanse.regelResultat?.toJson(),
            "kilde" to restanse.kilde?.toJson()
        )
    ).let { query -> tx.run(query.asUpdate) }

    private fun lagreAvkortetYtelse(
        behandlingId: UUID,
        avkortetYtelse: List<AvkortetYtelse>,
        tx: TransactionalSession
    ) =
        avkortetYtelse.forEach {
            queryOf(
                statement = """
                    INSERT INTO avkortet_ytelse(
                        id, behandling_id, type, fom, tom, ytelse_etter_avkorting, avkortingsbeloep, restanse, 
                        ytelse_foer_avkorting, ytelse_etter_avkorting_uten_restanse, tidspunkt, regel_resultat, kilde
                    ) VALUES (
                        :id, :behandlingId, :type, :fom, :tom,:ytelseEtterAvkorting, :avkortingsbeloep, :restanse,
                        :ytelseFoerAvkorting, :ytelseEtterAvkortingFoerRestanse, :tidspunkt, :regel_resultat, :kilde
                    )
                """.trimIndent(),
                paramMap = mapOf(
                    "id" to it.id,
                    "type" to it.type.name,
                    "behandlingId" to behandlingId,
                    "fom" to it.periode.fom.atDay(1),
                    "tom" to it.periode.tom?.atDay(1),
                    "ytelseEtterAvkorting" to it.ytelseEtterAvkorting,
                    "avkortingsbeloep" to it.avkortingsbeloep,
                    "restanse" to it.restanse,
                    "ytelseFoerAvkorting" to it.ytelseFoerAvkorting,
                    "ytelseEtterAvkortingFoerRestanse" to it.ytelseEtterAvkortingFoerRestanse,
                    "tidspunkt" to it.tidspunkt.toTimestamp(),
                    "regel_resultat" to it.regelResultat.toJson(),
                    "kilde" to it.kilde.toJson()
                )
            ).let { query -> tx.run(query.asUpdate) }
        }

    private fun Row.toAvkortingsgrunnlag() = AvkortingGrunnlag(
        id = uuid("id"),
        periode = Periode(
            fom = sqlDate("fom").let { YearMonth.from(it.toLocalDate()) },
            tom = sqlDateOrNull("tom")?.let { YearMonth.from(it.toLocalDate()) }
        ),
        aarsinntekt = int("aarsinntekt"),
        fratrekkInnAar = int("fratrekk_inn_ut"),
        relevanteMaanederInnAar = int("relevante_maaneder"),
        spesifikasjon = string("spesifikasjon"),
        kilde = string("kilde").let { objectMapper.readValue(it) }
    )
    private fun Row.toYtelseFoerAvkorting() = YtelseFoerAvkorting(
        beregning = int("beregning"),
        periode = Periode(
            fom = sqlDate("fom").let { YearMonth.from(it.toLocalDate()) },
            tom = sqlDateOrNull("tom")?.let { YearMonth.from(it.toLocalDate()) }
        ),
        beregningsreferanse = uuid("beregningsreferanse")
    )
    private fun Row.toAvkortingsperiode() = Avkortingsperiode(
        id = uuid("id"),
        periode = Periode(
            fom = sqlDate("fom").let { YearMonth.from(it.toLocalDate()) },
            tom = sqlDateOrNull("tom")?.let { YearMonth.from(it.toLocalDate()) }
        ),
        avkorting = int("avkorting"),
        tidspunkt = sqlTimestamp("tidspunkt").toTidspunkt(),
        regelResultat = objectMapper.readTree(string("regel_resultat")),
        kilde = string("kilde").let { objectMapper.readValue(it) }
    )
    private fun Row.toRestanse() = Restanse(
        id = uuid("id"),
        totalRestanse = int("total_restanse"),
        fordeltRestanse = int("fordelt_restanse"),
        tidspunkt = sqlTimestamp("tidspunkt").toTidspunkt() ,
        regelResultat = string("regel_resultat").let { objectMapper.readTree(it) },
        kilde = string("kilde").let { objectMapper.readValue(it) }
    )
    private fun Row.toAvkortetYtelse() = AvkortetYtelse(
        id = uuid("id"),
        type = string("type").let { AvkortetYtelseType.valueOf(it) },
        periode = Periode(
            fom = sqlDate("fom").let { YearMonth.from(it.toLocalDate()) },
            tom = sqlDateOrNull("tom")?.let { YearMonth.from(it.toLocalDate()) }
        ),
        ytelseEtterAvkorting = int("ytelse_etter_avkorting"),
        avkortingsbeloep = int("avkortingsbeloep"),
        restanse = int("restanse"),
        ytelseEtterAvkortingFoerRestanse = int("ytelse_etter_Avkorting_uten_restanse"),
        ytelseFoerAvkorting = int("ytelse_foer_avkorting"),
        tidspunkt = sqlTimestamp("tidspunkt").toTidspunkt(),
        regelResultat = objectMapper.readTree(string("regel_resultat")),
        kilde = string("kilde").let { objectMapper.readValue(it) }
    )

}