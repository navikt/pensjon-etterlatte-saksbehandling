package no.nav.etterlatte.beregningasynk

import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.libs.common.behandling.Omberegningsnoekler
import no.nav.etterlatte.libs.common.beregning.BeregningDTO
import no.nav.etterlatte.libs.common.beregning.Beregningstype
import no.nav.etterlatte.libs.common.grunnlag.Metadata
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.toJson
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.io.FileNotFoundException
import java.util.*

internal class OmberegningsHendelserTest {

    private val behandlingService = mockk<BeregningService>()
    private val inspector = TestRapid().apply { OmberegningHendelser(this, behandlingService) }

    @Test
    fun `skal opprette omberegning`() {
        val omberegningsid = slot<Long>()
        val beregningDTO = BeregningDTO(
            beregningId = UUID.randomUUID(),
            behandlingId = UUID.randomUUID(),
            type = Beregningstype.BP,
            beregningsperioder = listOf(),
            beregnetDato = Tidspunkt.now(),
            grunnlagMetadata = Metadata(1234, 1)
        )

        val returnValue = mockk<HttpResponse>().also {
            every {
                runBlocking { it.body<BeregningDTO>() }
            } returns beregningDTO
        }
        every { behandlingService.opprettOmberegning(capture(omberegningsid)) }.returns(returnValue)

        val inspector = inspector.apply { sendTestMessage(fullMelding) }

        inspector.sendTestMessage(fullMelding)

        Assertions.assertEquals(1234, omberegningsid.captured)
        Assertions.assertEquals(2, inspector.inspektør.size)
        Assertions.assertEquals(
            beregningDTO.toJson(),
            inspector.inspektør.message(1).get(Omberegningsnoekler.beregning).toJson()
        )
    }

    companion object {
        val fullMelding = readFile("/omberegningshendelse.json")
    }
}

fun readFile(file: String) = OmberegningsHendelserTest::class.java.getResource(file)?.readText()
    ?: throw FileNotFoundException("Fant ikke filen $file")