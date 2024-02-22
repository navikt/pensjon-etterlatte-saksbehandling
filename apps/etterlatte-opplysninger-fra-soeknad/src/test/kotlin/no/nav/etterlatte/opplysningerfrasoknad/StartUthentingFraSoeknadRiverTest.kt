package no.nav.etterlatte.opplysningerfrasoknad

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.treeToValue
import io.mockk.every
import io.mockk.mockk
import no.nav.etterlatte.libs.common.innsendtsoeknad.common.SoeknadType
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.opplysningerfrasoknad.opplysningsuthenter.Opplysningsuthenter
import no.nav.etterlatte.rapidsandrivers.BEHANDLING_ID_KEY
import no.nav.etterlatte.rapidsandrivers.OPPLYSNING_KEY
import no.nav.etterlatte.rapidsandrivers.SAK_ID_KEY
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.io.FileNotFoundException

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class StartUthentingFraSoeknadRiverTest {
    companion object {
        val melding = readFile("/melding.json")
        val opplysningsuthenterMock = mockk<Opplysningsuthenter>()

        fun readFile(file: String) =
            Companion::class.java.getResource(file)?.readText()
                ?: throw FileNotFoundException("Fant ikke filen $file")
    }

    private val inspector = TestRapid().apply { StartUthentingFraSoeknadRiver(this, opplysningsuthenterMock) }

    @Test
    fun `skal lese inn melding og lage message med opplysninger`() {
        val soknad: JsonNode =
            objectMapper.treeToValue(
                objectMapper.readTree(
                    javaClass.getResource("/melding.json")!!.readText(),
                )!!["@skjema_info"],
            )
        val opplysninger =
            Opplysningsuthenter().lagOpplysningsListe(
                soknad,
                SoeknadType.BARNEPENSJON,
            )
        every { opplysningsuthenterMock.lagOpplysningsListe(soknad, SoeknadType.BARNEPENSJON) } returns opplysninger
        val inspector = inspector.apply { sendTestMessage(melding) }.inspektør

        assertEquals(1, inspector.message(0).get(SAK_ID_KEY).intValue())
        assertEquals("f525f2f7-e246-43d7-b61a-5f0757472916", inspector.message(0).get(BEHANDLING_ID_KEY).asText())
        assertEquals(11, inspector.message(0).get(OPPLYSNING_KEY).size())
    }
}
