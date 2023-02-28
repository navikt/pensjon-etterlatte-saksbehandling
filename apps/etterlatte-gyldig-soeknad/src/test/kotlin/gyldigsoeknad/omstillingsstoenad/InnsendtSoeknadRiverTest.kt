package gyldigsoeknad.omstillingsstoenad

import io.mockk.every
import io.mockk.mockk
import no.nav.etterlatte.gyldigsoeknad.client.BehandlingClient
import no.nav.etterlatte.gyldigsoeknad.omstillingsstoenad.GyldigOmstillingsSoeknadService
import no.nav.etterlatte.gyldigsoeknad.omstillingsstoenad.InnsendtSoeknadRiver
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.event.GyldigSoeknadVurdert
import no.nav.etterlatte.libs.common.gyldigSoeknad.GyldighetsResultat
import no.nav.etterlatte.libs.common.gyldigSoeknad.VurderingsResultat
import no.nav.etterlatte.libs.common.rapidsandrivers.EVENT_NAME_KEY
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.io.FileNotFoundException
import java.time.LocalDateTime
import java.util.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class InnsendtSoeknadRiverTest {

    private val behandlingClientMock = mockk<BehandlingClient>()
    private val gyldigOmstillingsSoeknadServiceMock = mockk<GyldigOmstillingsSoeknadService>()
    private val inspector = TestRapid().apply {
        InnsendtSoeknadRiver(this, gyldigOmstillingsSoeknadServiceMock, behandlingClientMock)
    }

    @Test
    fun `skal sjekke om soknad om omstillingsstonad er gyldig fremsatt og returnere resultatet av dette`() {
        val persongalleri = Persongalleri(
            "soeker",
            "innsender"
        )

        val gyldighetsResultat = GyldighetsResultat(VurderingsResultat.OPPFYLT, listOf(), LocalDateTime.now())
        val id = UUID.randomUUID()

        every { gyldigOmstillingsSoeknadServiceMock.hentPersongalleriFraSoeknad(any()) } returns persongalleri
        every {
            gyldigOmstillingsSoeknadServiceMock.vurderGyldighet(
                persongalleri.innsender,
                persongalleri.avdoed
            )
        } returns gyldighetsResultat
        every { behandlingClientMock.skaffSak(any(), any()) } returns 4
        every { behandlingClientMock.initierBehandling(any(), any(), persongalleri) } returns id
        every { behandlingClientMock.lagreGyldighetsVurdering(any(), any()) } returns Unit

        val inspector = inspector.apply { sendTestMessage(melding) }.inspektør

        assertEquals(GyldigSoeknadVurdert.eventName, inspector.first().get(EVENT_NAME_KEY).asText())
        assertEquals(4, inspector.first().get(GyldigSoeknadVurdert.sakIdKey).longValue())
        assertEquals(id.toString(), inspector.first().get(GyldigSoeknadVurdert.behandlingIdKey).asText())
        assertEquals(true, inspector.first().get(GyldigSoeknadVurdert.gyldigInnsenderKey).asBoolean())
    }

    private fun TestRapid.RapidInspector.first() = this.message(0)

    companion object {
        private val melding = readFile("/innsendtsoeknad.json")

        fun readFile(file: String) = Companion::class.java.getResource(file)?.readText()
            ?: throw FileNotFoundException("Fant ikke filen $file")
    }
}