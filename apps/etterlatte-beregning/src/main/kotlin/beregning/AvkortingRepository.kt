package no.nav.etterlatte.beregning

import kotliquery.Row
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
            val beregnetAvkortingGrunnlag = queryOf(
                "SELECT * FROM beregnet_avkortinggrunnlag WHERE behandling_id = ?",
                behandlingId
            ).let { query ->
                tx.run(query.map { row -> row.toBeregnetAvkortinggrunnlag() }.asList)
            }
            queryOf(
                "SELECT * FROM avkortinggrunnlag WHERE behandling_id = ?",
                behandlingId
            ).let { query ->
                tx.run(query.map { row -> row.toAvkortinggrunnlag(beregnetAvkortingGrunnlag) }.asList).ifEmpty {
                    null
                }
            }?.let { avkortinggrunnlag ->
                Avkorting(
                    behandlingId = behandlingId,
                    avkortinggrunnlag,
                    avkortetYtelse = emptyList()
                )
            }
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
                        id, behandling_id, fom, tom, aarsinntekt, gjeldende_aar, spesifikasjon
                        ) VALUES (
                        :id, :behandlingId, :fom, :tom, :aarsinntekt, :gjeldendeAar, :spesifikasjon
                        )
                    """.trimIndent(),
                    paramMap = mapOf(
                        "id" to avkortingGrunnlagId,
                        "behandlingId" to behandlingId,
                        "fom" to avkortingGrunnlag.periode.fom.atDay(1),
                        "tom" to avkortingGrunnlag.periode.tom?.atDay(1),
                        "aarsinntekt" to avkortingGrunnlag.aarsinntekt,
                        "gjeldendeAar" to avkortingGrunnlag.gjeldendeAar,
                        "spesifikasjon" to avkortingGrunnlag.spesifikasjon
                    )
                ).let { tx.run(it.asUpdate) }

                avkortingGrunnlag.beregnetAvkorting.forEach { beregnetAvkorting ->
                    queryOf(
                        statement = """
                        INSERT INTO beregnet_avkortinggrunnlag(
                        id, avkortinggrunnlag, behandling_id, fom, tom, avkorting, tidspunkt, regel_resultat 
                        ) VALUES (
                        :id, :avkortingGrunnlag, :behandlingId, :fom, :tom, :avkorting, :tidspunkt, :regel_resultat
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
                            "regel_resultat" to beregnetAvkorting.regelResultat.toJson()
                        )
                    ).let { tx.run(it.asUpdate) }
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
        beregnetAvkorting = beregnetAvkorting
    )

    private fun Row.toBeregnetAvkortinggrunnlag() = BeregnetAvkortingGrunnlag(
        periode = Periode(
            fom = sqlDate("fom").let { YearMonth.from(it.toLocalDate()) },
            tom = sqlDateOrNull("tom")?.let { YearMonth.from(it.toLocalDate()) }
        ),
        avkorting = int("avkorting"),
        tidspunkt = sqlTimestamp("tidspunkt").toTidspunkt(),
        regelResultat = objectMapper.readTree(string("regel_resultat"))
    )
}