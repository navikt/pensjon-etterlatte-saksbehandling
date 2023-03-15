package no.nav.etterlatte.statistikk.service

import io.mockk.every
import io.mockk.mockk
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.statistikk.database.SoeknadStatistikkRepository
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class SoeknadStatistikkServiceImplTest {

    @Test
    fun `registrer soeknadstatistikk registrerer gyldig for behandling riktig`() {
        val soeknadStatistikkRepository: SoeknadStatistikkRepository = mockk()

        every {
            soeknadStatistikkRepository.lagreNedSoeknadStatistikk(any())
        } returnsArgument 0

        val sut = SoeknadStatistikkServiceImpl(soeknadStatistikkRepository)
        val soeknadId = 1337L
        val gyldigForBehandling = true
        val feilendeKriterier = null
        val registrert = sut.registrerSoeknadStatistikk(
            soeknadId = soeknadId,
            gyldigForBehandling = gyldigForBehandling,
            sakType = SakType.BARNEPENSJON,
            feilendeKriterier = feilendeKriterier
        )

        Assertions.assertEquals(registrert.soeknadId, soeknadId)
        Assertions.assertEquals(registrert.gyldigForBehandling, gyldigForBehandling)
        Assertions.assertEquals(registrert.kriterierForIngenBehandling, emptyList<String>())
    }

    @Test
    fun `registrer soeknadstatistikk registrerer ikke gyldig for behandling riktig`() {
        val soeknadStatistikkRepository: SoeknadStatistikkRepository = mockk()

        every {
            soeknadStatistikkRepository.lagreNedSoeknadStatistikk(any())
        } returnsArgument 0

        val sut = SoeknadStatistikkServiceImpl(soeknadStatistikkRepository)
        val soeknadId = 1337L
        val gyldigForBehandling = false
        val feilendeKriterier = listOf("SOEKER_HAR_UTENLANDSADRESSE")
        val registrert = sut.registrerSoeknadStatistikk(
            soeknadId = soeknadId,
            gyldigForBehandling = gyldigForBehandling,
            sakType = SakType.BARNEPENSJON,
            feilendeKriterier = feilendeKriterier
        )

        Assertions.assertEquals(registrert.soeknadId, soeknadId)
        Assertions.assertEquals(registrert.gyldigForBehandling, gyldigForBehandling)
        Assertions.assertEquals(registrert.kriterierForIngenBehandling, feilendeKriterier)
    }
}