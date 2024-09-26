package no.nav.etterlatte.grunnlag.itest

import com.fasterxml.jackson.databind.JsonNode
import io.mockk.mockk
import no.nav.etterlatte.behandling.sakId1
import no.nav.etterlatte.grunnlag.GrunnlagDbExtension
import no.nav.etterlatte.grunnlag.OpplysningDao
import no.nav.etterlatte.grunnlag.RealGrunnlagService
import no.nav.etterlatte.grunnlag.klienter.PdlTjenesterKlientImpl
import no.nav.etterlatte.grunnlag.rivers.GrunnlagHendelserRiver
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.rapidsandrivers.BEHOV_NAME_KEY
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
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import java.util.UUID
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(GrunnlagDbExtension::class)
internal class RapidTest(
    private val dataSource: DataSource,
) {
    private lateinit var opplysningRepo: OpplysningDao
    private lateinit var grunnlagService: RealGrunnlagService
    private lateinit var inspector: TestRapid

    @BeforeAll
    fun beforeAll() {
        opplysningRepo = OpplysningDao(dataSource)
        val pdlTjenesterKlientImpl = mockk<PdlTjenesterKlientImpl>()
        grunnlagService = RealGrunnlagService(pdlTjenesterKlientImpl, opplysningRepo, mockk())
        inspector =
            TestRapid().apply {
                GrunnlagHendelserRiver(this, grunnlagService)
            }
    }

    private val fnr = AVDOED_FOEDSELSNUMMER
    private val tidspunkt = Tidspunkt.now()
    private val kilde = Grunnlagsopplysning.Pdl(tidspunkt, null, null)
    private val nyOpplysning =
        Grunnlagsopplysning(
            id = statiskUuid,
            kilde = kilde,
            opplysningType = Opplysningstype.NAVN,
            meta = objectMapper.createObjectNode(),
            opplysning = "Ola".toJsonNode(),
            attestering = null,
            fnr = fnr,
        )

    @Nested
    inner class NyOpplysning {
        private val melding =
            JsonMessage
                .newMessage(
                    mapOf(
                        EventNames.NY_OPPLYSNING.lagParMedEventNameKey(),
                        OPPLYSNING_KEY to listOf(nyOpplysning),
                        FNR_KEY to fnr,
                        SAK_ID_KEY to 1,
                        BEHANDLING_ID_KEY to UUID.randomUUID(),
                    ),
                ).toJson()

        @Test
        fun `ny enkeltopplysning lagres i databasen med riktige verdier`() {
            assertOpplysningBlirLagret(melding = melding, expectedOpplysning = nyOpplysning)
        }
    }

    @Nested
    inner class Opplysningsbehov {
        private val melding =
            JsonMessage
                .newMessage(
                    mapOf(
                        BEHOV_NAME_KEY to Opplysningstype.SOEKER_PDL_V1,
                        OPPLYSNING_KEY to listOf(nyOpplysning),
                        FNR_KEY to fnr,
                        SAK_ID_KEY to 1,
                        BEHANDLING_ID_KEY to UUID.randomUUID(),
                    ),
                ).toJson()

        @Test
        fun `ny enkeltopplysning lagres i databasen med riktige verdier`() {
            assertOpplysningBlirLagret(melding = melding, expectedOpplysning = nyOpplysning)
        }
    }

    private fun assertOpplysningBlirLagret(
        melding: String,
        expectedOpplysning: Grunnlagsopplysning<JsonNode>,
    ) {
        inspector.sendTestMessage(melding)
        val grunnlagshendelse = opplysningRepo.finnHendelserIGrunnlag(sakId1).first()

        Assertions.assertEquals(grunnlagshendelse.sakId, sakId1)
        Assertions.assertEquals(grunnlagshendelse.hendelseNummer, 1)

        with(grunnlagshendelse.opplysning) {
            Assertions.assertEquals(this.id, expectedOpplysning.id)
            Assertions.assertEquals(this.kilde, expectedOpplysning.kilde)
            Assertions.assertEquals(this.opplysningType, expectedOpplysning.opplysningType)
            Assertions.assertEquals(this.meta, expectedOpplysning.meta)
            Assertions.assertEquals(this.kilde, expectedOpplysning.kilde)
            Assertions.assertEquals(this.attestering, expectedOpplysning.attestering)
            Assertions.assertEquals(this.fnr!!.value, expectedOpplysning.fnr!!.value)
            Assertions.assertEquals(this.opplysning, expectedOpplysning.opplysning)
        }
    }
}
