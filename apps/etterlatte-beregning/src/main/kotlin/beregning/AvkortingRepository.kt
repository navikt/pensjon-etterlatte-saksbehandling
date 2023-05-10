package no.nav.etterlatte.beregning

import com.fasterxml.jackson.module.kotlin.readValue
import kotliquery.Row
import kotliquery.TransactionalSession
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.periode.Periode
import no.nav.etterlatte.libs.common.tidspunkt.toTidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.toTimestamp
import no.nav.etterlatte.libs.common.toJson
import java.time.YearMonth
import java.util.*
import javax.sql.DataSource

class AvkortingRepository(private val dataSource: DataSource) {

    fun hentAvkorting(behandlingId: UUID): Avkorting? = using(sessionOf(dataSource)) { session ->
        session.transaction { tx ->
            hentAvkortingGrunnlag(tx, behandlingId)?.let { avkortingGrunnlag ->
                val avkortetYtelse = hentAvkortetYtelse(tx, behandlingId)
                Avkorting(
                    behandlingId = behandlingId,
                    avkortingGrunnlag = avkortingGrunnlag,
                    avkortetYtelse = avkortetYtelse
                )
            }
        }
    }

    private fun hentAvkortingGrunnlag(tx: TransactionalSession, behandlingId: UUID): List<AvkortingGrunnlag>? {
        val beregnetAvkortingGrunnlag = queryOf(
            "SELECT * FROM beregnet_avkortinggrunnlag WHERE behandling_id = ?",
            behandlingId
        ).let { query ->
            tx.run(query.map { row -> row.toBeregnetAvkortinggrunnlag() }.asList)
        }
        return queryOf(
            "SELECT * FROM avkortinggrunnlag WHERE behandling_id = ?",
            behandlingId
        ).let { query ->
            tx.run(query.map { row -> row.toAvkortinggrunnlag(beregnetAvkortingGrunnlag) }.asList).ifEmpty {
                null
            }
        }
    }

    private fun hentAvkortetYtelse(tx: TransactionalSession, behandlingId: UUID): List<AvkortetYtelse> {
        return queryOf(
            "SELECT * FROM avkortet_ytelse WHERE behandling_id = ?",
            behandlingId
        ).let { query ->
            tx.run(query.map { row -> row.toAvkortetYtelse() }.asList)
        }
    }

    fun lagreEllerOppdaterAvkortingGrunnlag(behandlingId: UUID, avkortingGrunnlag: AvkortingGrunnlag): Avkorting {
        using(sessionOf(dataSource)) { session ->
            session.transaction { tx ->
                queryOf(
                    "DELETE FROM beregnet_avkortinggrunnlag WHERE behandling_id = ?",
                    behandlingId
                ).let { query ->
                    tx.run(query.asUpdate)
                }
                queryOf(
                    "DELETE FROM avkortinggrunnlag WHERE behandling_id = ?",
                    behandlingId
                ).let { query ->
                    tx.run(query.asUpdate)
                }

                val avkortingGrunnlagId = UUID.randomUUID()
                queryOf(
                    statement = """
                        INSERT INTO avkortinggrunnlag(
                        id, behandling_id, fom, tom, aarsinntekt, gjeldende_aar, spesifikasjon, kilde
                        ) VALUES (
                        :id, :behandlingId, :fom, :tom, :aarsinntekt, :gjeldendeAar, :spesifikasjon, :kilde
                        )
                    """.trimIndent(),
                    paramMap = mapOf(
                        "id" to avkortingGrunnlagId,
                        "behandlingId" to behandlingId,
                        "fom" to avkortingGrunnlag.periode.fom.atDay(1),
                        "tom" to avkortingGrunnlag.periode.tom?.atDay(1),
                        "aarsinntekt" to avkortingGrunnlag.aarsinntekt,
                        "gjeldendeAar" to avkortingGrunnlag.gjeldendeAar,
                        "spesifikasjon" to avkortingGrunnlag.spesifikasjon,
                        "kilde" to avkortingGrunnlag.kilde.toJson()
                    )
                ).let { tx.run(it.asUpdate) }

                avkortingGrunnlag.beregnetAvkorting.forEach { beregnetAvkorting ->
                    queryOf(
                        statement = """
                        INSERT INTO beregnet_avkortinggrunnlag(
                        id, avkortinggrunnlag, behandling_id, fom, tom, avkorting, tidspunkt, regel_resultat, kilde
                        ) VALUES (
                        :id, :avkortingGrunnlag, :behandlingId, :fom, :tom, :avkorting,
                        :tidspunkt, :regel_resultat, :kilde
                        )
                        """.trimIndent(),
                        paramMap = mapOf(
                            "id" to UUID.randomUUID(),
                            "avkortingGrunnlag" to avkortingGrunnlagId,
                            "behandlingId" to behandlingId,
                            "fom" to beregnetAvkorting.periode.fom.atDay(1),
                            "tom" to beregnetAvkorting.periode.tom?.atDay(1),
                            "avkorting" to beregnetAvkorting.avkorting,
                            "tidspunkt" to beregnetAvkorting.tidspunkt.toTimestamp(),
                            "regel_resultat" to beregnetAvkorting.regelResultat.toJson(),
                            "kilde" to beregnetAvkorting.kilde.toJson()
                        )
                    ).let { tx.run(it.asUpdate) }
                }
            }
        }
        return hentAvkortingUtenNullable(behandlingId)
    }

