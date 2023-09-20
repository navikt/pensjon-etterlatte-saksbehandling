package no.nav.etterlatte.gyldigsoeknad.barnepensjon

import io.mockk.every
import io.mockk.mockk
import no.nav.etterlatte.gyldigsoeknad.client.BehandlingClient
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.event.GyldigSoeknadVurdert
import no.nav.etterlatte.libs.common.event.SoeknadInnsendt
import no.nav.etterlatte.libs.common.gyldigSoeknad.GyldighetsResultat
import no.nav.etterlatte.libs.common.gyldigSoeknad.GyldighetsTyper
import no.nav.etterlatte.libs.common.gyldigSoeknad.VurderingsResultat
import no.nav.etterlatte.libs.common.gyldigSoeknad.VurdertGyldighet
import no.nav.etterlatte.libs.common.rapidsandrivers.EVENT_NAME_KEY
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.toLocalDatetimeUTC
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.io.FileNotFoundException
import java.util.UUID

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class FordeltSoeknadRiverTest {
    private val behandlingClientMock = mockk<BehandlingClient>()
    private val gyldigSoeknadServiceMock = mockk<GyldigSoeknadService>()
    private val inspector =
        TestRapid().apply {
            FordeltSoeknadRiver(this, gyldigSoeknadServiceMock, behandlingClientMock)
        }

    @Test
    fun `skal sjekke om søknad er gyldig fremsatt og returnere resultatet av dette`() {
        val persongalleri =
            Persongalleri(
                soeker = "soeker",
                innsender = "innsender",
                avdoed = listOf("avdoed"),
                gjenlevende = listOf("gjenlevende"),
            )

        val gyldighetsResultat =
            GyldighetsResultat(
                VurderingsResultat.OPPFYLT,
                listOf(
                    VurdertGyldighet(
                        GyldighetsTyper.INNSENDER_ER_FORELDER,
                        VurderingsResultat.OPPFYLT,
                        "innsenderFnr",
                    ),
                ),
                Tidspunkt.now().toLocalDatetimeUTC(),
            )
        val id = UUID.randomUUID()
        val sakId = 12345L

        every { gyldigSoeknadServiceMock.hentPersongalleriFraSoeknad(any()) } returns persongalleri
        every { gyldigSoeknadServiceMock.vurderGyldighet(persongalleri, any()) } returns gyldighetsResultat
        every {
            behandlingClientMock.finnEllerOpprettSak(any(), any())
        } returns Sak(persongalleri.soeker, SakType.BARNEPENSJON, sakId, "4808")
        every { behandlingClientMock.opprettBehandling(any(), any(), persongalleri) } returns id
        every { behandlingClientMock.lagreGyldighetsVurdering(any(), any()) } returns Unit

        val inspector = inspector.apply { sendTestMessage(melding) }.inspektør

        assertEquals(SoeknadInnsendt.eventNameBehandlingBehov, inspector.message(0).get(EVENT_NAME_KEY).asText())
        assertEquals(sakId, inspector.message(0).get(GyldigSoeknadVurdert.sakIdKey).longValue())
        assertEquals(id.toString(), inspector.message(0).get(GyldigSoeknadVurdert.behandlingIdKey).asText())

        assertEquals(1, inspector.size)
    }

    companion object {
        private val melding = readFile("/fordeltmelding.json")

        fun readFile(file: String) =
            Companion::class.java.getResource(file)?.readText()
                ?: throw FileNotFoundException("Fant ikke filen $file")
    }
}
