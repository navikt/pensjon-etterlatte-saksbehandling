package no.nav.etterlatte.vilkaarsvurdering

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import no.nav.etterlatte.libs.common.behandling.Revurderingaarsak
import no.nav.etterlatte.libs.common.rapidsandrivers.EVENT_NAME_KEY
import no.nav.etterlatte.libs.common.rapidsandrivers.lagParMedEventNameKey
import no.nav.etterlatte.rapidsandrivers.HENDELSE_DATA_KEY
import no.nav.etterlatte.rapidsandrivers.OmregningHendelseType
import no.nav.etterlatte.rapidsandrivers.Omregningshendelse
import no.nav.etterlatte.rapidsandrivers.SAK_ID_KEY
import no.nav.etterlatte.vilkaarsvurdering.services.VilkaarsvurderingService
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

internal class VilkaarsvurderingRiverTest {
    private val vilkaarsvurderingServiceMock =
        mockk<VilkaarsvurderingService> {
            coEvery { kopierForrigeVilkaarsvurdering(any(), any()) } returns mockk()
        }
    private val testRapid = TestRapid().apply { VilkaarsvurderingRiver(this, vilkaarsvurderingServiceMock) }

    @Test
    fun `tar opp VILKAARSVURDER-event, kopierer vilkaarsvurdering og poster ny BEREGN-meldig på koen`() {
        val behandlingId = UUID.randomUUID()
        val behandlingViOmregnerFra = UUID.randomUUID()
        val omregningshendelse =
            Omregningshendelse(
                1L,
                LocalDate.now(),
                Revurderingaarsak.REGULERING,
                behandlingId = behandlingId,
                forrigeBehandlingId = behandlingViOmregnerFra,
            )

        val melding =
            JsonMessage
                .newMessage(
                    mapOf(
                        OmregningHendelseType.BEHANDLING_OPPRETTA.lagParMedEventNameKey(),
                        SAK_ID_KEY to 1,
                        HENDELSE_DATA_KEY to omregningshendelse.toPacket(),
                    ),
                ).toJson()
        testRapid.sendTestMessage(melding)

        coVerify(exactly = 1) {
            vilkaarsvurderingServiceMock.kopierForrigeVilkaarsvurdering(
                behandlingId,
                behandlingViOmregnerFra,
            )
        }
        with(testRapid.inspektør.message(0)) {
            Assertions.assertEquals(
                OmregningHendelseType.VILKAARSVURDERT.lagEventnameForType(),
                this[EVENT_NAME_KEY].asText(),
            )
        }
    }
}
