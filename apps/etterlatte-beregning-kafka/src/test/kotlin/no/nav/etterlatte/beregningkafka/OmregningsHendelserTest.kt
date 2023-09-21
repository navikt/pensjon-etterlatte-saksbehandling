package no.nav.etterlatte.beregningkafka

import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.libs.common.beregning.BeregningDTO
import no.nav.etterlatte.libs.common.beregning.Beregningstype
import no.nav.etterlatte.libs.common.grunnlag.Metadata
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.toJson
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import rapidsandrivers.BEREGNING_KEY
import java.io.FileNotFoundException
import java.util.UUID

internal class OmregningsHendelserTest {
    private val behandlingService = mockk<BeregningService>()
    private val trygdetidService = mockk<TrygdetidService>()
    private val inspector = TestRapid().apply { OmregningHendelser(this, behandlingService, trygdetidService) }

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
                beregningsperioder = listOf(),
                beregnetDato = Tidspunkt.now(),
                grunnlagMetadata = Metadata(1234, 1),
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

        every { behandlingService.beregn(capture(omregningsid)) }.returns(returnValue)
        every {
            behandlingService.opprettBeregningsgrunnlagFraForrigeBehandling(capture(behandlingsId), capture(forrigeBehandlingId))
        }.returns(noContentValue)

        val inspector = inspector.apply { sendTestMessage(fullMelding) }

        inspector.sendTestMessage(fullMelding)

        Assertions.assertEquals(UUID.fromString("11bf9683-4edb-403c-99da-b6ec6ff7fc31"), omregningsid.captured)
        Assertions.assertEquals(UUID.fromString("11bf9683-4edb-403c-99da-b6ec6ff7fc31"), behandlingsId.captured)
        Assertions.assertEquals(UUID.fromString("1be3d0dd-97be-4ccb-a71c-b3254ce7ae0a"), forrigeBehandlingId.captured)
        Assertions.assertEquals(2, inspector.inspektør.size)
        Assertions.assertEquals(
            beregningDTO.toJson(),
            inspector.inspektør.message(1).get(BEREGNING_KEY).toJson(),
        )
    }

    companion object {
        val fullMelding = readFile("/omregningshendelse.json")
    }
}

fun readFile(file: String) =
    OmregningsHendelserTest::class.java.getResource(file)?.readText()
        ?: throw FileNotFoundException("Fant ikke filen $file")
