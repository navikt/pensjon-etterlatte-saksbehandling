package no.nav.etterlatte

import config.ApplicationContext
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

        rapidsConnection = TestRapid()
        applicationContext = ApplicationContext(mapOf(
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
            "srvuser" to "admin",
            "srvpwd" to "passw0rd",

        ), rapidsConnection)
    }

    @AfterAll
    fun afterAll() {
        rapidsConnection.stop()
        postgreSQLContainer.stop()
        ibmMQContainer.stop()
    }

    @Test
    fun `skal starte opp applikasjon, motta vedtak og sende til MQ`() {
        bootstrap(applicationContext)

        rapidsConnection.sendTestMessage(FATTET_VEDTAK)
    }

    companion object {
        val FATTET_VEDTAK = readFile("/vedtak.json")
    }

}