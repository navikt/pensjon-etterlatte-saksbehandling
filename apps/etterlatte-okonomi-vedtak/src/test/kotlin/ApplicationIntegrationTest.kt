package no.nav.etterlatte

import io.mockk.every
import io.mockk.spyk
import no.nav.etterlatte.config.ApplicationContext
import no.nav.etterlatte.util.TestContainers
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.testcontainers.junit.jupiter.Container

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ApplicationIntegrationTest {

    @Container private val postgreSQLContainer = TestContainers.postgreSQLContainer
    @Container private val ibmMQContainer = TestContainers.ibmMQContainer

    private lateinit var applicationContext: ApplicationContext
    private lateinit var rapidsConnection: TestRapid

    @BeforeAll
    fun beforeAll() {
        postgreSQLContainer.start()
        ibmMQContainer.start()

        val env = mapOf(
            // DB
            "DB_HOST" to postgreSQLContainer.host,
            "DB_PORT" to postgreSQLContainer.firstMappedPort.toString(),
            "DB_DATABASE" to postgreSQLContainer.databaseName,
            "DB_USERNAME" to postgreSQLContainer.username,
            "DB_PASSWORD" to postgreSQLContainer.password,

            // MQ
            "OPPDRAG_SEND_MQ_NAME" to "DEV.QUEUE.1",
            "OPPDRAG_KVITTERING_MQ_NAME" to "DEV.QUEUE.1",
            "OPPDRAG_MQ_HOSTNAME" to ibmMQContainer.host,
            "OPPDRAG_MQ_PORT" to ibmMQContainer.firstMappedPort.toString(),
            "OPPDRAG_MQ_CHANNEL" to "DEV.ADMIN.SVRCONN",
            "OPPDRAG_MQ_MANAGER" to "QM1",

            // Service user
            "srvuser" to "admin",
            "srvpwd" to "passw0rd",
        )

        rapidsConnection = TestRapid()
        applicationContext = spyk(ApplicationContext(env)).apply {
            every { rapidsConnection() } returns rapidsConnection
        }
    }

    @AfterAll
    fun afterAll() {
        ibmMQContainer.stop()
        postgreSQLContainer.stop()
    }

    @Test
    fun `skal starte opp applikasjon, motta vedtak og sende til MQ`() {
        bootstrap(applicationContext)

        rapidsConnection.sendTestMessage(FATTET_VEDTAK)

        // TODO gjøre noen sjekker her
    }

    companion object {
        val FATTET_VEDTAK = readFile("/vedtak.json")
    }

}