package no.nav.etterlatte.beregning

import kotliquery.Row
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.etterlatte.libs.common.periode.Periode
import java.time.YearMonth
import java.util.*
import javax.sql.DataSource

class AvkortingRepository(private val dataSource: DataSource) {

    fun hentAvkorting(behandlingId: UUID): Avkorting? = using(sessionOf(dataSource)) { session ->
        session.transaction { tx ->
            queryOf(
                "SELECT * FROM avkortinggrunnlag WHERE behandling_id = ?",
                behandlingId
            ).let { query ->
                tx.run(query.map { row -> row.toAvkortinggrunnlag() }.asList).ifEmpty {
                    null
                }
            }?.let { avkortinggrunnlag ->
                Avkorting(
                    behandlingId = behandlingId,
                    avkortinggrunnlag,
                    beregningEtterAvkorting = emptyList()
                )
            }
        }
    }

    fun lagreEllerOppdaterAvkortingGrunnlag(behandlingId: UUID, avkortingGrunnlag: AvkortingGrunnlag): Avkorting {
        using(sessionOf(dataSource)) { session ->
            session.transaction { tx ->
                queryOf(
                    "DELETE FROM avkortinggrunnlag WHERE behandling_id = ?",
                    behandlingId
                ).let { query ->
                    tx.run(query.asUpdate)
                }

                queryOf(
                    statement = """
                        INSERT INTO avkortinggrunnlag(
                        id, behandling_id, fom, tom, aarsinntekt, gjeldende_aar, spesifikasjon
                        ) VALUES (
                        :id, :behandlingId, :fom, :tom, :aarsinntekt, :gjeldendeAar, :spesifikasjon
                        )
                    """.trimIndent(),
                    paramMap = mapOf(
                        "id" to UUID.randomUUID(),
                        "behandlingId" to behandlingId,
                        "fom" to avkortingGrunnlag.periode.fom.atDay(1),
                        "tom" to avkortingGrunnlag.periode.tom?.atDay(1),
                        "aarsinntekt" to avkortingGrunnlag.aarsinntekt,
                        "gjeldendeAar" to avkortingGrunnlag.gjeldendeAar,
                        "spesifikasjon" to avkortingGrunnlag.spesifikasjon
                    )
                ).let { tx.run(it.asUpdate) }
            }
        }
        return hentAvkortingUtenNullable(behandlingId)
    }

    private fun hentAvkortingUtenNullable(behandlingId: UUID): Avkorting =
        hentAvkorting(behandlingId) ?: throw Exception("Uthenting av avkorting for behandling $behandlingId feilet")

    private fun Row.toAvkortinggrunnlag() = AvkortingGrunnlag(
        periode = Periode(
            fom = sqlDate("fom").let { YearMonth.from(it.toLocalDate()) },
            tom = sqlDateOrNull("tom")?.let { YearMonth.from(it.toLocalDate()) }
        ),
        aarsinntekt = int("aarsinntekt"),
        gjeldendeAar = int("gjeldende_aar"),
        spesifikasjon = string("spesifikasjon")
    )
}