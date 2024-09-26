package no.nav.etterlatte.beregningkafka

import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.behandling.randomSakId
import no.nav.etterlatte.grunnbeloep.Grunnbeloep
import no.nav.etterlatte.libs.common.beregning.BeregningDTO
import no.nav.etterlatte.libs.common.beregning.Beregningsperiode
import no.nav.etterlatte.libs.common.beregning.Beregningstype
import no.nav.etterlatte.libs.common.grunnlag.Metadata
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.rapidsandrivers.BEREGNING_KEY
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.io.FileNotFoundException
import java.math.BigDecimal
import java.time.Month
import java.time.YearMonth
import java.util.UUID

internal class OmregningsHendelserTest {
    private val beregningService = mockk<BeregningService>()
    private val inspector = TestRapid().apply { OmregningHendelserBeregningRiver(this, beregningService) }

    @Test
    fun `skal opprette omregning`() {
        val omregningsid = slot<UUID>()
        val behandlingsId = slot<UUID>()
        val forrigeBehandlingId = slot<UUID>()
        val beregningDTO =
            BeregningDTO(
                beregningId = UUID.randomUUID(),
                behandlingId = UUID.randomUUID(),
                type = Beregningstype.BP,
                beregningsperioder =
                    listOf(
                        Beregningsperiode(
                            datoFOM = YearMonth.of(2023, Month.JANUARY),
                            utbetaltBeloep = 1000,
                            grunnbelopMnd = 1000,
                            grunnbelop = 12000,
                            trygdetid = 40,
                        ),
                    ),
                beregnetDato = Tidspunkt.now(),
                grunnlagMetadata = Metadata(randomSakId(), 1),
                overstyrBeregning = null,
            )

        val returnValue =
            mockk<HttpResponse>().also {
                every {
                    runBlocking { it.body<BeregningDTO>() }
                } returns beregningDTO
            }

        val noContentValue =
            mockk<HttpResponse>().also {
                every {
                    runBlocking { it.status }
                } returns HttpStatusCode.NoContent
            }

        every {
            beregningService.opprettBeregningsgrunnlagFraForrigeBehandling(
                capture(behandlingsId),
                capture(forrigeBehandlingId),
            )
        }.returns(noContentValue)
        every { beregningService.tilpassOverstyrtBeregningsgrunnlagForRegulering(capture(omregningsid)) } returns mockk()
        every { beregningService.beregn(capture(omregningsid)) }.returns(returnValue)
        every { beregningService.hentBeregning(any()) }.returns(returnValue)
        coEvery { beregningService.hentGrunnbeloep() } returns Grunnbeloep(YearMonth.now(), 1000, 100, BigDecimal.ONE)

        val inspector = inspector.apply { sendTestMessage(fullMelding) }

        inspector.sendTestMessage(fullMelding)

        Assertions.assertEquals(UUID.fromString("11bf9683-4edb-403c-99da-b6ec6ff7fc31"), behandlingsId.captured)
        Assertions.assertEquals(UUID.fromString("1be3d0dd-97be-4ccb-a71c-b3254ce7ae0a"), forrigeBehandlingId.captured)
        Assertions.assertEquals(2, inspector.inspektør.size)
        Assertions.assertEquals(
            beregningDTO.toJson(),
            inspector.inspektør
                .message(1)
                .get(BEREGNING_KEY)
                .toJson(),
        )
    }

    companion object {
        val fullMelding = readFile("/omregningshendelse.json")
    }
}

fun readFile(file: String) =
    OmregningsHendelserTest::class.java.getResource(file)?.readText()
        ?: throw FileNotFoundException("Fant ikke filen $file")
