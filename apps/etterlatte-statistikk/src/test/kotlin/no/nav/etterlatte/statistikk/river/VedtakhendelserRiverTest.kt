package no.nav.etterlatte.statistikk.river

import io.mockk.every
import io.mockk.mockk
import no.nav.etterlatte.behandling.randomSakId
import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.rapidsandrivers.EVENT_NAME_KEY
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.statistikk.domain.SakUtland
import no.nav.etterlatte.statistikk.domain.SakYtelsesgruppe
import no.nav.etterlatte.statistikk.domain.StoenadRad
import no.nav.etterlatte.statistikk.service.StatistikkService
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.io.FileNotFoundException
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class VedtakhendelserRiverTest {
//    companion object {
//        val melding = readFile("/melding.json")
//
//        private fun readFile(file: String) =
//            Companion::class.java.getResource(file)?.readText()
//                ?: throw FileNotFoundException(
//                    "Fant ikke filen $file for kjøring av test ${Companion::class.java.canonicalName}",
//                )
//    }
//
//    private val statistikkService: StatistikkService = mockk()
//
//    private val testRapid: TestRapid = TestRapid().apply { VedtakhendelserRiver(this, statistikkService) }
//
//    private val mockStoenadRad =
//        StoenadRad(
//            id = 0,
//            fnrSoeker = "",
//            fnrForeldre = emptyList(),
//            fnrSoesken = emptyList(),
//            anvendtTrygdetid = "",
//            nettoYtelse = "",
//            beregningType = "",
//            anvendtSats = "",
//            behandlingId = UUID.randomUUID(),
//            sakId = randomSakId(),
//            sakNummer = 0,
//            tekniskTid = Tidspunkt.now(),
//            sakYtelse = "",
//            versjon = "",
//            saksbehandler = "",
//            attestant = "",
//            vedtakLoependeFom = LocalDate.now(),
//            vedtakLoependeTom = null,
//            beregning = null,
//            avkorting = null,
//            vedtakType = null,
//            sakUtland = SakUtland.NASJONAL,
//            virkningstidspunkt = YearMonth.of(2023, 6),
//            utbetalingsdato = LocalDate.of(2023, 7, 20),
//            vedtaksloesning = Vedtaksloesning.GJENNY,
//            pesysId = 123L,
//            sakYtelsesgruppe = SakYtelsesgruppe.EN_AVDOED_FORELDER,
//            opphoerFom = null,
//            vedtaksperioder = null,
//        )
//
//    @Test
//    fun `Når statistikk blir registrert sendes en kvittering ut på river`() {
//        every {
//            statistikkService.registrerStatistikkForVedtak(any(), any(), any())
//        } returns (null to mockStoenadRad)
//        val inspector = testRapid.apply { sendTestMessage(melding) }.inspektør
//
//        Assertions.assertEquals(StatistikkhendelseType.REGISTRERT.lagEventnameForType(), inspector.message(0).get(EVENT_NAME_KEY).asText())
//        Assertions.assertEquals(
//            mockStoenadRad.toJson(),
//            inspector.message(0).get("stoenad_rad").toString(),
//        )
//    }
//
//    @Test
//    fun `Hvis ingen statistikk blir registrert sendes det ikke ut en kvittering`() {
//        every {
//            statistikkService.registrerStatistikkForVedtak(any(), any(), any())
//        } returns (null to null)
//        val inspector = testRapid.apply { sendTestMessage(melding) }.inspektør
//
//        Assertions.assertEquals(inspector.size, 0)
//    }
}
