package migrering

import io.ktor.server.testing.testApplication
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.rapidsandrivers.EVENT_NAME_KEY
import no.nav.etterlatte.libs.database.DataSourceBuilder
import no.nav.etterlatte.libs.database.KotliqueryRepository
import no.nav.etterlatte.libs.database.migrate
import no.nav.etterlatte.migrering.Migrering
import no.nav.etterlatte.migrering.PesysRepository
import no.nav.etterlatte.migrering.Pesyssak
import no.nav.etterlatte.rapidsandrivers.migrering.Enhet
import no.nav.etterlatte.rapidsandrivers.migrering.MigreringRequest
import no.nav.etterlatte.rapidsandrivers.migrering.Migreringshendelser
import no.nav.etterlatte.rapidsandrivers.migrering.PesysId
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import java.time.LocalDateTime
import java.util.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MigreringIntegrationTest {

    @Container
    private val postgreSQLContainer = PostgreSQLContainer<Nothing>("postgres:15")

    @BeforeAll
    fun start() {
        postgreSQLContainer.start()
        postgreSQLContainer.withUrlParam("user", postgreSQLContainer.username)
        postgreSQLContainer.withUrlParam("password", postgreSQLContainer.password)
    }

    @AfterAll
    fun stop() = postgreSQLContainer.stop()

    @Test
    fun `skal sende migreringsmelding for hver enkelt sak`() {
        val datasource = DataSourceBuilder.createDataSource(
            postgreSQLContainer.jdbcUrl,
            postgreSQLContainer.username,
            postgreSQLContainer.password
        ).also { it.migrate() }
        testApplication {
            val repository = PesysRepository(KotliqueryRepository(datasource))
            val syntetiskFnr = "19078504903"
            val sakInn = Pesyssak(
                UUID.randomUUID(),
                PesysId("4"),
                Enhet("4808"),
                Folkeregisteridentifikator.of(syntetiskFnr),
                LocalDateTime.now(),
                Persongalleri(syntetiskFnr, "innsender", emptyList(), emptyList(), emptyList())
            )
            repository.lagrePesyssak(sakInn)
            val inspector = TestRapid()
                .apply { Migrering(this, repository) }

            val melding = JsonMessage.newMessage(
                mapOf(EVENT_NAME_KEY to Migreringshendelser.START_MIGRERING)
            )
            inspector.sendTestMessage(melding.toJson())

            val melding1 = inspector.inspektør.message(0)

            val request = objectMapper.readValue(melding1.get("request").asText(), MigreringRequest::class.java)
            Assertions.assertEquals(PesysId("4"), request.pesysId)
            Assertions.assertEquals(sakInn.folkeregisteridentifikator, request.fnr)
        }
    }
}