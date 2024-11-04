package no.nav.etterlatte.tidspunkt
import no.nav.etterlatte.libs.common.dbutils.norskTidssone
import no.nav.etterlatte.libs.common.dbutils.toLocalDatetimeNorskTid
import no.nav.etterlatte.libs.common.dbutils.toLocalDatetimeUTC
import no.nav.etterlatte.libs.common.dbutils.toNorskTid
import no.nav.etterlatte.libs.common.dbutils.toNorskTidspunkt
import no.nav.etterlatte.libs.common.dbutils.toTidspunkt
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.Month

class MidlertidigeKonverteringsfunksjonerTest {
    @Test
    fun `konvertering mellom LocalDateTime og Tidspunkt fram og tilbake gir samme resultat`() {
        val localDateTime = LocalDateTime.of(2023, Month.MARCH, 13, 12, 0, 0)
        val konvertertFramOgTilbake = localDateTime.toTidspunkt().toLocalDatetimeUTC()
        assertEquals(localDateTime, konvertertFramOgTilbake)
    }

    @Test
    fun `konvertering mellom LocalDateTime og Tidspunkt norsk tid fram og tilbake gir samme resultat`() {
        val localDateTime = LocalDateTime.of(2023, Month.APRIL, 20, 8, 1, 2)
        val konvertertFramOgTilbake = localDateTime.toNorskTidspunkt().toLocalDatetimeNorskTid()
        assertEquals(localDateTime, konvertertFramOgTilbake)
    }

    @Test
    fun `konvertering til ZonedDateTime og LocalDateTime norsk tid gir samme tid`() {
        val localDateTime = LocalDateTime.of(2023, Month.MAY, 30, 21, 10, 20)
        val tidspunkt = localDateTime.toTidspunkt()
        val norskTidZonedDateTime = tidspunkt.toNorskTid()
        val norskTidLocalDateTime = tidspunkt.toLocalDatetimeNorskTid()
        assertEquals(norskTidLocalDateTime, norskTidZonedDateTime.toLocalDateTime())
        assertEquals(norskTidLocalDateTime.atZone(norskTidssone), norskTidZonedDateTime)
    }
}
