package no.nav.etterlatte.vilkaarsvurdering

import com.fasterxml.jackson.module.kotlin.readValue
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.grunnlag.Grunnlag
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.vilkaarsvurdering.barnepensjon.barnepensjonVilkaar
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Test
import java.io.FileNotFoundException
import java.util.*

internal class GrunnlagEndretRiverTest {

    private val vilkaarsvurderingService: VilkaarsvurderingService = mockk()
    private val inspector = TestRapid().apply { GrunnlagEndretRiver(this, vilkaarsvurderingService) }

    @Test
    fun `skal motta grunnlagsendring og opprette ny vilkaarsvurdering med innholdet i meldingen`() {
        every { vilkaarsvurderingService.hentVilkaarsvurdering(any()) } returns null
        every {
            vilkaarsvurderingService.opprettVilkaarsvurdering(
                any(),
                SakType.BARNEPENSJON,
                BehandlingType.FØRSTEGANGSBEHANDLING,
                any(),
                any()
            )
        } returns eksisterendeVilkaarsvurdering()

        inspector.apply { sendTestMessage(grunnlagEndretMelding) }.inspektør

        verify(exactly = 1) { vilkaarsvurderingService.hentVilkaarsvurdering(any()) }
        verify(exactly = 1) {
            vilkaarsvurderingService.opprettVilkaarsvurdering(
                any(),
                SakType.BARNEPENSJON,
                BehandlingType.FØRSTEGANGSBEHANDLING,
                any(),
                any()
            )
        }
        confirmVerified(vilkaarsvurderingService)
    }

    @Test
    fun `skal motta grunnlagsendring og oppdatere eksisterende vilkaarsvurdering med innholdet i meldingen`() {
        every { vilkaarsvurderingService.hentVilkaarsvurdering(any()) } returns eksisterendeVilkaarsvurdering()
        every {
            vilkaarsvurderingService.oppdaterVilkaarsvurderingPayload(any(), any())
        } returns eksisterendeVilkaarsvurdering()

        inspector.apply { sendTestMessage(grunnlagEndretMelding) }.inspektør

        verify(exactly = 1) { vilkaarsvurderingService.hentVilkaarsvurdering(any()) }
        verify(exactly = 1) { vilkaarsvurderingService.oppdaterVilkaarsvurderingPayload(any(), any()) }
        confirmVerified(vilkaarsvurderingService)
    }

    private fun eksisterendeVilkaarsvurdering() =
        Vilkaarsvurdering(
            behandlingId = UUID.fromString("dbbd9a01-3e5d-4ec1-819c-1781d1f6a440"),
            kafkaPayload = grunnlagEndretMelding,
            vilkaar = barnepensjonVilkaar(grunnlag)
        )

    companion object {
        val grunnlagEndretMelding = readFile("/grunnlagEndret.json")
        val grunnlag: Grunnlag = objectMapper.readValue(readFile("/grunnlag.json"))

        private fun readFile(file: String) = Companion::class.java.getResource(file)?.readText()
            ?: throw FileNotFoundException("Fant ikke filen $file")
    }
}