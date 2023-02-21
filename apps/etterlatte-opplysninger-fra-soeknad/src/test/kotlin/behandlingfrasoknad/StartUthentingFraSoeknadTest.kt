package no.nav.etterlatte.behandlingfrasoknad

import com.fasterxml.jackson.module.kotlin.treeToValue
import io.mockk.every
import io.mockk.mockk
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.SoeknadType
import no.nav.etterlatte.opplysningerfrasoknad.Opplysningsuthenter
import no.nav.etterlatte.opplysningerfrasoknad.StartUthentingFraSoeknad
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.io.FileNotFoundException

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class StartUthentingFraSoeknadTest {

    companion object {
        val melding = readFile("/melding.json")
        val opplysningsuthenterMock = mockk<Opplysningsuthenter>()

        fun readFile(file: String) = Companion::class.java.getResource(file)?.readText()
            ?: throw FileNotFoundException("Fant ikke filen $file")
    }

    private val inspector = TestRapid().apply { StartUthentingFraSoeknad(this, opplysningsuthenterMock) }

    @Test
    fun `skal lese inn melding og lage message med opplysninger`() {
        val opplysninger = Opplysningsuthenter().lagOpplysningsListe(
            objectMapper.treeToValue(
                objectMapper.readTree(
                    javaClass.getResource("/melding.json")!!.readText()
                )!!["@skjema_info"]
            ),
            SoeknadType.BARNEPENSJON
        )
        every { opplysningsuthenterMock.lagOpplysningsListe(any(), any()) } returns opplysninger
        val inspector = inspector.apply { sendTestMessage(melding) }.inspektør

        assertEquals(1, inspector.message(0).get("sakId").intValue())
        assertEquals("f525f2f7-e246-43d7-b61a-5f0757472916", inspector.message(0).get("behandlingId").asText())
        assertEquals(true, inspector.message(0).get("gyldigInnsender").asBoolean())
        assertEquals(12, inspector.message(0).get("opplysning").size())
    }
}