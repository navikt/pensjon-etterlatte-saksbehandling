package no.nav.etterlatte.itest

import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.auth.*
import io.ktor.http.*
import io.ktor.server.testing.*
import no.nav.common.KafkaEnvironment
import no.nav.etterlatte.*
import no.nav.etterlatte.behandling.*
import no.nav.etterlatte.kafka.KafkaConfig
import no.nav.etterlatte.kafka.KafkaProdusent
import no.nav.etterlatte.kafka.KafkaProdusentImpl
import no.nav.etterlatte.libs.common.behandling.BehandlingListe
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.gyldigSoeknad.GyldighetsResultat
import no.nav.etterlatte.libs.common.gyldigSoeknad.GyldighetsTyper
import no.nav.etterlatte.libs.common.gyldigSoeknad.VurdertGyldighet
import no.nav.etterlatte.libs.common.vikaar.VurderingsResultat
import no.nav.etterlatte.sak.Sak
import no.nav.etterlatte.sikkerhet.tokenTestSupportAcceptsAllTokens
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.testcontainers.containers.PostgreSQLContainer
import java.time.LocalDateTime
import java.util.*

class ApplicationTest {
    @Test
    fun verdikjedetest() {
        val topicname = "test_topic"
        val embeddedKafkaEnvironment = KafkaEnvironment(
            autoStart = false,
            noOfBrokers = 1,
            topicInfos = listOf(KafkaEnvironment.TopicInfo(name = topicname, partitions = 1)),
            withSchemaRegistry = false,
            withSecurity = false,
            brokerConfigOverrides = Properties().apply {
                this["auto.leader.rebalance.enable"] = "false"
                this["group.initial.rebalance.delay.ms"] =
                    "1" //Avoid waiting for new consumers to join group before first rebalancing (default 3000ms)
            }
        )

        embeddedKafkaEnvironment.start()

        val kafkaConfig = EmbeddedKafkaConfig(embeddedKafkaEnvironment.brokersURL.substringAfterLast("/"))
        val consumer = KafkaConsumer(kafkaConfig.consumer(), StringDeserializer(), StringDeserializer())
        consumer.subscribe(mutableListOf(topicname))

        val fnr = "123"
        val postgreSQLContainer = PostgreSQLContainer<Nothing>("postgres:12")
        postgreSQLContainer.start()
        postgreSQLContainer.withUrlParam("user", postgreSQLContainer.username)
        postgreSQLContainer.withUrlParam("password", postgreSQLContainer.password)

        var behandlingOpprettet: UUID? = null
        val beans = TestBeanFactory(postgreSQLContainer.jdbcUrl, kafkaConfig, topicname)
        withTestApplication({
            module(beans)
        }) {

            handleRequest(HttpMethod.Get, "/saker/123") {
                addAuthSaksbehandler()
            }.apply {
                assertEquals(HttpStatusCode.NotFound, response.status())
            }

            val sak: Sak = handleRequest(HttpMethod.Get, "/personer/$fnr/saker/BP") {
                addAuthSaksbehandler()
            }.apply {
                assertEquals(HttpStatusCode.OK, response.status())
            }.response.content!!.let { objectMapper.readValue(it) }

            handleRequest(HttpMethod.Get, "/saker/${sak.id}") {
                addAuthSaksbehandler()
            }.also {
                assertEquals(HttpStatusCode.OK, it.response.status())
                val lestSak: Sak = objectMapper.readValue(it.response.content!!)
                assertEquals("123", lestSak.ident)
                assertEquals("BP", lestSak.sakType)

            }

            val behandlingId = handleRequest(HttpMethod.Post, "/behandlinger") {
                addAuthServiceBruker()
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(
                    no.nav.etterlatte.libs.common.objectMapper.writeValueAsString(
                        BehandlingsBehov(
                            1,
                            Persongalleri("søker", "innsender", emptyList(), emptyList(), emptyList()),
                            LocalDateTime.now().toString()
                        )
                    )
                )
            }.let {
                assertEquals(HttpStatusCode.OK, it.response.status())
                UUID.fromString(it.response.content)
            }
            behandlingOpprettet = behandlingId

            handleRequest(HttpMethod.Get, "/sak/1/behandlinger") {
                addAuthSaksbehandler()
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            }.let {
                assertEquals(HttpStatusCode.OK, it.response.status())
                objectMapper.readValue<BehandlingListe>(it.response.content!!)
            }.also {
                assertEquals(1, it.behandlinger.size)
            }

            handleRequest(HttpMethod.Post, "/behandlinger/$behandlingId/gyldigfremsatt") {
                addAuthSaksbehandler()
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(
                    no.nav.etterlatte.libs.common.objectMapper.writeValueAsString(
                        GyldighetsResultat(
                            VurderingsResultat.OPPFYLT,
                            listOf(
                                VurdertGyldighet(
                                    GyldighetsTyper.INNSENDER_ER_FORELDER,
                                    VurderingsResultat.OPPFYLT,
                                    "innsenderFnr"
                                )
                            ),
                            LocalDateTime.now()
                        )
                    )
                )
            }.let {
                assertEquals(HttpStatusCode.OK, it.response.status())
            }

            handleRequest(HttpMethod.Get, "/behandlinger/$behandlingId") {
                addAuthSaksbehandler()
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            }.also {
                assertEquals(HttpStatusCode.OK, it.response.status())
                val behandling: Behandling = (objectMapper.readValue(it.response.content!!))
                assertNotNull(behandling.id)
                assertEquals("innsender", behandling.innsender)
                assertEquals(VurderingsResultat.OPPFYLT, behandling.gyldighetsproeving?.resultat)

            }
        }

        beans.behandlingHendelser().nyHendelse.close()

        assertNotNull(behandlingOpprettet)
        consumer.poll(2000).also { assertFalse(it.isEmpty) }.forEach {
            assertEquals(behandlingOpprettet.toString(), it.key())
            assertEquals("BEHANDLING:OPPRETTET", objectMapper.readTree(it.value())["@event"].textValue())
        }

        postgreSQLContainer.stop()
    }
}