    fun lagreEllerOppdaterAvkortetYtelse(behandlingId: UUID, avkortetYtelse: List<AvkortetYtelse>): Avkorting {
        using(sessionOf(dataSource)) { session ->
            session.transaction { tx ->
                queryOf(
                    "DELETE FROM avkortet_ytelse WHERE behandling_id = ?",
                    behandlingId
                ).let { query ->
                    tx.run(query.asUpdate)
                }

                avkortetYtelse.forEach {
                    queryOf(
                        statement = """
                        INSERT INTO avkortet_ytelse(
                        id, behandling_id, fom, tom, ytelse_etter_avkorting, tidspunkt, regel_resultat, kilde
                        ) VALUES (
                        :id, :behandlingId, :fom, :tom, :ytelseEtterAvkorting, :tidspunkt, :regel_resultat, :kilde
                        )
                        """.trimIndent(),
                        paramMap = mapOf(
                            "id" to UUID.randomUUID(),
                            "behandlingId" to behandlingId,
                            "fom" to it.periode.fom.atDay(1),
                            "tom" to it.periode.tom?.atDay(1),
                            "ytelseEtterAvkorting" to it.ytelseEtterAvkorting,
                            "tidspunkt" to it.tidspunkt.toTimestamp(),
                            "regel_resultat" to it.regelResultat.toJson(),
                            "kilde" to it.kilde.toJson()
                        )
                    ).let { query -> tx.run(query.asUpdate) }
                }
            }
        }
        return hentAvkortingUtenNullable(behandlingId)
    }

    private fun hentAvkortingUtenNullable(behandlingId: UUID): Avkorting =
        hentAvkorting(behandlingId) ?: throw Exception("Uthenting av avkorting for behandling $behandlingId feilet")

    private fun Row.toAvkortinggrunnlag(beregnetAvkorting: List<BeregnetAvkortingGrunnlag>) = AvkortingGrunnlag(
        periode = Periode(
            fom = sqlDate("fom").let { YearMonth.from(it.toLocalDate()) },
            tom = sqlDateOrNull("tom")?.let { YearMonth.from(it.toLocalDate()) }
        ),
        aarsinntekt = int("aarsinntekt"),
        gjeldendeAar = int("gjeldende_aar"),
        spesifikasjon = string("spesifikasjon"),
        kilde = string("kilde").let { objectMapper.readValue(it) },
        beregnetAvkorting = beregnetAvkorting
    )

    private fun Row.toBeregnetAvkortinggrunnlag() = BeregnetAvkortingGrunnlag(
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
        tidspunkt = sqlTimestamp("tidspunkt").toTidspunkt(),
        regelResultat = objectMapper.readTree(string("regel_resultat")),
        kilde = string("kilde").let { objectMapper.readValue(it) }
    )
}