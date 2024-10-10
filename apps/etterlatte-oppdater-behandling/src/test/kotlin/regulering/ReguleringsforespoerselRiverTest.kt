package no.nav.etterlatte.regulering

import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.etterlatte.BehandlingService
import no.nav.etterlatte.behandling.randomSakId
import no.nav.etterlatte.behandling.sakId1
import no.nav.etterlatte.behandling.sakId2
import no.nav.etterlatte.behandling.sakId3
import no.nav.etterlatte.behandling.tilSakId
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.rapidsandrivers.EVENT_NAME_KEY
import no.nav.etterlatte.libs.common.rapidsandrivers.FEILENDE_STEG
import no.nav.etterlatte.libs.common.rapidsandrivers.lagParMedEventNameKey
import no.nav.etterlatte.libs.common.sak.BehandlingOgSak
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.sak.SakIDListe
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.sak.Saker
import no.nav.etterlatte.rapidsandrivers.AAPNE_BEHANDLINGER_KEY
import no.nav.etterlatte.rapidsandrivers.DATO_KEY
import no.nav.etterlatte.rapidsandrivers.EventNames.FEILA
import no.nav.etterlatte.rapidsandrivers.HENDELSE_DATA_KEY
import no.nav.etterlatte.rapidsandrivers.OmregningDataPacket
import no.nav.etterlatte.rapidsandrivers.RapidEvents.ANTALL
import no.nav.etterlatte.rapidsandrivers.RapidEvents.KJOERING
import no.nav.etterlatte.rapidsandrivers.RapidEvents.SPESIFIKKE_SAKER
import no.nav.etterlatte.rapidsandrivers.ReguleringHendelseType
import no.nav.etterlatte.rapidsandrivers.SAK_TYPE
import no.nav.etterlatte.rapidsandrivers.TILBAKESTILTE_BEHANDLINGER_KEY
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

internal class ReguleringsforespoerselRiverTest {
    private val foersteMai2023 = LocalDate.of(2023, 5, 1)
    private val featureToggleService =
        mockk<FeatureToggleService>().also { every { it.isEnabled(any(), any()) } returns true }

    private fun genererReguleringMelding(dato: LocalDate) =
        JsonMessage.newMessage(
            mapOf(
                ReguleringHendelseType.REGULERING_STARTA.lagParMedEventNameKey(),
                DATO_KEY to dato,
                KJOERING to "Regulering2023",
                ANTALL to 12000,
                SPESIFIKKE_SAKER to listOf<SakId>(),
            ),
        )

    private val porsgrunn = Enheter.PORSGRUNN.enhetNr

    @Test
    fun `kan ta imot reguleringsmelding og kalle paa behandling`() {
        val melding = genererReguleringMelding(foersteMai2023)
        val vedtakServiceMock =
            mockk<BehandlingService>(relaxed = true).also {
                every { it.hentAlleSaker(any(), any(), any(), any()) } returns
                    Saker(
                        listOf(Sak("saksbehandler1", SakType.BARNEPENSJON, randomSakId(), porsgrunn)),
                    )
            }
        val inspector =
            TestRapid().apply { ReguleringsforespoerselRiver(this, vedtakServiceMock, featureToggleService) }

        inspector.sendTestMessage(melding.toJson())
        verify(exactly = 1) {
            vedtakServiceMock.migrerAlleTempBehandlingerTilbakeTilTrygdetidOppdatert(any())
            vedtakServiceMock.hentAlleSaker("Regulering2023", any(), any(), any())
        }
    }

