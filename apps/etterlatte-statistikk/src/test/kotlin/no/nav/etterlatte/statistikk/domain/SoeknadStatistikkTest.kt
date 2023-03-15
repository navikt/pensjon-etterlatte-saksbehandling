package no.nav.etterlatte.statistikk.domain

import no.nav.etterlatte.libs.common.behandling.SakType
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows

class SoeknadStatistikkTest {

    @Test
    fun `SoeknadStatistikk kan ikke lages hvis den er inkonsekvent mellom gyldig for behandling og kriterier`() {
        assertThrows<Exception> {
            SoeknadStatistikk(
                soeknadId = 1L,
                gyldigForBehandling = false,
                sakType = SakType.BARNEPENSJON,
                kriterierForIngenBehandling = listOf()
            )
        }
        assertThrows<Exception> {
            SoeknadStatistikk(
                soeknadId = 1L,
                gyldigForBehandling = true,
                sakType = SakType.BARNEPENSJON,
                kriterierForIngenBehandling = listOf("SOEKER_HAR_UTENLANDSADRESSE")
            )
        }
        assertDoesNotThrow {
            SoeknadStatistikk(
                soeknadId = 0,
                gyldigForBehandling = true,
                sakType = SakType.BARNEPENSJON,
                kriterierForIngenBehandling = listOf()
            )
        }
        assertDoesNotThrow {
            SoeknadStatistikk(
                soeknadId = 0,
                gyldigForBehandling = false,
                sakType = SakType.BARNEPENSJON,
                kriterierForIngenBehandling = listOf("SOEKER_HAR_UTENLANDSADRESSE", "SOEKER_HAR_VERGE")
            )
        }
    }
}