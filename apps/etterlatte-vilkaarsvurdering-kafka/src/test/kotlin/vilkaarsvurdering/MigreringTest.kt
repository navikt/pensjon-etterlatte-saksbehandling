package vilkaarsvurdering

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.testdata.grunnlag.SOEKER_FOEDSELSNUMMER
import no.nav.etterlatte.rapidsandrivers.migrering.Enhet
import no.nav.etterlatte.rapidsandrivers.migrering.MigreringRequest
import no.nav.etterlatte.rapidsandrivers.migrering.Migreringshendelser
import no.nav.etterlatte.rapidsandrivers.migrering.PesysId
import no.nav.etterlatte.vilkaarsvurdering.Migrering
import no.nav.etterlatte.vilkaarsvurdering.services.VilkaarsvurderingService
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import rapidsandrivers.BEHANDLING_ID_KEY
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.*

internal class MigreringTest {
    private val vilkaarsvurderingServiceMock = mockk<VilkaarsvurderingService> {
        coEvery { migrer(any()) } returns mockk()
    }
    private val testRapid = TestRapid()
        .apply { Migrering(this, vilkaarsvurderingServiceMock) }

    @Test
    fun `tar opp migrer vilkaarsvurdering-event, kopierer vilkaarsvurdering og poster ny BEREGN-melding`() {
        val behandlingId = UUID.randomUUID()

        val melding = JsonMessage.newMessage(
            mapOf(
                "@event_name" to Migreringshendelser.VILKAARSVURDER,
                "sakId" to 1L,
                "request" to MigreringRequest(
                    PesysId("123"),
                    Enhet("1234"),
                    SOEKER_FOEDSELSNUMMER,
                    LocalDateTime.now(),
                    Persongalleri(soeker = SOEKER_FOEDSELSNUMMER.value),
                    YearMonth.now()
                ),
                BEHANDLING_ID_KEY to behandlingId
            )
        ).toJson()
        testRapid.sendTestMessage(melding)

        coVerify(exactly = 1) {
            vilkaarsvurderingServiceMock.migrer(
                any()
            )
        }
        with(testRapid.inspektør.message(0)) {
            assertEquals(Migreringshendelser.TRYGDETID, this["@event_name"].asText())
        }
    }
}