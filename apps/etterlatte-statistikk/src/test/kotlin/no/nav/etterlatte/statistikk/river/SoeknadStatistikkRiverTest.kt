package no.nav.etterlatte.statistikk.river

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.rapidsandrivers.CORRELATION_ID_KEY
import no.nav.etterlatte.libs.common.rapidsandrivers.EVENT_NAME_KEY
import no.nav.etterlatte.libs.common.rapidsandrivers.FEILENDE_KRITERIER_KEY
import no.nav.etterlatte.libs.common.rapidsandrivers.GYLDIG_FOR_BEHANDLING_KEY
import no.nav.etterlatte.libs.common.rapidsandrivers.SAK_TYPE_KEY
import no.nav.etterlatte.libs.common.rapidsandrivers.SOEKNAD_ID_KEY
import no.nav.etterlatte.rapidsandrivers.EventNames
import no.nav.etterlatte.statistikk.domain.SoeknadStatistikk
import no.nav.etterlatte.statistikk.service.SoeknadStatistikkService
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.util.UUID

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SoeknadStatistikkRiverTest {
    private val soeknadStatistikkService: SoeknadStatistikkService = mockk()

    private val testRapid: TestRapid =
        TestRapid().apply {
            SoeknadStatistikkRiver(this, soeknadStatistikkService)
        }

    @Test
    fun `melding om soeknadStatistikk for ugyldig for behandling leses ut og håndteres`() {
        val soeknadStatistikk: SoeknadStatistikk = mockk()
        val soeknadId = 1337L
        val gyldigForBehandling = false
        val sakType = SakType.BARNEPENSJON
        val feilendeKriterier = listOf("SØKNADEN ER HJEMSØKT")
        every {
            soeknadStatistikkService.registrerSoeknadStatistikk(
                soeknadId,
                gyldigForBehandling,
                sakType,
                feilendeKriterier,
            )
        } returns soeknadStatistikk

        val message =
            JsonMessage.newMessage(
                mapOf(
                    EVENT_NAME_KEY to EventNames.FORDELER_STATISTIKK,
                    CORRELATION_ID_KEY to UUID.randomUUID(),
                    SOEKNAD_ID_KEY to soeknadId,
                    SAK_TYPE_KEY to sakType,
                    GYLDIG_FOR_BEHANDLING_KEY to gyldigForBehandling,
                    FEILENDE_KRITERIER_KEY to feilendeKriterier,
                ),
            ).toJson()

        val inspector = testRapid.apply { sendTestMessage(message) }.inspektør

        Assertions.assertEquals(0, inspector.size) // Skal ikke sende ut noen nye kafka-meldinger
        verify(exactly = 1) {
            soeknadStatistikkService.registrerSoeknadStatistikk(
                soeknadId,
                gyldigForBehandling,
                sakType,
                feilendeKriterier,
            )
        }
    }

    @Test
    fun `melding om soeknadStatistikk for gyldig for behandling leses ut og håndteres`() {
        val soeknadStatistikk: SoeknadStatistikk = mockk()
        val soeknadId = 1337L
        val gyldigForBehandling = true
        val sakType = SakType.BARNEPENSJON
        val feilendeKriterier = null
        every {
            soeknadStatistikkService.registrerSoeknadStatistikk(
                soeknadId,
                gyldigForBehandling,
                sakType,
                feilendeKriterier,
            )
        } returns soeknadStatistikk

        val message =
            JsonMessage.newMessage(
                mapOf(
                    EVENT_NAME_KEY to EventNames.FORDELER_STATISTIKK,
                    CORRELATION_ID_KEY to UUID.randomUUID(),
                    SOEKNAD_ID_KEY to soeknadId,
                    SAK_TYPE_KEY to sakType,
                    GYLDIG_FOR_BEHANDLING_KEY to gyldigForBehandling,
                ),
            ).toJson()

        val inspector = testRapid.apply { sendTestMessage(message) }.inspektør

        Assertions.assertEquals(0, inspector.size) // Skal ikke sende ut noen nye kafka-meldinger
        verify(exactly = 1) {
            soeknadStatistikkService.registrerSoeknadStatistikk(
                soeknadId,
                gyldigForBehandling,
                sakType,
                feilendeKriterier,
            )
        }
    }
}
