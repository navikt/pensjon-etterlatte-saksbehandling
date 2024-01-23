package grunnlag.itest

import com.fasterxml.jackson.databind.JsonNode
import io.mockk.mockk
import no.nav.etterlatte.grunnlag.OpplysningDao
import no.nav.etterlatte.grunnlag.RealGrunnlagService
import no.nav.etterlatte.grunnlag.klienter.PdlTjenesterKlientImpl
import no.nav.etterlatte.grunnlag.rivers.GrunnlagHendelserRiver
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.rapidsandrivers.BEHOV_NAME_KEY
import no.nav.etterlatte.libs.common.rapidsandrivers.EVENT_NAME_KEY
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.toJsonNode
import no.nav.etterlatte.libs.database.DataSourceBuilder
import no.nav.etterlatte.libs.database.POSTGRES_VERSION
import no.nav.etterlatte.libs.database.migrate
import no.nav.etterlatte.libs.testdata.grunnlag.AVDOED_FOEDSELSNUMMER
import no.nav.etterlatte.libs.testdata.grunnlag.statiskUuid
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import rapidsandrivers.BEHANDLING_ID_KEY
import rapidsandrivers.FNR_KEY
import rapidsandrivers.NY_OPPLYSNING_KEY
import rapidsandrivers.OPPLYSNING_KEY
import rapidsandrivers.SAK_ID_KEY
import java.util.UUID

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class RapidTest {
    @Container
    private val postgreSQLContainer = PostgreSQLContainer<Nothing>("postgres:$POSTGRES_VERSION")

    private lateinit var opplysningRepo: OpplysningDao
    private lateinit var grunnlagService: RealGrunnlagService
    private lateinit var inspector: TestRapid

    @BeforeAll
    fun beforeAll() {
        postgreSQLContainer.start()
        postgreSQLContainer.withUrlParam("user", postgreSQLContainer.username)
        postgreSQLContainer.withUrlParam("password", postgreSQLContainer.password)

        val dataSource =
            DataSourceBuilder.createDataSource(
                jdbcUrl = postgreSQLContainer.jdbcUrl,
                username = postgreSQLContainer.username,
                password = postgreSQLContainer.password,
            )
        dataSource.migrate()

        opplysningRepo = OpplysningDao(dataSource)
        val pdlTjenesterKlientImpl = mockk<PdlTjenesterKlientImpl>()
        grunnlagService = RealGrunnlagService(pdlTjenesterKlientImpl, opplysningRepo, mockk(), mockk(), mockk())
        inspector =
            TestRapid().apply {
                GrunnlagHendelserRiver(this, grunnlagService)
            }
    }

    @AfterAll
    fun afterAll() {
        postgreSQLContainer.stop()
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
            JsonMessage.newMessage(
                mapOf(
                    EVENT_NAME_KEY to NY_OPPLYSNING_KEY,
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
            JsonMessage.newMessage(
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
        val grunnlagshendelse = opplysningRepo.finnHendelserIGrunnlag(1).first()

        Assertions.assertEquals(grunnlagshendelse.sakId, 1)
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