    @Test
    fun `skal lage ny melding for hver sak den faar tilbake`() {
        val melding = genererReguleringMelding(foersteMai2023)
        val vedtakServiceMock = mockk<BehandlingService>(relaxed = true)
        every { vedtakServiceMock.hentAlleSaker("Regulering2023", any(), any(), any()) } returns
            Saker(
                listOf(
                    Sak("saksbehandler1", SakType.BARNEPENSJON, sakId1, porsgrunn),
                    Sak("saksbehandler2", SakType.BARNEPENSJON, sakId2, porsgrunn),
                    Sak("saksbehandler1", SakType.BARNEPENSJON, sakId3, porsgrunn),
                ),
            ) andThen Saker(listOf())
        val inspector =
            TestRapid().apply { ReguleringsforespoerselRiver(this, vedtakServiceMock, featureToggleService) }

        inspector.sendTestMessage(melding.toJson())
        val sendteMeldinger = inspector.inspektør.size
        Assertions.assertEquals(3, sendteMeldinger)

        for (i in 0 until inspector.inspektør.size) {
            Assertions.assertEquals(
                ReguleringHendelseType.SAK_FUNNET.lagEventnameForType(),
                inspector.inspektør
                    .message(i)
                    .get(EVENT_NAME_KEY)
                    .asText(),
            )
            Assertions.assertEquals(
                foersteMai2023.toString(),
                inspector.inspektør
                    .message(i)
                    .get(DATO_KEY)
                    .asText(),
            )
        }
    }

    @Test
    fun `skal sende med sakId for alle saker i basen`() {
        val melding = genererReguleringMelding(foersteMai2023)
        val behandlingServiceMock = mockk<BehandlingService>(relaxed = true)
        val sak1 = randomSakId()
        val sak2 = randomSakId()
        val sak3 = randomSakId()
        every { behandlingServiceMock.hentAlleSaker("Regulering2023", any(), any(), any()) } returns
            Saker(
                listOf(
                    Sak("saksbehandler1", SakType.BARNEPENSJON, sak1, porsgrunn),
                    Sak("saksbehandler2", SakType.BARNEPENSJON, sak2, porsgrunn),
                    Sak("saksbehandler1", SakType.BARNEPENSJON, sak3, porsgrunn),
                ),
            )
        val inspector =
            TestRapid().apply { ReguleringsforespoerselRiver(this, behandlingServiceMock, featureToggleService) }
        inspector.sendTestMessage(melding.toJson())

        val melding1 = inspector.inspektør.message(0)
        val melding2 = inspector.inspektør.message(1)
        val melding3 = inspector.inspektør.message(2)

        melding1.get(HENDELSE_DATA_KEY).get(OmregningDataPacket::sakId.name).tilSakId() shouldBe sak1
        melding2.get(HENDELSE_DATA_KEY).get(OmregningDataPacket::sakId.name).tilSakId() shouldBe sak2
        melding3.get(HENDELSE_DATA_KEY).get(OmregningDataPacket::sakId.name).tilSakId() shouldBe sak3
    }

    @Test
    fun `ider fra tilbakestilte og aapne behandlinger sendes med i meldinga videre`() {
        val melding = genererReguleringMelding(foersteMai2023)
        val behandlingServiceMock = mockk<BehandlingService>(relaxed = true)
        val sakId = randomSakId()
        every { behandlingServiceMock.hentAlleSaker("Regulering2023", any(), any(), any()) } returns
            Saker(
                listOf(
                    Sak("saksbehandler1", SakType.BARNEPENSJON, sakId, porsgrunn),
                ),
            )
        val behandlingId1 = UUID.randomUUID()
        val behandlingId2 = UUID.randomUUID()
        every { behandlingServiceMock.migrerAlleTempBehandlingerTilbakeTilTrygdetidOppdatert(any()) } returns
            SakIDListe(
                tilbakestileBehandlinger =
                    listOf(
                        BehandlingOgSak(behandlingId1, sakId),
                        BehandlingOgSak(behandlingId2, sakId),
                    ),
                aapneBehandlinger = listOf(BehandlingOgSak(behandlingId1, sakId)),
            )
        val inspector =
            TestRapid().apply { ReguleringsforespoerselRiver(this, behandlingServiceMock, featureToggleService) }
        inspector.sendTestMessage(melding.toJson())

        val melding1 = inspector.inspektør.message(0)
        val tilbakestilte = melding1.get(TILBAKESTILTE_BEHANDLINGER_KEY)
        val aapne = melding1.get(AAPNE_BEHANDLINGER_KEY)
        Assertions.assertEquals("$behandlingId1;$behandlingId2", tilbakestilte.textValue())
        Assertions.assertEquals("$behandlingId1", aapne.textValue())
    }

