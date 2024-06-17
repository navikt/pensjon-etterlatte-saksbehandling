package no.nav.etterlatte.statistikk.river

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.etterlatte.libs.common.behandling.BEHANDLING_ID_PAA_VENT_RIVER_KEY
import no.nav.etterlatte.libs.common.behandling.BehandlingHendelseType
import no.nav.etterlatte.libs.common.behandling.PAA_VENT_AARSAK_KEY
import no.nav.etterlatte.libs.common.behandling.PaaVentAarsak
import no.nav.etterlatte.libs.common.rapidsandrivers.CORRELATION_ID_KEY
import no.nav.etterlatte.libs.common.rapidsandrivers.TEKNISK_TID_KEY
import no.nav.etterlatte.libs.common.rapidsandrivers.lagParMedEventNameKey
import no.nav.etterlatte.statistikk.service.StatistikkService
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.time.LocalDateTime
import java.util.UUID

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class BehandlingPaaVentHendelseRiverTest {
    private val service: StatistikkService =
        mockk {
            every { registrerStatistikkBehandlingPaaVentHendelse(any(), any(), any(), any()) } returns null
        }

    private val testRapid: TestRapid =
        TestRapid().apply {
            BehandlingPaaVentHendelseRiver(this, service)
        }

    @Test
    fun `skal ta i mot hendelse om paa vent`() {
        val behandlingId = UUID.randomUUID()
        val tekniskTid = LocalDateTime.now()
        val message =
            JsonMessage
                .newMessage(
                    mapOf(
                        BehandlingHendelseType.PAA_VENT.lagParMedEventNameKey(),
                        CORRELATION_ID_KEY to UUID.randomUUID(),
                        TEKNISK_TID_KEY to tekniskTid,
                        BEHANDLING_ID_PAA_VENT_RIVER_KEY to behandlingId,
                        PAA_VENT_AARSAK_KEY to PaaVentAarsak.OPPLYSNING_FRA_ANDRE.name,
                    ),
                ).toJson()
        val inspector = testRapid.apply { sendTestMessage(message) }.inspektør
        Assertions.assertEquals(0, inspector.size)

        verify {
            service.registrerStatistikkBehandlingPaaVentHendelse(
                behandlingId,
                BehandlingHendelseType.PAA_VENT,
                tekniskTid,
                PaaVentAarsak.OPPLYSNING_FRA_ANDRE,
            )
        }
    }
}
