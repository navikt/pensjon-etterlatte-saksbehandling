import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import no.nav.etterlatte.libs.common.behandling.Behandlingsopplysning
import no.nav.etterlatte.libs.common.behandling.opplysningstyper.Opplysningstyper
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import vilkaar.HendelseNyttGrunnlag
import vilkaar.VurderVilkaar
import java.util.UUID

internal class BehovBesvartTest{


    @Test
    fun skalLeseBesvartBehov(){
        val behandling = UUID.randomUUID()
        val opplysningsType = Opplysningstyper.AVDOED_PDL_V1
        val vurderVilkaar: VurderVilkaar = mockk()
        val nyttGrunnlagHendelse = slot<HendelseNyttGrunnlag>()
        every { vurderVilkaar.handleHendelse(capture(nyttGrunnlagHendelse)) } returns emptyList()
        every { vurderVilkaar.interessantGrunnlag } returns listOf(opplysningsType)

        TestRapid().also {
            BehovBesvart(it, vurderVilkaar)
            it.sendTestMessage("""{
  "@behov": "$opplysningsType",
  "behandling": "$behandling",
  "opplysning": {
    "id": "9a3b5bf4-1bca-48dd-b846-8358bd994900",
    "kilde": {
      "navn": "pdl",
      "tidspunktForInnhenting": "2022-04-12T07:19:22.323885377Z",
      "registersReferanse": null,
      "type": "pdl"
    },
    "opplysningType": "AVDOED_PDL_V1",
    "meta": {},
    "opplysning": {"assertkey":"assertvalue"},
    "attestering": null
  }
}""")
        }
        assertTrue(nyttGrunnlagHendelse.isCaptured)
        nyttGrunnlagHendelse.captured.also { hendelse->
            assertEquals(behandling, hendelse.behandling)
            hendelse.opplysninger.first().also {
                assertEquals(opplysningsType, it.opplysningType)
                assertEquals("pdl", (it.kilde as Behandlingsopplysning.Pdl).type)
                assertEquals("assertvalue", it.opplysning["assertkey"].textValue())
            }
        }
    }


}