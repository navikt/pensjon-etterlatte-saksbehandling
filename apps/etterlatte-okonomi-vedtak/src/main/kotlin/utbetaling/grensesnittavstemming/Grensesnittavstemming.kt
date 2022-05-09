package no.nav.etterlatte.utbetaling.grensesnittavstemming

import no.nav.etterlatte.utbetaling.common.Tidspunkt
import java.nio.ByteBuffer
import java.util.*

data class Grensesnittavstemming(
    val id: String = encodeUUIDBase64(UUID.randomUUID()),
    val opprettet: Tidspunkt,
    val periodeFraOgMed: Tidspunkt,
    val periodeTil: Tidspunkt,
    val antallOppdrag: Int,
    val avstemmingsdata: String? = null,
) {
    companion object {
        // Kortere UUID for oppdrag
        private fun encodeUUIDBase64(uuid: UUID): String {
            val bb = ByteBuffer.wrap(ByteArray(16))
            bb.putLong(uuid.mostSignificantBits)
            bb.putLong(uuid.leastSignificantBits)
            return Base64.getUrlEncoder().encodeToString(bb.array()).substring(0, 22)
        }
    }
}