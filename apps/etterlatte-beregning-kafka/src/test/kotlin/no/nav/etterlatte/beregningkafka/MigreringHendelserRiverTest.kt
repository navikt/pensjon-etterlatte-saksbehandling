package no.nav.etterlatte.beregningkafka

import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.brev.model.Spraak
import no.nav.etterlatte.libs.common.beregning.BeregningDTO
import no.nav.etterlatte.libs.common.beregning.Beregningsperiode
import no.nav.etterlatte.libs.common.beregning.Beregningstype
import no.nav.etterlatte.libs.common.grunnlag.Metadata
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.rapidsandrivers.EVENT_NAME_KEY
import no.nav.etterlatte.libs.common.rapidsandrivers.FEILENDE_STEG
import no.nav.etterlatte.libs.common.rapidsandrivers.FEILMELDING_KEY
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.rapidsandrivers.EventNames
import no.nav.etterlatte.rapidsandrivers.migrering.AvdoedForelder
import no.nav.etterlatte.rapidsandrivers.migrering.Beregning
import no.nav.etterlatte.rapidsandrivers.migrering.Enhet
import no.nav.etterlatte.rapidsandrivers.migrering.MigreringRequest
import no.nav.etterlatte.rapidsandrivers.migrering.Migreringshendelser
import no.nav.etterlatte.rapidsandrivers.migrering.PesysId
import no.nav.etterlatte.rapidsandrivers.migrering.Trygdetid
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import rapidsandrivers.BEHANDLING_ID_KEY
import rapidsandrivers.BEREGNING_KEY
import rapidsandrivers.HENDELSE_DATA_KEY
import java.math.BigDecimal
import java.time.YearMonth
import java.util.UUID

internal class MigreringHendelserRiverTest {
    private val behandlingService = mockk<BeregningService>()
    private val inspector = TestRapid().apply { MigreringHendelserRiver(this, behandlingService) }
    private val beregningDTO =
        BeregningDTO(
            beregningId = UUID.randomUUID(),
            behandlingId = UUID.randomUUID(),
            type = Beregningstype.BP,
            beregningsperioder =
                listOf(
                    Beregningsperiode(
                        datoFOM = YearMonth.of(2024, 1),
                        datoTOM = null,
                        utbetaltBeloep = 2500,
                        grunnbelop = 100000,
                        trygdetid = 40,
                        grunnbelopMnd = 100000 / 12,
                    ),
                ),
            beregnetDato = Tidspunkt.now(),
            grunnlagMetadata = Metadata(1234, 1),
        )
    private val fnr = Folkeregisteridentifikator.of("12101376212")
    private val request =
        MigreringRequest(
            pesysId = PesysId(1),
            enhet = Enhet("4817"),
            soeker = fnr,
            avdoedForelder = listOf(AvdoedForelder(fnr, Tidspunkt.now())),
            gjenlevendeForelder = Folkeregisteridentifikator.of("01498344336"),
            virkningstidspunkt = YearMonth.now(),
            foersteVirkningstidspunkt = YearMonth.now().minusYears(10),
            beregning =
                Beregning(
                    brutto = BigDecimal(1000),
                    netto = BigDecimal(1000),
                    anvendtTrygdetid = BigDecimal(40),
                    datoVirkFom = Tidspunkt.now(),
                    g = BigDecimal(100000),
                ),
            trygdetid = Trygdetid(emptyList()),
            spraak = Spraak.NN,
        )

    @BeforeEach
    fun setUp() {
        every { behandlingService.opprettBeregningsgrunnlag(any(), any()) } returns mockk()
    }

    @Test
    fun `skal beregne for migrering`() {
        val behandlingId = slot<UUID>()
        val returnValue =
            mockk<HttpResponse>().also {
                every {
                    runBlocking { it.body<BeregningDTO>() }
                } returns beregningDTO
            }

        every { behandlingService.beregn(capture(behandlingId)) } returns returnValue

        val melding =
            JsonMessage.newMessage(
                Migreringshendelser.BEREGN,
                mapOf(
                    BEHANDLING_ID_KEY to "a9d42eb9-561f-4320-8bba-2ba600e66e21",
                    HENDELSE_DATA_KEY to request,
                ),
            )

        inspector.sendTestMessage(melding.toJson())

        assertEquals(UUID.fromString("a9d42eb9-561f-4320-8bba-2ba600e66e21"), behandlingId.captured)
        assertEquals(1, inspector.inspektør.size)
        val resultat = inspector.inspektør.message(0)
        assertEquals(beregningDTO.toJson(), resultat.get(BEREGNING_KEY).toJson())
    }

