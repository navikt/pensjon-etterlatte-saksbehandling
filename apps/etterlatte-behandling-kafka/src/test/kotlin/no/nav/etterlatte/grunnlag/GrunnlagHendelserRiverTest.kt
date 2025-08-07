package no.nav.etterlatte.grunnlag

import com.fasterxml.jackson.databind.JsonNode
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.rapidsandrivers.lagParMedEventNameKey
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.toJsonNode
import no.nav.etterlatte.libs.testdata.grunnlag.AVDOED_FOEDSELSNUMMER
import no.nav.etterlatte.libs.testdata.grunnlag.statiskUuid
import no.nav.etterlatte.rapidsandrivers.BEHANDLING_ID_KEY
import no.nav.etterlatte.rapidsandrivers.EventNames
import no.nav.etterlatte.rapidsandrivers.FNR_KEY
import no.nav.etterlatte.rapidsandrivers.OPPLYSNING_KEY
import no.nav.etterlatte.rapidsandrivers.SAK_ID_KEY
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.util.UUID

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class GrunnlagHendelserRiverTest {
//    private val grunnlagKlientMock = mockk<GrunnlagKlient>(relaxed = true)
//
//    private lateinit var inspector: TestRapid
//
//    @BeforeAll
//    fun beforeAll() {
//        inspector =
//            TestRapid().apply {
//                GrunnlagHendelserRiver(this, grunnlagKlientMock)
//            }
//    }
//
//    private val fnr = AVDOED_FOEDSELSNUMMER
//    private val tidspunkt = Tidspunkt.now()
//    private val kilde = Grunnlagsopplysning.Pdl(tidspunkt, null, null)
//    private val nyOpplysning =
//        Grunnlagsopplysning(
//            id = statiskUuid,
//            kilde = kilde,
//            opplysningType = Opplysningstype.NAVN,
//            meta = objectMapper.createObjectNode(),
//            opplysning = "Ola".toJsonNode(),
//            attestering = null,
//            fnr = fnr,
//        )
//
//    @Test
//    fun `Ny personopplysning sendes til grunnlag`() {
//        val melding =
//            JsonMessage
//                .newMessage(
//                    mapOf(
//                        EventNames.NY_OPPLYSNING.lagParMedEventNameKey(),
//                        OPPLYSNING_KEY to listOf(nyOpplysning),
//                        FNR_KEY to fnr,
//                        SAK_ID_KEY to 1,
//                        BEHANDLING_ID_KEY to UUID.randomUUID(),
//                    ),
//                ).toJson()
//
//        inspector.sendTestMessage(melding)
//
//        val opplysningSlot = slot<List<Grunnlagsopplysning<JsonNode>>>()
//
//        verify { grunnlagKlientMock.lagreNyePersonopplysninger(any(), any(), any(), capture(opplysningSlot)) }
//
//        with(opplysningSlot.captured.first()) {
//            assertEquals(this.id, nyOpplysning.id)
//            assertEquals(this.kilde, nyOpplysning.kilde)
//            assertEquals(this.opplysningType, nyOpplysning.opplysningType)
//            assertEquals(this.meta, nyOpplysning.meta)
//            assertEquals(this.kilde, nyOpplysning.kilde)
//            assertEquals(this.attestering, nyOpplysning.attestering)
//            assertEquals(this.fnr!!.value, nyOpplysning.fnr!!.value)
//            assertEquals(this.opplysning, nyOpplysning.opplysning)
//        }
//    }
//
//    @Test
//    fun `Ny saksopplysning sendes til grunnlag`() {
//        val melding =
//            JsonMessage
//                .newMessage(
//                    mapOf(
//                        EventNames.NY_OPPLYSNING.lagParMedEventNameKey(),
//                        OPPLYSNING_KEY to listOf(nyOpplysning),
//                        // UTEN fnr for å trigge lagring til saksnivå
//                        SAK_ID_KEY to 1,
//                        BEHANDLING_ID_KEY to UUID.randomUUID(),
//                    ),
//                ).toJson()
//
//        inspector.sendTestMessage(melding)
//
//        val opplysningSlot = slot<List<Grunnlagsopplysning<JsonNode>>>()
//
//        verify { grunnlagKlientMock.lagreNyeSaksopplysninger(any(), any(), capture(opplysningSlot)) }
//
//        with(opplysningSlot.captured.first()) {
//            assertEquals(this.id, nyOpplysning.id)
//            assertEquals(this.kilde, nyOpplysning.kilde)
//            assertEquals(this.opplysningType, nyOpplysning.opplysningType)
//            assertEquals(this.meta, nyOpplysning.meta)
//            assertEquals(this.kilde, nyOpplysning.kilde)
//            assertEquals(this.attestering, nyOpplysning.attestering)
//            assertEquals(this.fnr!!.value, nyOpplysning.fnr!!.value)
//            assertEquals(this.opplysning, nyOpplysning.opplysning)
//        }
//    }
}
