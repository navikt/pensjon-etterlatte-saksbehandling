package behandlingfrasoknad


import Behandling
import LesGyldigSoeknadsmelding
import io.mockk.every
import io.mockk.mockk
import model.GyldigSoeknadService
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.gyldigSoeknad.GyldighetsResultat
import no.nav.etterlatte.libs.common.gyldigSoeknad.GyldighetsTyper
import no.nav.etterlatte.libs.common.gyldigSoeknad.VurdertGyldighet
import no.nav.etterlatte.libs.common.vikaar.VurderingsResultat

import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.io.FileNotFoundException
import java.time.LocalDateTime
import java.util.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class LesVilkaarsmeldingTest {
    companion object {
        val melding = readFile("/fordeltmelding.json")
        val behandlingMock = mockk<Behandling>()
        val gyldigSoeknadServiceMock = mockk<GyldigSoeknadService>()

        fun readFile(file: String) = Companion::class.java.getResource(file)?.readText()
            ?: throw FileNotFoundException("Fant ikke filen $file")
    }

    private val inspector =
        TestRapid().apply { LesGyldigSoeknadsmelding(this, gyldigSoeknadServiceMock, behandlingMock) }

    @Test
    fun `skal lage nye messages for sakid, behandlingid, og gyldig framsatt`() {
        val persongalleri = Persongalleri(
            "soeker",
            "innsender",
        )

        val gyldighetsResultat = GyldighetsResultat(
            VurderingsResultat.OPPFYLT,
            listOf(
                VurdertGyldighet(
                    GyldighetsTyper.INNSENDER_ER_FORELDER,
                    VurderingsResultat.OPPFYLT,
                    "innsenderFnr"
                )
            ),
            LocalDateTime.now()
        )
        val id  = UUID.randomUUID()

        every { gyldigSoeknadServiceMock.hentPersongalleriFraSoeknad(any()) } returns persongalleri
        every { gyldigSoeknadServiceMock.vurderGyldighet(persongalleri) } returns gyldighetsResultat
        every { behandlingMock.skaffSak(any(), any()) } returns 4
        every { behandlingMock.initierBehandling(any(), any(), persongalleri) } returns id
        every { behandlingMock.lagreGyldighetsVurdering(any(), any()) } returns Unit

        val inspector = inspector.apply { sendTestMessage(melding) }.inspektør

        Assertions.assertEquals("ey_fordelt", inspector.message(0).get("@event_name").asText())
        Assertions.assertEquals(4, inspector.message(0).get("@sak_id").longValue())
        Assertions.assertEquals(id.toString(), inspector.message(0).get("@behandling_id").asText())
        Assertions.assertEquals(true, inspector.message(0).get("@gyldig_innsender").asBoolean())

    }
}