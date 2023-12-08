package no.nav.etterlatte.behandling.brevutfall

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.setTidspunkt
import no.nav.etterlatte.libs.database.singleOrNull
import no.nav.helse.rapids_rivers.toUUID
import java.sql.Connection
import java.sql.ResultSet
import java.time.YearMonth
import java.util.UUID

class BrevutfallDao(private val connection: () -> Connection) {
    fun lagre(brevutfall: Brevutfall): Brevutfall {
        return connection().prepareStatement(
            """
            INSERT INTO brevutfall(
                behandling_id, oppdatert, etterbetaling_fom, etterbetaling_tom, aldersgruppe, kilde
            )
            VALUES (?, ?, ?, ?, ?, ?)
            ON CONFLICT (behandling_id) DO 
            UPDATE SET 
                oppdatert = excluded.oppdatert, 
                etterbetaling_fom = excluded.etterbetaling_fom, 
                etterbetaling_tom = excluded.etterbetaling_tom, 
                aldersgruppe = excluded.aldersgruppe,
                kilde = excluded.kilde
            """.trimIndent(),
        )
            .apply {
                setObject(1, brevutfall.behandlingId)
                setTidspunkt(2, Tidspunkt.now())
                setDate(3, brevutfall.etterbetaling?.fom?.atDay(1)?.let { java.sql.Date.valueOf(it) })
                setDate(4, brevutfall.etterbetaling?.tom?.atEndOfMonth()?.let { java.sql.Date.valueOf(it) })
                setString(5, brevutfall.aldersgruppe?.name)
                setString(6, brevutfall.kilde.toJson())
            }
            .run { executeUpdate() }
            .also { require(it == 1) }
            .let { hent(brevutfall.behandlingId) ?: throw InternfeilException("Feilet under lagring av brevutfall") }
    }

    fun hent(behandlingId: UUID): Brevutfall? {
        return connection()
            .prepareStatement(
                """
                    SELECT behandling_id, etterbetaling_fom, etterbetaling_tom, aldersgruppe, kilde 
                    FROM brevutfall 
                    WHERE behandling_id = ?::UUID
                    """,
            )
            .apply { setObject(1, behandlingId) }
            .run { executeQuery().singleOrNull { toBrevutfall() } }
    }

    private fun ResultSet.toBrevutfall() =
        Brevutfall(
            behandlingId = getString("behandling_id").toUUID(),
            etterbetaling =
                getDate("etterbetaling_fom")?.let {
                    Etterbetaling(
                        fom = getDate("etterbetaling_fom").toLocalDate().let { YearMonth.from(it) },
                        tom = getDate("etterbetaling_tom").toLocalDate().let { YearMonth.from(it) },
                    )
                },
            aldersgruppe = getString("aldersgruppe")?.let { Aldersgruppe.valueOf(it) },
            kilde = getString("kilde").let { objectMapper.readValue(it) },
        )
}
