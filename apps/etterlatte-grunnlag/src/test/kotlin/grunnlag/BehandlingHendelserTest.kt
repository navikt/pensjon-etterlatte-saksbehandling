package grunnlag

import io.mockk.every
import io.mockk.mockk
import no.nav.etterlatte.grunnlag.BehandlingHendelser
import no.nav.etterlatte.grunnlag.GrunnlagFactory
import no.nav.etterlatte.grunnlag.GrunnlagHendelser
import no.nav.etterlatte.grunnlag.OpplysningDao
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstyper
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.io.FileNotFoundException
import java.util.*

class BehandlingHendelserTest {
    companion object {
        val melding = readFile("/behandlingsmelding.json")
        val opplysningerMock = mockk<OpplysningDao>()
        fun readFile(file: String) = Companion::class.java.getResource(file)?.readText()
            ?: throw FileNotFoundException("Fant ikke filen $file")
    }


    private val inspector = TestRapid().apply { BehandlingHendelser(this, GrunnlagFactory(opplysningerMock)) }

    @Test
    fun `skal lese melding om behandling opprettet og lage opplysningsbehov`() {
        val opplysninger = listOf(
            Grunnlagsopplysning(
                UUID.randomUUID(),
                Grunnlagsopplysning.Saksbehandler("S01"),
                Opplysningstyper.SOEKER_SOEKNAD_V1,
                objectMapper.createObjectNode(),
                objectMapper.createObjectNode()
            ),
            Grunnlagsopplysning(
                UUID.randomUUID(),
                Grunnlagsopplysning.Saksbehandler("S01"),
                Opplysningstyper.AVDOED_SOEKNAD_V1,
                objectMapper.createObjectNode(),
                objectMapper.createObjectNode()
            ),
        )

        every { opplysningerMock.finnOpplysningerIGrunnlag(any())} returns opplysninger
        every { opplysningerMock.leggOpplysningTilGrunnlag(any(),any())} returns Unit
        val inspector = inspector.apply { sendTestMessage(melding) }.inspektør

        Assertions.assertEquals(Opplysningstyper.SOEKER_PDL_V1.name, inspector.message(0).get("@behov").asText())
        Assertions.assertEquals(Opplysningstyper.GJENLEVENDE_FORELDER_PDL_V1.name, inspector.message(1).get("@behov").asText())
        Assertions.assertEquals(4, inspector.size)
    }
}
