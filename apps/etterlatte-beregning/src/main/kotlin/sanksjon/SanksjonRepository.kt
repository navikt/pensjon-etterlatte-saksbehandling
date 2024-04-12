package no.nav.etterlatte.sanksjon

import kotliquery.Row
import kotliquery.queryOf
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.toTidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.toTimestamp
import no.nav.etterlatte.libs.database.transaction
import java.time.YearMonth
import java.util.UUID
import javax.sql.DataSource

class SanksjonRepository(private val dataSource: DataSource) {
    fun hentSanksjon(behandlingId: UUID): List<Sanksjon>? =
        dataSource.transaction { tx ->
            queryOf(
                statement =
                    "SELECT * FROM sanksjon WHERE behandling_id = ?",
                behandlingId,
            ).let { query ->
                tx.run(query.map { row -> row.toSanksjon() }.asList).ifEmpty {
                    null
                }
            }
        }

    fun opprettSanksjon(
        behandlingId: UUID,
        sanksjon: Sanksjon,
    ) {
        dataSource.transaction { tx ->
            queryOf(
                statement =
                    """
                    INSERT INTO sanksjon(
                        id, behandling_id, sak_id, fom, tom, saksbehandler, opprettet, endret, beskrivelse
                    ) VALUES (
                        :id, :behandlingId, :sak_id, :fom, :tom, :saksbehandler, :opprettet, :endret, :beskrivelse
                    )
                    """.trimIndent(),
                paramMap =
                    mapOf(
                        "id" to UUID.randomUUID(),
                        "behandlingId" to behandlingId,
                        "sak_id" to sanksjon.sakId,
                        "fom" to sanksjon.fom.atDay(1),
                        "tom" to sanksjon.tom?.atDay(1),
                        "saksbehandler" to sanksjon.saksbehandler,
                        "opprettet" to Tidspunkt.now().toTimestamp(),
                        "endret" to Tidspunkt.now().toTimestamp(),
                        "beskrivelse" to sanksjon.beskrivelse,
                    ),
            ).let { query -> tx.run(query.asUpdate) }
        }
    }

    fun oppdaterSanksjon(sanksjon: Sanksjon) {
        dataSource.transaction { tx ->
            queryOf(
                statement =
                    """
                    UPDATE sanksjon
                    SET fom = :fom, 
                        tom = :tom, 
                        saksbehandler = :saksbehandler, 
                        endret = :endret, 
                        beskrivelse = :beskrivelse
                    WHERE id = :id
                    """.trimIndent(),
                paramMap =
                    mapOf(
                        "id" to sanksjon.id,
                        "fom" to sanksjon.fom.atDay(1),
                        "tom" to sanksjon.tom?.atDay(1),
                        "saksbehandler" to sanksjon.saksbehandler,
                        "endret" to Tidspunkt.now().toTimestamp(),
                        "beskrivelse" to sanksjon.beskrivelse,
                    ),
            ).let { query -> tx.run(query.asUpdate) }
        }
    }

    private fun Row.toSanksjon() =
        Sanksjon(
            id = uuid("id"),
            behandlingId = uuid("behandling_id"),
            sakId = long("sak_id"),
            fom = sqlDate("fom").let { YearMonth.from(it.toLocalDate()) },
            tom = sqlDateOrNull("tom")?.let { YearMonth.from(it.toLocalDate()) },
            saksbehandler = string("saksbehandler"),
            opprettet = sqlTimestamp("opprettet").toTidspunkt(),
            endret = sqlTimestamp("endret").toTidspunkt(),
            beskrivelse = string("beskrivelse"),
        )
}

data class Sanksjon(
    val id: UUID?,
    val behandlingId: UUID,
    val sakId: Long,
    val fom: YearMonth,
    val tom: YearMonth?,
    val saksbehandler: String,
    val opprettet: Tidspunkt,
    val endret: Tidspunkt,
    val beskrivelse: String,
)
