package no.nav.etterlatte.itest

import com.fasterxml.jackson.databind.JsonNode
import io.mockk.mockk
import lagGrunnlagsopplysning
import no.nav.etterlatte.grunnlag.BehandlingHendelser
import no.nav.etterlatte.grunnlag.GrunnlagHendelser
import no.nav.etterlatte.grunnlag.OpplysningDao
import no.nav.etterlatte.grunnlag.RealGrunnlagService
import no.nav.etterlatte.klienter.BehandlingKlient
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.person.Foedselsnummer
import no.nav.etterlatte.libs.common.person.PersonRolle
import no.nav.etterlatte.libs.common.rapidsandrivers.EVENT_NAME_KEY
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.libs.common.toJsonNode
import no.nav.etterlatte.libs.database.DataSourceBuilder
import no.nav.etterlatte.libs.database.migrate
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
import java.time.Instant
import java.util.UUID

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class RapidTest {
    @Container
    private val postgreSQLContainer = PostgreSQLContainer<Nothing>("postgres:14")

    private lateinit var opplysningRepo: OpplysningDao
    private lateinit var grunnlagService: RealGrunnlagService
    private lateinit var inspector: TestRapid
    private val behandlingKlient = mockk<BehandlingKlient>()
    private val sendToRapid: (String, UUID) -> Unit = mockk(relaxed = true)

    @BeforeAll
    fun beforeAll() {
        postgreSQLContainer.start()
        postgreSQLContainer.withUrlParam("user", postgreSQLContainer.username)
        postgreSQLContainer.withUrlParam("password", postgreSQLContainer.password)

        val dataSource = DataSourceBuilder.createDataSource(
            jdbcUrl = postgreSQLContainer.jdbcUrl,
            username = postgreSQLContainer.username,
            password = postgreSQLContainer.password
        )
        dataSource.migrate()

        opplysningRepo = OpplysningDao(dataSource)
        grunnlagService = RealGrunnlagService(opplysningRepo, sendToRapid, behandlingKlient, mockk())
        inspector = TestRapid().apply {
            GrunnlagHendelser(this, grunnlagService)
            BehandlingHendelser(this)
        }
    }

    @AfterAll
    fun afterAll() {
        postgreSQLContainer.stop()
    }

    private val fnr = Foedselsnummer.of("18057404783")
    private val tidspunkt = Instant.now()
    private val kilde = Grunnlagsopplysning.Pdl("pdl", tidspunkt, null, null)
    private val nyOpplysning = Grunnlagsopplysning(
        id = statiskUuid,
        kilde = kilde,
        opplysningType = Opplysningstype.NAVN,
        meta = objectMapper.createObjectNode(),
        opplysning = "Ola".toJsonNode(),
        attestering = null,
        fnr = fnr
    )

    @Nested
    inner class NyOpplysning {
        private val melding = JsonMessage.newMessage(
            mapOf(
                "@event_name" to "OPPLYSNING:NY",
                "opplysning" to listOf(nyOpplysning),
                "fnr" to fnr,
                "sakId" to 1
            )
        ).toJson()

        @Test
        fun `ny enkeltopplysning sender ut melding om at grunnlaget er endret`() {
            inspector.sendTestMessage(melding)
            Assertions.assertEquals(
                "GRUNNLAG:GRUNNLAGENDRET",
                inspector.inspektør.message(0)[EVENT_NAME_KEY].textValue()
            )
        }

        @Test
        fun `ny enkeltopplysning lagres i databasen med riktige verdier`() {
            assertOpplysningBlirLagret(melding = melding, expectedOpplysning = nyOpplysning)
        }
    }

    @Nested
    inner class Opplysningsbehov {
        private val melding = JsonMessage.newMessage(
            mapOf(
                "@behov" to Opplysningstype.SOEKER_PDL_V1,
                "opplysning" to listOf(nyOpplysning),
                "fnr" to fnr,
                "sakId" to 1
            )
        ).toJson()

        @Test
        fun `svar på opplysningsbehov sender ut melding om at grunnlaget er endret`() {
            inspector.sendTestMessage(melding)
            Assertions.assertEquals(
                "GRUNNLAG:GRUNNLAGENDRET",
                inspector.inspektør.message(0)[EVENT_NAME_KEY].textValue()
            )
        }

        @Test
        fun `ny enkeltopplysning lagres i databasen med riktige verdier`() {
            assertOpplysningBlirLagret(melding = melding, expectedOpplysning = nyOpplysning)
        }
    }

    @Test
    fun `melding om ny opplysning sender ut melding om at grunnlaget er endret`() {
        val personRolleOpplysning = lagGrunnlagsopplysning(
            opplysningstype = Opplysningstype.PERSONROLLE,
            kilde = kilde,
            uuid = statiskUuid,
            verdi = PersonRolle.BARN.toJsonNode(),
            fnr = fnr
        )
        val nyOpplysningMelding = JsonMessage.newMessage(
            mapOf(
                "@event_name" to "OPPLYSNING:NY",
                "opplysning" to listOf(
                    nyOpplysning,
                    personRolleOpplysning
                ),
                "fnr" to fnr,
                "sakId" to 1
            )
        ).toJson()

        inspector.sendTestMessage(nyOpplysningMelding)

        val packet = inspector.inspektør.message(0)

        Assertions.assertEquals("GRUNNLAG:GRUNNLAGENDRET".toJson(), packet[EVENT_NAME_KEY].toJson())
        Assertions.assertEquals(1, packet["sakId"].longValue())
    }

    private fun assertOpplysningBlirLagret(melding: String, expectedOpplysning: Grunnlagsopplysning<JsonNode>) {
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