    @Test
    fun `kjoerer med feilhaandtering`() {
        val melding = genererReguleringMelding(foersteMai2023)
        val behandlingServiceMock =
            mockk<BehandlingService>(relaxed = true).also {
                every { it.hentAlleSaker(any(), any(), any(), any()) } returns
                    Saker(
                        listOf(Sak("saksbehandler1", SakType.BARNEPENSJON, randomSakId(), porsgrunn)),
                    )
            }
        coEvery {
            behandlingServiceMock.migrerAlleTempBehandlingerTilbakeTilTrygdetidOppdatert(any())
        } throws RuntimeException("feil")

        val inspector =
            TestRapid().apply { ReguleringsforespoerselRiver(this, behandlingServiceMock, featureToggleService) }

        inspector.sendTestMessage(melding.toJson())

        val melding1 = inspector.inspektør.message(0)
        Assertions.assertEquals(FEILA.lagEventnameForType(), melding1.get(EVENT_NAME_KEY).textValue())
        Assertions.assertEquals(ReguleringsforespoerselRiver::class.simpleName, melding1.get(FEILENDE_STEG).textValue())
    }

    @Test
    fun `deler opp i flere kall hvis vi proever med mange av gangen`() {
        val melding = genererReguleringMelding(foersteMai2023)
        val vedtakServiceMock = mockk<BehandlingService>(relaxed = true)
        val kjoering = "Regulering2023"
        every { vedtakServiceMock.hentAlleSaker(kjoering, any(), any(), any()) } returns
            Saker(
                (0..MAKS_BATCHSTOERRELSE).map {
                    Sak("saksbehandler1", SakType.BARNEPENSJON, SakId(it.toLong()), porsgrunn)
                },
            ) andThen
            Saker(
                listOf(
                    Sak("saksbehandler1", SakType.BARNEPENSJON, randomSakId(), porsgrunn),
                ),
            ) andThen Saker(listOf())
        val inspector =
            TestRapid().apply { ReguleringsforespoerselRiver(this, vedtakServiceMock, featureToggleService) }

        inspector.sendTestMessage(melding.toJson())

        verify(exactly = 2) { vedtakServiceMock.hentAlleSaker(kjoering, any(), any(), any()) }
    }

    @Test
    fun `henter saker for gitt sakstype`() {
        val melding =
            JsonMessage.newMessage(
                mapOf(
                    ReguleringHendelseType.REGULERING_STARTA.lagParMedEventNameKey(),
                    DATO_KEY to LocalDate.now(),
                    KJOERING to "Regulering2023",
                    ANTALL to 10,
                    SPESIFIKKE_SAKER to listOf<SakId>(),
                    SAK_TYPE to SakType.BARNEPENSJON.name,
                ),
            )

        val vedtakServiceMock = mockk<BehandlingService>(relaxed = true)
        val inspector =
            TestRapid().apply { ReguleringsforespoerselRiver(this, vedtakServiceMock, featureToggleService) }

        inspector.sendTestMessage(melding.toJson())
        verify(exactly = 1) {
            vedtakServiceMock.hentAlleSaker("Regulering2023", 10, emptyList(), any(), SakType.BARNEPENSJON)
        }
    }

    @Test
    @Disabled
    fun `ignorerer spesifiserte saker`() {
        val melding = genererReguleringMelding(LocalDate.now())
        val vedtakServiceMock =
            mockk<BehandlingService>(relaxed = true).also {
                every { it.hentAlleSaker(any(), any(), any(), any(), any()) } returns mockk()
            }
        val inspector =
            TestRapid().apply { ReguleringsforespoerselRiver(this, vedtakServiceMock, featureToggleService) }

        inspector.sendTestMessage(melding.toJson())
        verify(exactly = 1) {
            vedtakServiceMock.hentAlleSaker(
                any(),
                any(),
                any(),
                match { it.any { ekskludert -> ekskludert == randomSakId() } },
            )
        }
    }
}
