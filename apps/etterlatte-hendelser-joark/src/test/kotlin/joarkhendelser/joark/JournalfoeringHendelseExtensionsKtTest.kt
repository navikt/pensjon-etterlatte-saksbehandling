package joarkhendelser.joark

import io.mockk.every
import io.mockk.mockk
import no.nav.etterlatte.joarkhendelser.joark.erTemaEtterlatte
import no.nav.etterlatte.joarkhendelser.joark.temaTilSakType
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.joarkjournalfoeringhendelser.JournalfoeringHendelseRecord
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

internal class JournalfoeringHendelseExtensionsKtTest {
    @ParameterizedTest
    @CsvSource(
        value = [
            "AAP, false",
            "DAG, false",
            "PEN, false",
            "MED, false",
            "FOR, false",
            "EYO, true",
            "EYB, true",
        ],
    )
    fun `Skal behandle EYB og EYO`(
        tema: String,
        skalBehandles: Boolean,
    ) {
        val hendelse =
            mockk<JournalfoeringHendelseRecord> {
                every { temaNytt } returns tema
            }

        assertEquals(skalBehandles, hendelse.erTemaEtterlatte())
    }

    @Test
    fun `Tema konverteres til SakType`() {
        val hendelse1 =
            mockk<JournalfoeringHendelseRecord> {
                every { temaNytt } returns "EYB"
            }
        assertEquals(hendelse1.temaTilSakType(), SakType.BARNEPENSJON)

        val hendelse2 =
            mockk<JournalfoeringHendelseRecord> {
                every { temaNytt } returns "EYO"
            }
        assertEquals(hendelse2.temaTilSakType(), SakType.OMSTILLINGSSTOENAD)

        val hendelse3 =
            mockk<JournalfoeringHendelseRecord> {
                every { temaNytt } returns "UKJENT TEMA"
            }

        assertThrows<Exception> {
            hendelse3.temaTilSakType()
        }
    }
}