    @Test
    fun `Beregning skal feile hvis beregnet beloep er lavere enn opprinnelig beloep fra Pesys`() {
        val behandlingId = slot<UUID>()
        val returnValue =
            mockk<HttpResponse>().also {
                every {
                    runBlocking { it.body<BeregningDTO>() }
                } returns
                    beregningDTO.copy(
                        beregningsperioder =
                            listOf(
                                beregningDTO.beregningsperioder.first().copy(utbetaltBeloep = 500),
                            ),
                    )
            }

        every { behandlingService.beregn(capture(behandlingId)) } returns returnValue

        val melding =
            JsonMessage.newMessage(
                Migreringshendelser.BEREGN,
                mapOf(
                    BEHANDLING_ID_KEY to "a9d42eb9-561f-4320-8bba-2ba600e66e21",
                    HENDELSE_DATA_KEY to request,
                ),
            )

        inspector.sendTestMessage(melding.toJson())

        assertEquals(UUID.fromString("a9d42eb9-561f-4320-8bba-2ba600e66e21"), behandlingId.captured)
        assertEquals(1, inspector.inspektør.size)
        val resultat = inspector.inspektør.message(0)
        assertEquals(EventNames.FEILA, resultat.get(EVENT_NAME_KEY).textValue())
        assertEquals(Migreringshendelser.BEREGN, resultat.get(FEILENDE_STEG).textValue())
        assertTrue(
            resultat.get(FEILMELDING_KEY).textValue()
                .contains("Beregnet beløp i Gjenny er lavere enn dagens beløp i Pesys."),
        )
    }

    @Test
    fun `Beregning skal feile hvis forskjellige grunnbeloep er benyttet`() {
        val behandlingId = slot<UUID>()
        val returnValue =
            mockk<HttpResponse>().also {
                every {
                    runBlocking { it.body<BeregningDTO>() }
                } returns
                    beregningDTO.copy(
                        beregningsperioder =
                            listOf(
                                beregningDTO.beregningsperioder.first().copy(grunnbelop = 105000),
                            ),
                    )
            }

        every { behandlingService.beregn(capture(behandlingId)) } returns returnValue

        val melding =
            JsonMessage.newMessage(
                Migreringshendelser.BEREGN,
                mapOf(
                    BEHANDLING_ID_KEY to "a9d42eb9-561f-4320-8bba-2ba600e66e21",
                    HENDELSE_DATA_KEY to request,
                ),
            )

        inspector.sendTestMessage(melding.toJson())

        assertEquals(UUID.fromString("a9d42eb9-561f-4320-8bba-2ba600e66e21"), behandlingId.captured)
        assertEquals(1, inspector.inspektør.size)
        val resultat = inspector.inspektør.message(0)
        assertEquals(EventNames.FEILA, resultat.get(EVENT_NAME_KEY).textValue())
        assertEquals(Migreringshendelser.BEREGN, resultat.get(FEILENDE_STEG).textValue())
        assertTrue(
            resultat.get(FEILMELDING_KEY).textValue()
                .contains("Beregning må være basert på samme G som i Pesys"),
        )
    }

    @Test
    fun `Beregning skal feile hvis ulik trygdetid er benyttet`() {
        val behandlingId = slot<UUID>()
        val returnValue =
            mockk<HttpResponse>().also {
                every {
                    runBlocking { it.body<BeregningDTO>() }
                } returns beregningDTO
            }

        every { behandlingService.beregn(capture(behandlingId)) } returns returnValue

        val melding =
            JsonMessage.newMessage(
                Migreringshendelser.BEREGN,
                mapOf(
                    BEHANDLING_ID_KEY to "a9d42eb9-561f-4320-8bba-2ba600e66e21",
                    HENDELSE_DATA_KEY to
                        request.copy(
                            beregning =
                                request.beregning.copy(
                                    anvendtTrygdetid =
                                        BigDecimal(
                                            35,
                                        ),
                                ),
                        ),
                ),
            )

        inspector.sendTestMessage(melding.toJson())

        assertEquals(UUID.fromString("a9d42eb9-561f-4320-8bba-2ba600e66e21"), behandlingId.captured)
        assertEquals(1, inspector.inspektør.size)
        val resultat = inspector.inspektør.message(0)
        assertEquals(EventNames.FEILA, resultat.get(EVENT_NAME_KEY).textValue())
        assertEquals(Migreringshendelser.BEREGN, resultat.get(FEILENDE_STEG).textValue())
        assertTrue(
            resultat.get(FEILMELDING_KEY).textValue()
                .contains("Beregning må være basert på samme trygdetid som i Pesys"),
        )
    }
}
