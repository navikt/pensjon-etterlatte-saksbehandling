package behandling.migrering

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.jackson.jackson
import io.ktor.server.testing.testApplication
import no.nav.etterlatte.BehandlingIntegrationTest
import no.nav.etterlatte.funksjonsbrytere.FellesFeatureToggle
import no.nav.etterlatte.module
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class MigreringRoutesTest : BehandlingIntegrationTest() {

    @BeforeAll
    fun start() = startServer()

    @AfterAll
    fun shutdown() = afterAll()

    @Test
    fun `migrering oppretter sak og behandling`() {
        testApplication {
            environment {
                config = hoconApplicationConfig
            }
            val client = createClient {
                install(ContentNegotiation) {
                    jackson { registerModule(JavaTimeModule()) }
                }
            }
            application { module(beanFactory) }

            Assertions.assertTrue(
                beanFactory.featureToggleService().isEnabled(FellesFeatureToggle.NoOperationToggle, false)
            )
        }
    }
}