import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import no.nav.etterlatte.MigreringHendelserRiver
import no.nav.etterlatte.VedtakService
import no.nav.etterlatte.brev.model.Spraak
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.rapidsandrivers.EVENT_NAME_KEY
import no.nav.etterlatte.libs.common.sak.VedtakSak
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.vedtak.Behandling
import no.nav.etterlatte.libs.common.vedtak.VedtakInnholdDto
import no.nav.etterlatte.libs.common.vedtak.VedtakNyDto
import no.nav.etterlatte.libs.common.vedtak.VedtakDto
import no.nav.etterlatte.libs.common.vedtak.VedtakKafkaHendelseType
import no.nav.etterlatte.libs.common.vedtak.VedtakStatus
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import no.nav.etterlatte.libs.testdata.grunnlag.AVDOED_FOEDSELSNUMMER
import no.nav.etterlatte.rapidsandrivers.EventNames
import no.nav.etterlatte.rapidsandrivers.migrering.AvdoedForelder
import no.nav.etterlatte.rapidsandrivers.migrering.Beregning
import no.nav.etterlatte.rapidsandrivers.migrering.BeregningMeta
import no.nav.etterlatte.rapidsandrivers.migrering.Enhet
import no.nav.etterlatte.rapidsandrivers.migrering.MIGRERING_KJORING_VARIANT
import no.nav.etterlatte.rapidsandrivers.migrering.MigreringKjoringVariant
import no.nav.etterlatte.rapidsandrivers.migrering.MigreringRequest
import no.nav.etterlatte.rapidsandrivers.migrering.Migreringshendelser
import no.nav.etterlatte.rapidsandrivers.migrering.Migreringshendelser.PAUSE
import no.nav.etterlatte.rapidsandrivers.migrering.PesysId
import no.nav.etterlatte.rapidsandrivers.migrering.Trygdetid
import no.nav.etterlatte.vedtaksvurdering.RapidInfo
import no.nav.etterlatte.vedtaksvurdering.VedtakOgRapid
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import rapidsandrivers.BEHANDLING_ID_KEY
import rapidsandrivers.SAK_ID_KEY
import java.time.YearMonth
import java.util.UUID

class MigreringHendelserRiverTest {
    private val vedtakService: VedtakService = mockk()
    private val inspector = TestRapid().apply { MigreringHendelserRiver(this, vedtakService) }

    @Test
    fun `hvis opprett vedtak feila, legg meldinga rett paa feila-koea`() {
        val melding =
            JsonMessage.newMessage(
                Migreringshendelser.VEDTAK,
                mapOf(
                    BEHANDLING_ID_KEY to "a9d42eb9-561f-4320-8bba-2ba600e66e21",
                    SAK_ID_KEY to "1",
                    MIGRERING_KJORING_VARIANT to MigreringKjoringVariant.FULL_KJORING,
                ),
            )
        coEvery {
            vedtakService.opprettVedtakFattOgAttester(any(), any(), any())
        } throws RuntimeException("Feila under opprett vedtak")

        inspector.sendTestMessage(melding.toJson())

        assertEquals(1, inspector.inspektør.size)
        val sendtMelding = inspector.inspektør.message(0)
        assertEquals(sendtMelding.get(EVENT_NAME_KEY).asText(), EventNames.FEILA)
    }

