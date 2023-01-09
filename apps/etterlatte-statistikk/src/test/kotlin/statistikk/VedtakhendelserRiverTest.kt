package no.nav.etterlatte.statistikk

import io.mockk.every
import io.mockk.mockk
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.rapidsandrivers.eventNameKey
import no.nav.etterlatte.statistikk.database.StoenadRad
import no.nav.etterlatte.statistikk.river.VedtakhendelserRiver
import no.nav.etterlatte.statistikk.service.StatistikkService
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.io.FileNotFoundException
import java.time.Instant
import java.time.LocalDate
import java.util.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class VedtakhendelserRiverTest {

    companion object {
        val melding = readFile("/melding.json")

        fun readFile(file: String) = Companion::class.java.getResource(file)?.readText()
            ?: throw FileNotFoundException(
                "Fant ikke filen $file for kjøring av test ${Companion::class.java.canonicalName}"
            )
    }

    private val statistikkService: StatistikkService = mockk()

    private val testRapid: TestRapid = TestRapid().apply { VedtakhendelserRiver(this, statistikkService) }

    private val mockStoenadRad = StoenadRad(
        id = 0,
        fnrSoeker = "",
        fnrForeldre = listOf(),
        fnrSoesken = listOf(),
        anvendtTrygdetid = "",
        nettoYtelse = "",
        beregningType = "",
        anvendtSats = "",
        behandlingId = UUID.randomUUID(),
        sakId = 0,
        sakNummer = 0,
        tekniskTid = Instant.now(),
        sakYtelse = "",
        versjon = "",
        saksbehandler = "",
        attestant = "",
        vedtakLoependeFom = LocalDate.now(),
        vedtakLoependeTom = null
    )

    @Test
    fun `Når statistikk blir registrert sendes en kvittering ut på river`() {
        every {
            statistikkService.registrerStatistikkForVedtak(any(), any())
        } returns (null to mockStoenadRad)
        val inspector = testRapid.apply { sendTestMessage(melding) }.inspektør

        Assertions.assertEquals("STATISTIKK:REGISTRERT", inspector.message(0).get(eventNameKey).asText())
        Assertions.assertEquals(
            objectMapper.writeValueAsString(mockStoenadRad),
            inspector.message(0).get("stoenad_rad").asText()
        )
    }

    @Test
    fun `Hvis ingen statistikk blir registrert sendes det ikke ut en kvittering`() {
        every {
            statistikkService.registrerStatistikkForVedtak(any(), any())
        } returns (null to null)
        val inspector = testRapid.apply { sendTestMessage(melding) }.inspektør

        Assertions.assertEquals(inspector.size, 0)
    }
}