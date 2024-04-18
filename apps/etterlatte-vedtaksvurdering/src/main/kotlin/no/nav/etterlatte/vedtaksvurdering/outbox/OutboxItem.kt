package no.nav.etterlatte.vedtaksvurdering.outbox

import java.time.LocalDateTime
import java.util.UUID

data class OutboxItem(
    val id: UUID,
    val vedtakId: Long,
    val opprettet: LocalDateTime,
    val type: OutboxItemType,
    val publisert: Boolean,
)

enum class OutboxItemType {
    ATTESTERT,
}
