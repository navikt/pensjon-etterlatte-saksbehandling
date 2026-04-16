package no.nav.etterlatte.vedtaksvurdering.resend

import kotliquery.queryOf
import no.nav.etterlatte.libs.database.transaction
import java.util.UUID
import javax.sql.DataSource

class ResendTilUtbetalingRepository(
    private val datasource: DataSource,
) {
    fun hentUprosesserte(): List<UUID> =
        datasource.transaction { tx ->
            queryOf("SELECT behandling_id FROM resend_til_utbetaling WHERE prosessert = false ORDER BY opprettet")
                .let { query -> tx.run(query.map { row -> row.uuid("behandling_id") }.asList) }
        }

    fun merkSomProsessert(behandlingId: UUID) {
        datasource.transaction { tx ->
            queryOf(
                "UPDATE resend_til_utbetaling SET prosessert = true WHERE behandling_id = ?",
                behandlingId,
            ).let { query -> tx.run(query.asUpdate) }
        }
    }
}
