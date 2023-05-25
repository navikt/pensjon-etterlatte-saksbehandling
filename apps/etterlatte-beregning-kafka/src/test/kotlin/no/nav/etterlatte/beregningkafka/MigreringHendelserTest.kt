package no.nav.etterlatte.beregningkafka

import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.beregning.BeregningDTO
import no.nav.etterlatte.libs.common.beregning.Beregningstype
import no.nav.etterlatte.libs.common.grunnlag.Metadata
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.rapidsandrivers.migrering.Enhet
import no.nav.etterlatte.rapidsandrivers.migrering.MigreringRequest
import no.nav.etterlatte.rapidsandrivers.migrering.Migreringshendelser
import no.nav.etterlatte.rapidsandrivers.migrering.PesysId
import no.nav.etterlatte.rapidsandrivers.migrering.Trygdetidsgrunnlag
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import rapidsandrivers.BEHANDLING_ID_KEY
import rapidsandrivers.BEREGNING_KEY
import rapidsandrivers.HENDELSE_DATA_KEY
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.*

internal class MigreringHendelserTest {

    private val behandlingService = mockk<BeregningService>()
    private val inspector = TestRapid().apply { MigreringHendelser(this, behandlingService) }

    @Test
    fun `skal beregne for migrering`() {
        val behandlingId = slot<UUID>()
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

        val request = MigreringRequest(
            pesysId = PesysId("1"),
            enhet = Enhet("4817"),
            fnr = Folkeregisteridentifikator.of("12101376212"),
            mottattDato = LocalDateTime.now(),
            persongalleri = Persongalleri("12101376212", "innsender", emptyList(), emptyList(), emptyList()),
            virkningstidspunkt = YearMonth.now(),
            trygdetidsgrunnlag = Trygdetidsgrunnlag(
                "Norge",
                LocalDate.now().minusYears(40),
                LocalDate.now()
            )
        )
        every { behandlingService.opprettBeregningsgrunnlag(any(), any()) } returns mockk()
        every { behandlingService.beregn(capture(behandlingId)) } returns returnValue

        val melding = JsonMessage.newMessage(
            Migreringshendelser.BEREGN,
            mapOf(
                BEHANDLING_ID_KEY to "a9d42eb9-561f-4320-8bba-2ba600e66e21",
                HENDELSE_DATA_KEY to request
            )
        )

        inspector.sendTestMessage(melding.toJson())

        Assertions.assertEquals(UUID.fromString("a9d42eb9-561f-4320-8bba-2ba600e66e21"), behandlingId.captured)
        Assertions.assertEquals(1, inspector.inspektør.size)
        Assertions.assertEquals(
            beregningDTO.toJson(),
            inspector.inspektør.message(0).get(BEREGNING_KEY).toJson()
        )
    }
}