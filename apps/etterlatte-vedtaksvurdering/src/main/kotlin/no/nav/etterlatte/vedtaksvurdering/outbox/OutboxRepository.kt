package no.nav.etterlatte.vedtaksvurdering.outbox

import kotliquery.Row
import kotliquery.queryOf
import no.nav.etterlatte.libs.database.transaction
import java.util.UUID
import javax.sql.DataSource

class OutboxRepository(
    private val datasource: DataSource,
) {
    fun hentUpubliserte(): List<OutboxItem> =
        datasource.transaction { tx ->
            queryOf(
                """
                SELECT * FROM outbox_vedtakshendelse 
                WHERE publisert = false
                ORDER BY opprettet
                """.trimIndent(),
            ).let { query -> tx.run(query.map { row -> row.toOutboxItem() }.asList) }
        }

    fun merkSomPublisert(id: UUID) {
        datasource.transaction { tx ->
            queryOf(
                """
                UPDATE outbox_vedtakshendelse
                SET publisert = true
                WHERE id = ?
                """.trimIndent(),
                id,
            ).let { query -> tx.run(query.asUpdate) }
                .also { require(it == 1) { "Fant ikke hendelse med id $id og status upublisert" } }
        }
    }

    private fun Row.toOutboxItem() =
        OutboxItem(
            id = uuid("id"),
            vedtakId = long("vedtakId"),
            opprettet = localDateTime("opprettet"),
            type = OutboxItemType.valueOf(string("type")),
            publisert = boolean("publisert"),
        )
}
