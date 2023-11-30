package no.nav.etterlatte.behandling.brevoppsett

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.database.singleOrNull
import no.nav.helse.rapids_rivers.toUUID
import java.sql.Connection
import java.sql.ResultSet
import java.time.YearMonth
import java.util.UUID

class BrevoppsettDao(private val connection: () -> Connection) {
    fun lagre(brevoppsett: Brevoppsett) {
        connection().prepareStatement(
            """
            INSERT INTO brevoppsett(
                behandling_id, etterbetaling_fom, etterbetaling_tom, brevtype, aldersgruppe, kilde
            )
            VALUES (?, ?, ?, ?, ?, ?)
            ON CONFLICT (behandling_id) DO 
            UPDATE SET 
                etterbetaling_fom = excluded.etterbetaling_fom, 
                etterbetaling_tom = excluded.etterbetaling_tom, 
                brevtype = excluded.brevtype, 
                aldersgruppe = excluded.aldersgruppe,
                kilde = excluded.kilde
            """.trimIndent(),
        )
            .apply {
                setObject(1, brevoppsett.behandlingId)
                setDate(2, brevoppsett.etterbetaling?.fom?.atDay(1).let { java.sql.Date.valueOf(it) })
                setDate(3, brevoppsett.etterbetaling?.tom?.atEndOfMonth().let { java.sql.Date.valueOf(it) })
                setString(4, brevoppsett.brevtype.name)
                setString(5, brevoppsett.aldersgruppe.name)
                setString(6, brevoppsett.kilde.toJson())
            }
            .run { executeUpdate() }
            .also { require(it == 1) }
    }

    fun hent(behandlingId: UUID): Brevoppsett? {
        return connection()
            .prepareStatement("SELECT fra_dato, til_dato FROM etterbetaling WHERE behandling_id = ?::UUID")
            .apply { setObject(1, behandlingId) }
            .run { executeQuery().singleOrNull { toBrevoppsett() } }
    }

    private fun ResultSet.toBrevoppsett() =
        Brevoppsett(
            behandlingId = getString("behandling_id").toUUID(),
            etterbetaling =
                getDate("etterbetaling_fom")?.let {
                    Etterbetaling(
                        fom = getDate("etterbetaling_fom").toLocalDate().let { YearMonth.from(it) },
                        tom = getDate("etterbetaling_tom").toLocalDate().let { YearMonth.from(it) },
                    )
                },
            brevtype = Brevtype.valueOf(getString("brevtype")),
            aldersgruppe = Aldersgruppe.valueOf(getString("aldergruppe")),
            kilde = getString("kilde").let { objectMapper.readValue(it) },
        )
}
