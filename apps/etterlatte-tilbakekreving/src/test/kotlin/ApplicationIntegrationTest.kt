package no.nav.etterlatte

import io.mockk.spyk
import io.mockk.verify
import no.nav.etterlatte.tilbakekreving.TilbakekrevingService
import no.nav.etterlatte.tilbakekreving.config.ApplicationContext
import no.nav.etterlatte.tilbakekreving.config.ApplicationProperties
import no.nav.etterlatte.tilbakekreving.config.JmsConnectionFactory
import no.nav.tilbakekreving.kravgrunnlag.detalj.v1.DetaljertKravgrunnlagDto
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.testcontainers.junit.jupiter.Container
import javax.sql.DataSource


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ApplicationIntegrationTest {

    @Container
    private val postgreSQLContainer = TestContainers.postgreSQLContainer

    @Container
    private val ibmMQContainer = TestContainers.ibmMQContainer

    private lateinit var connectionFactory: JmsConnectionFactory
    private lateinit var dataSource: DataSource
    private lateinit var tilbakekrevingService: TilbakekrevingService

    @BeforeAll
    fun beforeAll() {
        postgreSQLContainer.start()
        ibmMQContainer.start()

        val applicationProperties = ApplicationProperties(
            dbName = postgreSQLContainer.databaseName,
            dbHost = postgreSQLContainer.host,
            dbPort = postgreSQLContainer.firstMappedPort,
            dbUsername = postgreSQLContainer.username,
            dbPassword = postgreSQLContainer.password,
            mqHost = ibmMQContainer.host,
            mqPort = ibmMQContainer.firstMappedPort,
            mqQueueManager = "QM1",
            mqChannel = "DEV.ADMIN.SVRCONN",
            mqKravgrunnlagQueue = "DEV.QUEUE.1",
            serviceUserUsername = "admin",
            serviceUserPassword = "passw0rd",
        )

        ApplicationContext(applicationProperties).also {
            connectionFactory = it.jmsConnectionFactory
            dataSource = it.dataSource
            tilbakekrevingService = spyk(it.tilbakekrevingService)
        }.also {
            startApplication(it)
        }
    }

    @Test
    fun `skal motta kravgrunnlag men feile fordi grunnlaget er null`() {
        sendKravgrunnlagsmeldingFraOppdrag(null)

        verify(exactly = 0) {
            tilbakekrevingService.lagreKravgrunnlag(any())
        }
    }


    @AfterEach
    fun afterEach() {
        /*using(sessionOf(dataSource)) {
            it.run(queryOf("TRUNCATE utbetaling CASCADE").asExecute)
        }*/
    }

    @AfterAll
    fun afterAll() {
        connectionFactory.stop()
        ibmMQContainer.stop()
        postgreSQLContainer.stop()
    }


    private fun sendKravgrunnlagsmeldingFraOppdrag(kravgrunnlag: DetaljertKravgrunnlagDto?) {
        connectionFactory.connection().createSession().use { session ->
            val producer = session.createProducer(session.createQueue("DEV.QUEUE.1"))
            val message = session.createTextMessage("test")
            producer.send(message)
        }
    }

    companion object {
        const val TIMEOUT: Long = 5000
    }
}