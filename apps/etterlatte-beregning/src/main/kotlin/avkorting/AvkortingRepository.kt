package no.nav.etterlatte.avkorting

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
            val avkortingGrunnlag = queryOf(
                "SELECT * FROM avkortinggrunnlag WHERE behandling_id = ?",
                behandlingId
            ).let { query -> tx.run(query.map { row -> row.toAvkortinggrunnlag() }.asList) }

            val avkortingsperioder = queryOf(
                "SELECT * FROM avkortingsperioder WHERE behandling_id = ?",
                behandlingId
            ).let { query -> tx.run(query.map { row -> row.toBeregnetAvkortinggrunnlag() }.asList) }

            val avkortetYtelse = queryOf(
                "SELECT * FROM avkortet_ytelse WHERE behandling_id = ?",
                behandlingId
            ).let { query -> tx.run(query.map { row -> row.toAvkortetYtelse() }.asList) }

            if (avkortingGrunnlag.isEmpty()) {
                null
            } else {
                Avkorting(
                    behandlingId = behandlingId,
                    avkortingGrunnlag = avkortingGrunnlag,
                    avkortingsperioder = avkortingsperioder,
                    avkortetYtelse = avkortetYtelse
                )
            }
        }
    }

    fun lagreEllerOppdaterAvkorting(
        behandlingId: UUID,
        avkortingGrunnlag: List<AvkortingGrunnlag>,
        avkortingsperioder: List<Avkortingsperiode>,
        avkortetYtelse: List<AvkortetYtelse>
    ): Avkorting {
        using(sessionOf(dataSource)) { session ->
            session.transaction { tx ->
                slettAvkorting(behandlingId, tx)
                lagreAvkortingGrunnlag(behandlingId, avkortingGrunnlag, tx)
                lagreAvkortingsperioder(behandlingId, avkortingsperioder, tx)
                lagreAvkortetYtelse(behandlingId, avkortetYtelse, tx)
            }
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
            "DELETE FROM avkortinggrunnlag WHERE behandling_id = ?",
            behandlingId
        ).let { query ->
            tx.run(query.asUpdate)
        }
        queryOf(
            "DELETE FROM avkortet_ytelse WHERE behandling_id = ?",
            behandlingId
        ).let { query ->
            tx.run(query.asUpdate)
        }
    }

    private fun lagreAvkortingGrunnlag(
        behandlingId: UUID,
        avkortingGrunnlag: List<AvkortingGrunnlag>,
        tx: TransactionalSession
    ) = avkortingGrunnlag.forEach {
        queryOf(
            statement = """
                INSERT INTO avkortinggrunnlag(
                    id, behandling_id, fom, tom, aarsinntekt, gjeldende_aar, spesifikasjon, kilde
                ) VALUES (
                    :id, :behandlingId, :fom, :tom, :aarsinntekt, :gjeldendeAar, :spesifikasjon, :kilde
                )
            """.trimIndent(),
            paramMap = mapOf(
                "id" to UUID.randomUUID(),
                "behandlingId" to behandlingId,
                "fom" to it.periode.fom.atDay(1),
                "tom" to it.periode.tom?.atDay(1),
                "aarsinntekt" to it.aarsinntekt,
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

    private fun lagreAvkortetYtelse(
        behandlingId: UUID,
        avkortetYtelse: List<AvkortetYtelse>,
        tx: TransactionalSession
    ) =
        avkortetYtelse.forEach {
            queryOf(
                statement = """
                    INSERT INTO avkortet_ytelse(
                        id, behandling_id, fom, tom, ytelse_etter_avkorting, avkortingsbeloep,
                        ytelse_foer_avkorting, tidspunkt, regel_resultat, kilde
                    ) VALUES (
                        :id, :behandlingId, :fom, :tom,:ytelseEtterAvkorting, :avkortingsbeloep, :ytelseFoerAvkorting,
                        :tidspunkt, :regel_resultat, :kilde
                    )
                """.trimIndent(),
                paramMap = mapOf(
                    "id" to UUID.randomUUID(),
                    "behandlingId" to behandlingId,
                    "fom" to it.periode.fom.atDay(1),
                    "tom" to it.periode.tom?.atDay(1),
                    "ytelseEtterAvkorting" to it.ytelseEtterAvkorting,
                    "avkortingsbeloep" to it.avkortingsbeloep,
                    "ytelseFoerAvkorting" to it.ytelseFoerAvkorting,
                    "tidspunkt" to it.tidspunkt.toTimestamp(),
                    "regel_resultat" to it.regelResultat.toJson(),
                    "kilde" to it.kilde.toJson()
                )
            ).let { query -> tx.run(query.asUpdate) }
        }

    private fun hentAvkortingUtenNullable(behandlingId: UUID): Avkorting =
        hentAvkorting(behandlingId) ?: throw Exception("Uthenting av avkorting for behandling $behandlingId feilet")

    private fun Row.toAvkortinggrunnlag() = AvkortingGrunnlag(
        periode = Periode(
            fom = sqlDate("fom").let { YearMonth.from(it.toLocalDate()) },
            tom = sqlDateOrNull("tom")?.let { YearMonth.from(it.toLocalDate()) }
        ),
        aarsinntekt = int("aarsinntekt"),
        spesifikasjon = string("spesifikasjon"),
        kilde = string("kilde").let { objectMapper.readValue(it) }
    )

    private fun Row.toBeregnetAvkortinggrunnlag() = Avkortingsperiode(
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
        ytelseFoerAvkorting = int("ytelse_foer_avkorting"),
        tidspunkt = sqlTimestamp("tidspunkt").toTidspunkt(),
        regelResultat = objectMapper.readTree(string("regel_resultat")),
        kilde = string("kilde").let { objectMapper.readValue(it) }
    )
}