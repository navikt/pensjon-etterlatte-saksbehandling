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

        val avkortingsperioder = queryOf(
            "SELECT * FROM avkortingsperioder WHERE behandling_id = ?",
            behandlingId
        ).let { query -> tx.run(query.map { row -> row.toAvkortingsperiode() }.asList) }

        val aarsoppgjoer = queryOf(
            "SELECT * FROM avkorting_aarsoppgjoer WHERE behandling_id = ?",
            behandlingId
        ).let { query -> tx.run(query.map { row -> row.toAarsoppgjoer() }.asList) }

        val avkortetYtelse = queryOf(
            "SELECT * FROM avkortet_ytelse WHERE behandling_id = ?",
            behandlingId
        ).let { query -> tx.run(query.map { row -> row.toAvkortetYtelse() }.asList) }

        if (avkortingGrunnlag.isEmpty()) {
            null
        } else {
            Avkorting(
                avkortingGrunnlag = avkortingGrunnlag,
                avkortingsperioder = avkortingsperioder,
                aarsoppgjoer = aarsoppgjoer,
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
            lagreAvkortingsperioder(behandlingId, avkorting.avkortingsperioder, tx)
            lagreAarsoppgjoer(behandlingId, avkorting.aarsoppgjoer, tx)
            lagreAvkortetYtelse(behandlingId, avkorting.avkortetYtelse, tx)
        }
        return hentAvkortingUtenNullable(behandlingId)
    }

    private fun slettAvkorting(behandlingId: UUID, tx: TransactionalSession) {
        queryOf(
            "DELETE FROM avkortingsperioder WHERE behandling_id = ?",
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
            "DELETE FROM avkorting_aarsoppgjoer WHERE behandling_id = ?",
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
                "id" to UUID.randomUUID(),
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

    private fun lagreAarsoppgjoer(
        behandlingId: UUID,
        aarsoppgjoer: List<AarsoppgjoerMaaned>,
        tx: TransactionalSession
    ) =
        aarsoppgjoer.forEach {
            // TODO EY-2368 legg til felter regelresultat og kilde for restanse og fordelt restanse
            queryOf(
                statement = """
                    INSERT INTO avkorting_aarsoppgjoer(
                        id, behandling_id, maaned,
                        beregning, avkorting, forventet_avkortet_ytelse, restanse, fordelt_restanse,
                        faktisk_avkortet_ytelse 
                    ) VALUES (
                        :id, :behandlingId, :maaned,
                        :beregning, :avkorting, :forventet_avkortet_ytelse, :restanse, :fordelt_restanse,
                        :faktisk_avkortet_ytelse 
                    )
                """.trimIndent(),
                paramMap = mapOf(
                    "id" to UUID.randomUUID(),
                    "behandlingId" to behandlingId,
                    "maaned" to it.maaned.atDay(1),
                    "beregning" to it.beregning,
                    "avkorting" to it.avkorting,
                    "forventet_avkortet_ytelse" to it.forventetAvkortetYtelse,
                    "restanse" to it.restanse,
                    "fordelt_restanse" to it.fordeltRestanse,
                    "faktisk_avkortet_ytelse" to it.faktiskAvkortetYtelse
                )
            ).let { query -> tx.run(query.asUpdate) }
        }

    private fun lagreAvkortetYtelse(
        behandlingId: UUID,
        avkortetYtelse: List<AvkortetYtelse>,
        tx: TransactionalSession
    ) =
        avkortetYtelse.forEach {
            // TODO EY-2368 legg til felt ytelseEtterAvkortingFoerRestanse
            queryOf(
                statement = """
                    INSERT INTO avkortet_ytelse(
                        id, behandling_id, fom, tom, ytelse_etter_avkorting, avkortingsbeloep, restanse, 
                        ytelse_foer_avkorting, tidspunkt, regel_resultat, kilde
                    ) VALUES (
                        :id, :behandlingId, :fom, :tom,:ytelseEtterAvkorting, :avkortingsbeloep, :restanse,
                        :ytelseFoerAvkorting, :tidspunkt, :regel_resultat, :kilde
                    )
                """.trimIndent(),
                paramMap = mapOf(
                    "id" to UUID.randomUUID(),
                    "behandlingId" to behandlingId,
                    "fom" to it.periode.fom.atDay(1),
                    "tom" to it.periode.tom?.atDay(1),
                    "ytelseEtterAvkorting" to it.ytelseEtterAvkorting,
                    "avkortingsbeloep" to it.avkortingsbeloep,
                    "restanse" to it.restanse,
                    "ytelseFoerAvkorting" to it.ytelseFoerAvkorting,
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

    private fun Row.toAvkortingsperiode() = Avkortingsperiode(
        periode = Periode(
            fom = sqlDate("fom").let { YearMonth.from(it.toLocalDate()) },
            tom = sqlDateOrNull("tom")?.let { YearMonth.from(it.toLocalDate()) }
        ),
        avkorting = int("avkorting"),
        tidspunkt = sqlTimestamp("tidspunkt").toTidspunkt(),
        regelResultat = objectMapper.readTree(string("regel_resultat")),
        kilde = string("kilde").let { objectMapper.readValue(it) }
    )

    private fun Row.toAvkortetYtelse() = AvkortetYtelse(
        periode = Periode(
            fom = sqlDate("fom").let { YearMonth.from(it.toLocalDate()) },
            tom = sqlDateOrNull("tom")?.let { YearMonth.from(it.toLocalDate()) }
        ),
        ytelseEtterAvkorting = int("ytelse_etter_avkorting"),
        avkortingsbeloep = int("avkortingsbeloep"),
        restanse = int("restanse"),
        ytelseEtterAvkortingFoerRestanse = 0,
        // TODO ytelseEtterAvkortingFoerRestanse = int("ytelse_etter_Avkorting_uten_restanse"),
        ytelseFoerAvkorting = int("ytelse_foer_avkorting"),
        tidspunkt = sqlTimestamp("tidspunkt").toTidspunkt(),
        regelResultat = objectMapper.readTree(string("regel_resultat")),
        kilde = string("kilde").let { objectMapper.readValue(it) }
    )

    private fun Row.toAarsoppgjoer() = AarsoppgjoerMaaned(
        maaned = sqlDate("maaned").let { YearMonth.from(it.toLocalDate()) },
        beregning = int("beregning"),
        avkorting = int("avkorting"),
        forventetAvkortetYtelse = int("forventet_avkortet_ytelse"),
        restanse = int("restanse"),
        fordeltRestanse = int("fordelt_restanse"),
        faktiskAvkortetYtelse = int("faktisk_avkortet_ytelse")
    )
}