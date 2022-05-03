package no.nav.etterlatte.avstemming

import java.nio.ByteBuffer
import java.time.LocalDateTime
import java.util.*

data class Avstemming(
    val id: String = encodeUUIDBase64(UUID.randomUUID()),
    val opprettet: LocalDateTime = LocalDateTime.now(),
    val fraOgMed: LocalDateTime,
    val til: LocalDateTime,
    val antallAvstemteOppdrag: Int
) {
    companion object {
        private fun encodeUUIDBase64(uuid: UUID): String {
            val bb = ByteBuffer.wrap(ByteArray(16))
            bb.putLong(uuid.mostSignificantBits)
            bb.putLong(uuid.leastSignificantBits)
            return Base64.getUrlEncoder().encodeToString(bb.array()).substring(0, 22)
        }
    }
}