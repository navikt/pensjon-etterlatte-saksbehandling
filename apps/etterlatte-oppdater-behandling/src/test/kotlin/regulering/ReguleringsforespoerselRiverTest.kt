package no.nav.etterlatte.regulering

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.etterlatte.BehandlingService
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.rapidsandrivers.EVENT_NAME_KEY
import no.nav.etterlatte.libs.common.rapidsandrivers.FEILENDE_STEG
import no.nav.etterlatte.libs.common.rapidsandrivers.lagParMedEventNameKey
import no.nav.etterlatte.libs.common.sak.BehandlingOgSak
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.sak.SakIDListe
import no.nav.etterlatte.libs.common.sak.Saker
import no.nav.etterlatte.rapidsandrivers.DATO_KEY
import no.nav.etterlatte.rapidsandrivers.EventNames.FEILA
import no.nav.etterlatte.rapidsandrivers.ReguleringEvents.ANTALL
import no.nav.etterlatte.rapidsandrivers.ReguleringEvents.KJOERING
import no.nav.etterlatte.rapidsandrivers.ReguleringHendelseType
import no.nav.etterlatte.rapidsandrivers.SAK_ID_KEY
import no.nav.etterlatte.rapidsandrivers.TILBAKESTILTE_BEHANDLINGER_KEY
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

internal class ReguleringsforespoerselRiverTest {
    private val foersteMai2023 = LocalDate.of(2023, 5, 1)

    private fun genererReguleringMelding(dato: LocalDate) =
        JsonMessage.newMessage(
            mapOf(
                ReguleringHendelseType.REGULERING_STARTA.lagParMedEventNameKey(),
                DATO_KEY to dato,
                KJOERING to "Regulering2023",
                ANTALL to Int.MAX_VALUE,
            ),
        )

    @Test
    fun `kan ta imot reguleringsmelding og kalle paa behandling`() {
        val melding = genererReguleringMelding(foersteMai2023)
        val vedtakServiceMock = mockk<BehandlingService>(relaxed = true)
        val featureToggleService = mockk<FeatureToggleService>().also { every { it.isEnabled(any(), any()) } returns true }
        val inspector = TestRapid().apply { ReguleringsforespoerselRiver(this, vedtakServiceMock, featureToggleService) }

        inspector.sendTestMessage(melding.toJson())
        verify(exactly = 1) {
            vedtakServiceMock.migrerAlleTempBehandlingerTilbakeTilTrygdetidOppdatert(any())
            vedtakServiceMock.hentAlleSaker("Regulering2023", Int.MAX_VALUE)
        }
    }

    @Test
    fun `skal lage ny melding for hver sak den faar tilbake`() {
        val melding = genererReguleringMelding(foersteMai2023)
        val vedtakServiceMock = mockk<BehandlingService>(relaxed = true)
        every { vedtakServiceMock.hentAlleSaker("Regulering2023", Int.MAX_VALUE) } returns
            Saker(
                listOf(
                    Sak("saksbehandler1", SakType.BARNEPENSJON, 1L, "4808"),
                    Sak("saksbehandler2", SakType.BARNEPENSJON, 2L, "4808"),
                    Sak("saksbehandler1", SakType.BARNEPENSJON, 3L, "4808"),
                ),
            )
        val featureToggleService = mockk<FeatureToggleService>().also { every { it.isEnabled(any(), any()) } returns true }
        val inspector = TestRapid().apply { ReguleringsforespoerselRiver(this, vedtakServiceMock, featureToggleService) }

        inspector.sendTestMessage(melding.toJson())
        val sendteMeldinger = inspector.inspektør.size
        Assertions.assertEquals(3, sendteMeldinger)

        for (i in 0 until inspector.inspektør.size) {
            Assertions.assertEquals(
                ReguleringHendelseType.SAK_FUNNET.lagEventnameForType(),
                inspector.inspektør.message(i).get(EVENT_NAME_KEY).asText(),
            )
            Assertions.assertEquals(foersteMai2023.toString(), inspector.inspektør.message(i).get(DATO_KEY).asText())
        }
    }

    @Test
    fun `skal sende med sakId for alle saker i basen`() {
        val melding = genererReguleringMelding(foersteMai2023)
        val behandlingServiceMock = mockk<BehandlingService>(relaxed = true)
        every { behandlingServiceMock.hentAlleSaker("Regulering2023", Int.MAX_VALUE) } returns
            Saker(
                listOf(
                    Sak("saksbehandler1", SakType.BARNEPENSJON, 1000L, "4808"),
                    Sak("saksbehandler2", SakType.BARNEPENSJON, 1002L, "4808"),
                    Sak("saksbehandler1", SakType.BARNEPENSJON, 1003L, "4808"),
                ),
            )
        val featureToggleService = mockk<FeatureToggleService>().also { every { it.isEnabled(any(), any()) } returns true }
        val inspector = TestRapid().apply { ReguleringsforespoerselRiver(this, behandlingServiceMock, featureToggleService) }
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
        val melding = genererReguleringMelding(foersteMai2023)
        val behandlingServiceMock = mockk<BehandlingService>(relaxed = true)
        val sakId = 1000L
        every { behandlingServiceMock.hentAlleSaker("Regulering2023", Int.MAX_VALUE) } returns
            Saker(
                listOf(
                    Sak("saksbehandler1", SakType.BARNEPENSJON, sakId, "4808"),
                ),
            )
        val behandlingId1 = UUID.randomUUID()
        val behandlingId2 = UUID.randomUUID()
        every { behandlingServiceMock.migrerAlleTempBehandlingerTilbakeTilTrygdetidOppdatert(any()) } returns
            SakIDListe(
                listOf(BehandlingOgSak(behandlingId1, sakId), BehandlingOgSak(behandlingId2, sakId)),
            )
        val featureToggleService = mockk<FeatureToggleService>().also { every { it.isEnabled(any(), any()) } returns true }
        val inspector = TestRapid().apply { ReguleringsforespoerselRiver(this, behandlingServiceMock, featureToggleService) }
        inspector.sendTestMessage(melding.toJson())

        val melding1 = inspector.inspektør.message(0)
        val ids = melding1.get(TILBAKESTILTE_BEHANDLINGER_KEY)
        Assertions.assertEquals("$behandlingId1;$behandlingId2", ids.textValue())
    }

    @Test
    fun `kjoerer med feilhaandtering`() {
        val melding = genererReguleringMelding(foersteMai2023)
        val behandlingServiceMock = mockk<BehandlingService>(relaxed = true)
        coEvery {
            behandlingServiceMock.migrerAlleTempBehandlingerTilbakeTilTrygdetidOppdatert(any())
        } throws RuntimeException("feil")

        val featureToggleService = mockk<FeatureToggleService>().also { every { it.isEnabled(any(), any()) } returns true }
        val inspector = TestRapid().apply { ReguleringsforespoerselRiver(this, behandlingServiceMock, featureToggleService) }

        inspector.sendTestMessage(melding.toJson())

        val melding1 = inspector.inspektør.message(0)
        Assertions.assertEquals(FEILA.lagEventnameForType(), melding1.get(EVENT_NAME_KEY).textValue())
        Assertions.assertEquals(ReguleringsforespoerselRiver::class.simpleName, melding1.get(FEILENDE_STEG).textValue())
    }
}
