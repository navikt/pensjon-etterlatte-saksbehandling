package no.nav.etterlatte.gyldigsoeknad

import com.fasterxml.jackson.module.kotlin.jacksonTypeRef
import no.nav.etterlatte.libs.common.innsendtsoeknad.barnepensjon.Barnepensjon
import no.nav.etterlatte.libs.common.innsendtsoeknad.common.InnsendtSoeknad
import no.nav.etterlatte.libs.common.innsendtsoeknad.omstillingsstoenad.Omstillingsstoenad
import no.nav.etterlatte.libs.common.objectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class PersongalleriMapperTest {
    @Test
    fun `Barnepensjon - skal hente persongalleri fra søknad`() {
        val soeknad = getSoeknad("/barnepensjon.json") as Barnepensjon

        val persongalleri = PersongalleriMapper.hentPersongalleriFraSoeknad(soeknad)

        assertEquals("24111258054", persongalleri.soeker)
        assertEquals(listOf("01498344336"), persongalleri.gjenlevende)
        assertEquals("01498344336", persongalleri.innsender)
        assertTrue(persongalleri.soesken.isEmpty())
        assertEquals(listOf("08498224343"), persongalleri.avdoed)
    }

    @Test
    fun `Omstillingsstoenad - skal hente persongalleri fra søknad`() {
        val soeknad = getSoeknad("/omstillingsstoenad.json") as Omstillingsstoenad

        val persongalleri = PersongalleriMapper.hentPersongalleriFraSoeknad(soeknad)

        assertEquals("13848599411", persongalleri.soeker)
        assertTrue(persongalleri.gjenlevende.isEmpty())
        assertEquals("13848599411", persongalleri.innsender)
        assertEquals(listOf("19021370870"), persongalleri.soesken)
        assertEquals(listOf("03428317423"), persongalleri.avdoed)
    }

    private fun getSoeknad(file: String): InnsendtSoeknad {
        val json = javaClass.getResource(file)!!.readText()

        val soeknad =
            objectMapper.writeValueAsString(
                objectMapper.readTree(json).get("@skjema_info"),
            )

        return objectMapper.readValue(soeknad, jacksonTypeRef<InnsendtSoeknad>())
    }
}