    @Test
    fun `oppretter vedtak, fatter vedtak og attesterer`() {
        val melding =
            JsonMessage.newMessage(
                Migreringshendelser.VEDTAK,
                mapOf(
                    BEHANDLING_ID_KEY to behandlingId,
                    SAK_ID_KEY to "1",
                    MIGRERING_KJORING_VARIANT to MigreringKjoringVariant.FULL_KJORING,
                ),
            )
        val vedtakDto =
            VedtakNyDto(
                id = 123,
                behandlingId = UUID.fromString(behandlingId),
                status = VedtakStatus.OPPRETTET,
                sak =
                    VedtakSak(
                        ident = "ident",
                        sakType = SakType.BARNEPENSJON,
                        id = 321L,
                    ),
                type = VedtakType.INNVILGELSE,
                vedtakFattet = null,
                attestasjon = null,
                innhold =
                    VedtakInnholdDto.VedtakBehandlingDto(
                        virkningstidspunkt = YearMonth.now(),
                        behandling =
                            Behandling(
                                type = BehandlingType.FØRSTEGANGSBEHANDLING,
                                id = UUID.fromString(behandlingId),
                                null,
                                null,
                            ),
                        utbetalingsperioder = emptyList(),
                    ),
            )
        coEvery { vedtakService.opprettVedtakFattOgAttester(any(), any(), any()) } returns vedtakDto

        inspector.sendTestMessage(melding.toJson())

        coVerify { vedtakService.opprettVedtakFattOgAttester(any(), any(), any()) }

        assertEquals(0, inspector.inspektør.size)
        val sendtTilRapid = inspector.inspektør.message(0)
        assertEquals(VedtakKafkaHendelseType.ATTESTERT.name, sendtTilRapid.get(EVENT_NAME_KEY).textValue())
        assertEquals(sendtTilRapid.get(BEHANDLING_ID_KEY).textValue(), behandlingId)
    }

    @Test
    fun `Sender hendelse om pause om kjoeringsvariant er pause`() {
        val melding =
            JsonMessage.newMessage(
                Migreringshendelser.VEDTAK,
                mapOf(
                    BEHANDLING_ID_KEY to "a9d42eb9-561f-4320-8bba-2ba600e66e21",
                    SAK_ID_KEY to "1",
                    MIGRERING_KJORING_VARIANT to MigreringKjoringVariant.MED_PAUSE,
                ),
            )

        coEvery { vedtakService.opprettVedtakFattOgAttester(any(), any(), any()) } returns vedtakDto

        inspector.sendTestMessage(melding.toJson())

        coVerify { vedtakService.opprettVedtakFattOgAttester(any(), any(), any()) }

        assertEquals(1, inspector.inspektør.size)
        val resultat = inspector.inspektør.message(0)
        assertEquals(PAUSE, resultat.get(EVENT_NAME_KEY).asText())
    }

    companion object {
        private val behandlingId = "a9d42eb9-561f-4320-8bba-2ba600e66e21"
        private val vedtakDto =
            VedtakNyDto(
                id = 123,
                behandlingId = UUID.fromString(behandlingId),
                status = VedtakStatus.OPPRETTET,
                sak =
                    VedtakSak(
                        ident = "ident",
                        sakType = SakType.BARNEPENSJON,
                        id = 321L,
                    ),
                type = VedtakType.INNVILGELSE,
                vedtakFattet = null,
                attestasjon = null,
                innhold =
                    VedtakInnholdDto.VedtakBehandlingDto(
                        virkningstidspunkt = YearMonth.now(),
                        behandling =
                            Behandling(
                                type = BehandlingType.FØRSTEGANGSBEHANDLING,
                                id = UUID.fromString(behandlingId),
                                null,
                                null,
                            ),
                        utbetalingsperioder = emptyList(),
                    ),
            )

        private val request =
            MigreringRequest(
                pesysId = PesysId(1),
                enhet = Enhet("4817"),
                soeker = AVDOED_FOEDSELSNUMMER,
                avdoedForelder = listOf(AvdoedForelder(AVDOED_FOEDSELSNUMMER, Tidspunkt.now())),
                gjenlevendeForelder = null,
                virkningstidspunkt = YearMonth.now(),
                beregning =
                    Beregning(
                        brutto = 1500,
                        netto = 1500,
                        anvendtTrygdetid = 40,
                        datoVirkFom = Tidspunkt.now(),
                        prorataBroek = null,
                        g = 100_000,
                        meta =
                            BeregningMeta(
                                beregningsMetodeType = "FOLKETRYGD",
                                resultatType = "",
                                resultatKilde = "AUTO",
                                kravVelgType = "",
                            ),
                    ),
                trygdetid = Trygdetid(emptyList()),
                spraak = Spraak.NN,
            )
    }
}
