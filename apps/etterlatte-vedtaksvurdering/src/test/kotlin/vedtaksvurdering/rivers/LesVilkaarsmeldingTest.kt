package vedtaksvurdering.rivers

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import no.nav.etterlatte.VedtaksvurderingService
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.vilkaarsvurdering.Vilkaarsvurdering
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingUtfall
import no.nav.etterlatte.vedtaksvurdering.rivers.LagreVilkaarsresultat
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import vilkaarsvurdering.VilkaarsvurderingTestData
import java.io.FileNotFoundException

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class LesVilkaarsmeldingTest {

    companion object {
        val melding = readFile("/vilkarsvurderingsmelding.json")
        private val vedtaksvurderingServiceMock = mockk<VedtaksvurderingService>()
        fun readFile(file: String) = Companion::class.java.getResource(file)?.readText()
            ?: throw FileNotFoundException("Fant ikke filen $file")
    }

    private val inspector = TestRapid().apply { LagreVilkaarsresultat(this, vedtaksvurderingServiceMock) }

    @Test
    fun `skal lese melding`() {
        println(objectMapper.writeValueAsString(VilkaarsvurderingTestData.oppfylt))
        val vilkarsres = slot<Vilkaarsvurdering>()
        every {
            vedtaksvurderingServiceMock.lagreVilkaarsresultat(
                any(),
                any(),
                any(),
                any(),
                capture(vilkarsres),
                any()
            )
        } returns Unit
        inspector.apply { sendTestMessage(melding) }.inspektør
        Assertions.assertEquals(VilkaarsvurderingUtfall.OPPFYLT, vilkarsres.captured.resultat.utfall)
    }
}