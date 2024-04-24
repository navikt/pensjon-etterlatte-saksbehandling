package no.nav.etterlatte.fordeler

import io.ktor.client.plugins.ResponseException
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.mockk
import no.nav.etterlatte.gyldigsoeknad.FordelerRiver
import no.nav.etterlatte.gyldigsoeknad.client.BehandlingClient
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.event.FordelerFordelt
import no.nav.etterlatte.libs.common.event.GyldigSoeknadVurdert
import no.nav.etterlatte.libs.common.event.SoeknadInnsendtHendelseType
import no.nav.etterlatte.libs.common.rapidsandrivers.EVENT_NAME_KEY
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import kotlin.random.Random

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class FordelerRiverTest {
    private val behandlingKlientMock = mockk<BehandlingClient>()

    @AfterEach
    fun afterEach() {
        confirmVerified(behandlingKlientMock)
        clearAllMocks()
    }

    @Test
    fun `BP - skal fordele gyldig soknad til behandling`() {
        val sakId = Random.nextLong()

        coEvery {
            behandlingKlientMock.finnEllerOpprettSak(any(), any())
        } returns Sak("-", SakType.BARNEPENSJON, sakId, "-")

        val inspector =
            testRapid {
                sendTestMessage(BARNEPENSJON_SOEKNAD)
            }

        assertEquals(
            SoeknadInnsendtHendelseType.EVENT_NAME_INNSENDT.lagEventnameForType(),
            inspector.message(0).get(EVENT_NAME_KEY).asText(),
        )
        assertEquals(sakId, inspector.message(0).get(GyldigSoeknadVurdert.sakIdKey).longValue())
        assertEquals("true", inspector.message(0).get(FordelerFordelt.soeknadFordeltKey).asText())

        coVerify { behandlingKlientMock.finnEllerOpprettSak("25478323363", SakType.BARNEPENSJON) }
    }

    @Test
    fun `BP - skal ikke fordele soknad uten sakId til behandling`() {
        coEvery { behandlingKlientMock.finnEllerOpprettSak(any(), any()) } throws
            ResponseException(
                mockk(),
                "Ukjent feil",
            )

        val inspector =
            testRapid {
                sendTestMessage(BARNEPENSJON_SOEKNAD)
            }

        assertEquals(0, inspector.size)

        coVerify { behandlingKlientMock.finnEllerOpprettSak("25478323363", SakType.BARNEPENSJON) }
    }

    @Test
    fun `OMS - skal fordele gyldig soknad til behandling`() {
        val sakId = Random.nextLong()

        coEvery {
            behandlingKlientMock.finnEllerOpprettSak(any(), any())
        } returns Sak("-", SakType.BARNEPENSJON, sakId, "-")

        val inspector =
            testRapid {
                sendTestMessage(OMSTILLINGSSTOENAD_SOEKNAD)
            }

        assertEquals(
            SoeknadInnsendtHendelseType.EVENT_NAME_INNSENDT.lagEventnameForType(),
            inspector.message(0).get(EVENT_NAME_KEY).asText(),
        )
        assertEquals(sakId, inspector.message(0).get(GyldigSoeknadVurdert.sakIdKey).longValue())
        assertEquals("true", inspector.message(0).get(FordelerFordelt.soeknadFordeltKey).asText())

        coVerify { behandlingKlientMock.finnEllerOpprettSak("13848599411", SakType.OMSTILLINGSSTOENAD) }
    }

    @Test
    fun `OMS - skal ikke fordele soknad uten sakId til behandling`() {
        coEvery { behandlingKlientMock.finnEllerOpprettSak(any(), any()) } throws
            ResponseException(
                mockk(),
                "Ukjent feil",
            )

        val inspector =
            testRapid {
                sendTestMessage(OMSTILLINGSSTOENAD_SOEKNAD)
            }

        assertEquals(0, inspector.size)
        coVerify { behandlingKlientMock.finnEllerOpprettSak("13848599411", SakType.OMSTILLINGSSTOENAD) }
    }

    private fun testRapid(block: TestRapid.() -> Unit) =
        TestRapid().apply {
            FordelerRiver(this, behandlingKlientMock)
            block()
        }.inspektør

    companion object {
        val BARNEPENSJON_SOEKNAD = readFile("/fordeler/barnepensjon.json")
        val OMSTILLINGSSTOENAD_SOEKNAD = readFile("/fordeler/omstillingsstoenad.json")

        private fun readFile(file: String) = FordelerRiverTest::class.java.getResource(file)!!.readText()
    }
}
