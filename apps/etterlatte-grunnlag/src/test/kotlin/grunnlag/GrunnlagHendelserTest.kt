package grunnlag

import io.mockk.every
import io.mockk.mockk
import no.nav.etterlatte.EnvBasedBeanFactory
import no.nav.etterlatte.grunnlag.GrunnlagFactory
import no.nav.etterlatte.grunnlag.GrunnlagHendelser
import no.nav.etterlatte.grunnlag.OpplysningDao
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstyper
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.soeknad.dataklasser.Barnepensjon
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.io.FileNotFoundException
import java.util.*

class GrunnlagHendelserTest {
    companion object {
        val melding = readFile("/opplysningsmelding.json")
        //val beanFactory = EnvBasedBeanFactory(env)
        val opplysningerMock = mockk<OpplysningDao>()

       /* fun readmelding(file: String): Barnepensjon {
            val skjemaInfo = objectMapper.writeValueAsString(
                objectMapper.readTree(readFile(file)).get("@skjema_info")
            )
            return objectMapper.readValue(skjemaInfo, Barnepensjon::class.java)
        }

        */

        fun readFile(file: String) = Companion::class.java.getResource(file)?.readText()
            ?: throw FileNotFoundException("Fant ikke filen $file")
    }


    private val inspector = TestRapid().apply { GrunnlagHendelser(this, GrunnlagFactory(opplysningerMock)) }

    @Test
    fun `skal lese opplysningsbehov og legge til opplysning`() {
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

        every { opplysningerMock.finnOpplysningerIGrunnlag(4)} returns opplysninger
        val inspector = inspector.apply { sendTestMessage(melding) }.inspektør

        Assertions.assertEquals("BEHANDLING:GRUNNLAGENDRET", inspector.message(0).get("@event").asText())
        //TODO oppdatere testen
        println(inspector.message(0).get("@vilkaarsvurdering"))

        Assertions.assertEquals(3, inspector.message(0).get("@vilkaarsvurdering").size())

        //verify { fordelerMetricLogger.logMetricFordelt() }
    }
}
