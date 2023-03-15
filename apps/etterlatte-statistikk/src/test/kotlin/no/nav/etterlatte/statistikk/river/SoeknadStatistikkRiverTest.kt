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
import java.util.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SoeknadStatistikkRiverTest {

    private val soeknadStatistikkService: SoeknadStatistikkService = mockk()

    private val testRapid: TestRapid = TestRapid().apply {
        SoeknadStatistikkRiver(this, soeknadStatistikkService)
    }

    @Test
    fun `melding om soeknadStatistikk leses ut og håndteres`() {
        val soeknadStatistikk: SoeknadStatistikk = mockk()
        every {
            soeknadStatistikkService.registrerSoeknadStatistikk(
                soeknadId = any(),
                gyldigForBehandling = any(),
                sakType = any(),
                feilendeKriterier = any()
            )
        } returns soeknadStatistikk
        val message = JsonMessage.newMessage(
            mapOf(
                EVENT_NAME_KEY to EventNames.FORDELER_STATISTIKK,
                CORRELATION_ID_KEY to UUID.randomUUID(),
                SOEKNAD_ID_KEY to 1337L,
                SAK_TYPE_KEY to SakType.BARNEPENSJON.toString(),
                GYLDIG_FOR_BEHANDLING_KEY to false,
                FEILENDE_KRITERIER_KEY to listOf("SØKNADEN ER HJEMSØKT")
            )
        ).toJson()

        val inspector = testRapid.apply { sendTestMessage(message) }.inspektør

        Assertions.assertEquals(0, inspector.size) // Skal ikke sende ut noen nye kafka-meldinger
        verify(exactly = 1) { soeknadStatistikkService.registrerSoeknadStatistikk(any(), any(), any(), any()) }
    }
}