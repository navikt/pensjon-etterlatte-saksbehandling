import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.etterlatte.Behandling
import no.nav.etterlatte.Reguleringsforespoersel
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.rapidsandrivers.EVENT_NAME_KEY
import no.nav.etterlatte.libs.common.rapidsandrivers.FEILENDE_STEG
import no.nav.etterlatte.libs.common.sak.BehandlingOgSak
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.sak.SakIDListe
import no.nav.etterlatte.libs.common.sak.Saker
import no.nav.etterlatte.rapidsandrivers.EventNames.FEILA
import no.nav.etterlatte.rapidsandrivers.EventNames.FINN_LOEPENDE_YTELSER
import no.nav.etterlatte.rapidsandrivers.EventNames.REGULERING_EVENT_NAME
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import rapidsandrivers.DATO_KEY
import rapidsandrivers.SAK_ID_KEY
import rapidsandrivers.TILBAKESTILTE_BEHANDLINGER_KEY
import java.time.LocalDate
import java.util.*

internal class ReguleringsforespoerselTest {

    private val `1_mai_2023` = LocalDate.of(2023, 5, 1)

    private fun genererReguleringMelding(dato: LocalDate) = JsonMessage.newMessage(
        mapOf(
            EVENT_NAME_KEY to REGULERING_EVENT_NAME,
            DATO_KEY to dato
        )
    )

    @Test
    fun `kan ta imot reguleringsmelding og kalle på behandling`() {
        val melding = genererReguleringMelding(`1_mai_2023`)
        val vedtakServiceMock = mockk<Behandling>(relaxed = true)
        val inspector = TestRapid().apply { Reguleringsforespoersel(this, vedtakServiceMock) }

        inspector.sendTestMessage(melding.toJson())
        verify(exactly = 1) {
            vedtakServiceMock.migrerAlleTempBehandlingerTilbakeTilVilkaarsvurdert()
            vedtakServiceMock.hentAlleSaker()
        }
    }

    @Test
    fun `skal lage ny melding for hver sak den faar tilbake`() {
        val melding = genererReguleringMelding(`1_mai_2023`)
        val vedtakServiceMock = mockk<Behandling>(relaxed = true)
        every { vedtakServiceMock.hentAlleSaker() } returns Saker(
            listOf(
                Sak("saksbehandler1", SakType.BARNEPENSJON, 1L),
                Sak("saksbehandler2", SakType.BARNEPENSJON, 2L),
                Sak("saksbehandler1", SakType.BARNEPENSJON, 3L)
            )
        )
        val inspector = TestRapid().apply { Reguleringsforespoersel(this, vedtakServiceMock) }

        inspector.sendTestMessage(melding.toJson())
        val sendteMeldinger = inspector.inspektør.size
        Assertions.assertEquals(3, sendteMeldinger)

        for (i in 0 until inspector.inspektør.size) {
            Assertions.assertEquals(FINN_LOEPENDE_YTELSER, inspector.inspektør.message(i).get(EVENT_NAME_KEY).asText())
            Assertions.assertEquals(`1_mai_2023`.toString(), inspector.inspektør.message(i).get(DATO_KEY).asText())
        }
    }

    @Test
    fun `skal sende med sakId for alle saker i basen`() {
        val melding = genererReguleringMelding(`1_mai_2023`)
        val behandlingServiceMock = mockk<Behandling>(relaxed = true)
        every { behandlingServiceMock.hentAlleSaker() } returns Saker(
            listOf(
                Sak("saksbehandler1", SakType.BARNEPENSJON, 1000L),
                Sak("saksbehandler2", SakType.BARNEPENSJON, 1002L),
                Sak("saksbehandler1", SakType.BARNEPENSJON, 1003L)
            )
        )
        val inspector = TestRapid().apply { Reguleringsforespoersel(this, behandlingServiceMock) }
        inspector.sendTestMessage(melding.toJson())

        val melding1 = inspector.inspektør.message(0)
        val melding2 = inspector.inspektør.message(1)
        val melding3 = inspector.inspektør.message(2)

        Assertions.assertEquals(1000L, melding1.get(SAK_ID_KEY).asLong())
        Assertions.assertEquals(1002L, melding2.get(SAK_ID_KEY).asLong())
        Assertions.assertEquals(1003L, melding3.get(SAK_ID_KEY).asLong())
    }

    @Test
    fun `ider fra tilbakestilte behandlinger sendes med i meldinga videre`() {
        val melding = genererReguleringMelding(`1_mai_2023`)
        val behandlingServiceMock = mockk<Behandling>(relaxed = true)
        val sakId = 1000L
        every { behandlingServiceMock.hentAlleSaker() } returns Saker(
            listOf(
                Sak("saksbehandler1", SakType.BARNEPENSJON, sakId)
            )
        )
        val behandlingId1 = UUID.randomUUID()
        val behandlingId2 = UUID.randomUUID()
        every { behandlingServiceMock.migrerAlleTempBehandlingerTilbakeTilVilkaarsvurdert() } returns SakIDListe(
            listOf(BehandlingOgSak(behandlingId1, sakId), BehandlingOgSak(behandlingId2, sakId))
        )
        val inspector = TestRapid().apply { Reguleringsforespoersel(this, behandlingServiceMock) }
        inspector.sendTestMessage(melding.toJson())

        val melding1 = inspector.inspektør.message(0)
        val ids = melding1.get(TILBAKESTILTE_BEHANDLINGER_KEY)
        Assertions.assertEquals("$behandlingId1;$behandlingId2", ids.textValue())
    }

    @Test
    fun `kjoerer med feilhaandtering`() {
        val melding = genererReguleringMelding(`1_mai_2023`)
        val behandlingServiceMock = mockk<Behandling>(relaxed = true)
        coEvery {
            behandlingServiceMock.migrerAlleTempBehandlingerTilbakeTilVilkaarsvurdert()
        } throws RuntimeException("feil")

        val inspector = TestRapid().apply { Reguleringsforespoersel(this, behandlingServiceMock) }

        inspector.sendTestMessage(melding.toJson())

        val melding1 = inspector.inspektør.message(0)
        Assertions.assertEquals(FEILA, melding1.get(EVENT_NAME_KEY).textValue())
        Assertions.assertEquals(REGULERING_EVENT_NAME, melding1.get(FEILENDE_STEG).textValue())
    }
}