val clientCredentialTokenMedKanSetteKildeRolle =
    "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJhenVyZSIsInN1YiI6ImVuLWFwcCIsIm9pZCI6ImVuLWFwcCIsIm5hbWUiOiJKb2huIERvZSIsImlhdCI6MTUxNjIzOTAyMiwiTkFWaWRlbnQiOiJTYWtzYmVoYW5kbGVyMDEiLCJyb2xlcyI6WyJrYW4tc2V0dGUta2lsZGUiXX0.2ftwnoZiUfUa_J6WUkqj_Wdugb0CnvVXsEs-JYnQw_g"
val saksbehandlerToken =
    "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJhenVyZSIsInN1YiI6ImF6dXJlLWlkIGZvciBzYWtzYmVoYW5kbGVyIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyLCJOQVZpZGVudCI6IlNha3NiZWhhbmRsZXIwMSJ9.271mDij4YsO4Kk8w8AvX5BXxlEA8U-UAOtdG1Ix_kQY"

fun TestApplicationRequest.addAuthSaksbehandler() {
    addHeader(HttpHeaders.Authorization, "Bearer $saksbehandlerToken")
}

fun TestApplicationRequest.addAuthServiceBruker() {
    addHeader(HttpHeaders.Authorization, "Bearer $clientCredentialTokenMedKanSetteKildeRolle")
}


class TestBeanFactory(
    private val jdbcUrl: String,
    private val kafkaConfig: KafkaConfig,
    private val rapidtopic: String
) : CommonFactory() {
    override fun datasourceBuilder(): DataSourceBuilder = DataSourceBuilder(mapOf("DB_JDBC_URL" to jdbcUrl))
    override fun tokenValidering(): Authentication.Configuration.() -> Unit =
        Authentication.Configuration::tokenTestSupportAcceptsAllTokens

    override fun rapid(): KafkaProdusent<String, String> = KafkaProdusentImpl(
        KafkaProducer(kafkaConfig.producerConfig(), StringSerializer(), StringSerializer()),
        rapidtopic
    )
}

class EmbeddedKafkaConfig(
    private val bootstrapServers: String,
) : KafkaConfig {
    override fun producerConfig() = kafkaBaseConfig().apply {
        put(ProducerConfig.ACKS_CONFIG, "1")
        put(ProducerConfig.CLIENT_ID_CONFIG, "etterlatte-post-til-kafka")
        put(ProducerConfig.LINGER_MS_CONFIG, "0")
        put(ProducerConfig.RETRIES_CONFIG, Int.MAX_VALUE)
        put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, "1")
    }

    private fun kafkaBaseConfig() = Properties().apply {
        put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers)
    }

    fun consumer() = kafkaBaseConfig().apply {
        put(ConsumerConfig.GROUP_ID_CONFIG, "etterlatte-post-til-kafka")
        put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
        put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false)
    }
}

