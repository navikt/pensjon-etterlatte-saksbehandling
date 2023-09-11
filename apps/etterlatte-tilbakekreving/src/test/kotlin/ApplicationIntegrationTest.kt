package no.nav.etterlatte

import io.mockk.spyk
import io.mockk.verify
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.testsupport.TestContainers
import no.nav.etterlatte.testsupport.TestRapid
import no.nav.etterlatte.testsupport.readFile
import no.nav.etterlatte.tilbakekreving.ChipsetCheck
import no.nav.etterlatte.tilbakekreving.TilbakekrevingService
import no.nav.etterlatte.tilbakekreving.config.ApplicationContext
import no.nav.etterlatte.tilbakekreving.config.ApplicationProperties
import no.nav.etterlatte.tilbakekreving.config.JmsConnectionFactory
import no.nav.etterlatte.tilbakekreving.kravgrunnlag.TilbakekrevingEvent
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.condition.DisabledIf
import org.testcontainers.junit.jupiter.Container
import javax.sql.DataSource

@DisabledIf(value = ChipsetCheck.erM1EllerM2)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ApplicationIntegrationTest {

    @Container
    private val postgreSQLContainer = TestContainers.postgreSQLContainer

    @Container
    private val ibmMQContainer = TestContainers.ibmMQContainer

    private lateinit var connectionFactory: JmsConnectionFactory
    private lateinit var dataSource: DataSource
    private lateinit var tilbakekrevingService: TilbakekrevingService
    private val rapidsConnection: TestRapid = spyk(TestRapid())

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
            serviceUserPassword = "passw0rd"
        )

        ApplicationContext(applicationProperties).also {
            connectionFactory = it.jmsConnectionFactory
            dataSource = it.dataSource
            it.tilbakekrevingService = spyk(it.tilbakekrevingService)
                .also { tks -> tilbakekrevingService = tks }

            rapidApplication(it, rapidsConnection).start()
        }
    }

    @Test
    @Disabled("har skrudd av consumer inntil vi får på plass kø")
    fun `skal motta og lagre kravgrunnlag`() {
        sendKravgrunnlagsmeldingFraOppdrag()

        verify(timeout = TIMEOUT, exactly = 1) {
            tilbakekrevingService.opprettTilbakekrevingFraKravgrunnlag(any(), any())

            rapidsConnection.publish(
                any(),
                match {
                    objectMapper.readValue(it, TilbakekrevingEvent::class.java).run {
                        this.event == "TILBAKEKREVING:MOTTATT_KRAVGRUNNLAG"
                    }
                }
            )
        }
    }

    @AfterEach
    fun afterEach() {
        dataSource.connection.use {
            it.prepareStatement(""" TRUNCATE tilbakekreving CASCADE""").execute()
        }
    }

    @AfterAll
    fun afterAll() {
        rapidsConnection.stop()
        ibmMQContainer.stop()
        postgreSQLContainer.stop()
    }

    private fun sendKravgrunnlagsmeldingFraOppdrag() {
        connectionFactory.connection().createSession().use { session ->
            val producer = session.createProducer(session.createQueue("DEV.QUEUE.1"))
            val kravgrunnlag = readFile("/kravgrunnlag.xml")
            val message = session.createTextMessage(kravgrunnlag)
            producer.send(message)
        }
    }

    companion object {
        const val TIMEOUT: Long = 5000
    }
}