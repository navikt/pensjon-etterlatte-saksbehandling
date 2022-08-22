package no.nav.etterlatte.utbetaling.grensesnittavstemming

import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import java.nio.ByteBuffer
import java.util.*

data class UUIDBase64(val value: String = encodeUUIDBase64(UUID.randomUUID())) {
    companion object {
        private fun encodeUUIDBase64(uuid: UUID): String {
            val bb = ByteBuffer.wrap(ByteArray(16))
            bb.putLong(uuid.mostSignificantBits)
            bb.putLong(uuid.leastSignificantBits)
            return Base64.getUrlEncoder().encodeToString(bb.array()).substring(0, 22)
        }
    }
}

data class Avstemmingsperiode(
    val fraOgMed: Tidspunkt,
    val til: Tidspunkt
) {
    init {
        require(fraOgMed.instant.isBefore(til.instant)) { "fraOgMed-tidspunkt maa vaere foer til-tidspunkt" }
    }
}

data class Grensesnittavstemming(
    val id: UUIDBase64 = UUIDBase64(),
    val opprettet: Tidspunkt,
    val periode: Avstemmingsperiode,
    val antallOppdrag: Int,
    val avstemmingsdata: String
)