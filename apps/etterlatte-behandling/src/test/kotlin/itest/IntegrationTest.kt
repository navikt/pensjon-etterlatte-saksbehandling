package no.nav.etterlatte.itest

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.auth.*
import io.ktor.server.testing.*
import no.nav.common.KafkaEnvironment
import no.nav.etterlatte.CommonFactory
import no.nav.etterlatte.DataSourceBuilder
import no.nav.etterlatte.behandling.BehandlingsBehov
import no.nav.etterlatte.behandling.HendelseDao
import no.nav.etterlatte.behandling.VedtakHendelse
import no.nav.etterlatte.behandling.objectMapper
import no.nav.etterlatte.kafka.KafkaConfig
import no.nav.etterlatte.kafka.KafkaProdusent
import no.nav.etterlatte.kafka.KafkaProdusentImpl
import no.nav.etterlatte.libs.common.behandling.BehandlingListe
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.gyldigSoeknad.GyldighetsResultat
import no.nav.etterlatte.libs.common.gyldigSoeknad.GyldighetsTyper
import no.nav.etterlatte.libs.common.gyldigSoeknad.VurdertGyldighet
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.vikaar.VurderingsResultat
import no.nav.etterlatte.module
import no.nav.etterlatte.oppgave.OppgaveListeDto
import no.nav.etterlatte.sak.Sak
import no.nav.etterlatte.sikkerhet.tokenTestSupportAcceptsAllTokens
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
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

        testApplication {

            val client = createClient {
                install(ContentNegotiation) {
                    jackson{
                        registerModule(JavaTimeModule())
                    }
                }
            }
            install(Authentication){
                tokenTestSupportAcceptsAllTokens()
            }
            application { module(beans) }
            client.get("/saker/123"){
                addAuthSaksbehandler()
            }.apply {
                assertEquals(HttpStatusCode.NotFound, status)
            }
            val sak: Sak = client.get("/personer/$fnr/saker/BP") {
                addAuthSaksbehandler()
            }.apply {
                assertEquals(HttpStatusCode.OK, status)
            }.body()

            client.get( "/saker/${sak.id}") {
                addAuthSaksbehandler()
            }.also {
                assertEquals(HttpStatusCode.OK, it.status)
                val lestSak: Sak = it.body()
                assertEquals("123", lestSak.ident)
                assertEquals("BP", lestSak.sakType)

            }

            val behandlingId = client.post( "/behandlinger") {
                addAuthServiceBruker()
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(
                        BehandlingsBehov(
                            1,
                            Persongalleri("søker", "innsender", emptyList(), emptyList(), emptyList()),
                            LocalDateTime.now().toString()
                        )

                )
            }.let {
                assertEquals(HttpStatusCode.OK, it.status)
                UUID.fromString(it.body())
            }
            behandlingOpprettet = behandlingId

            client.get( "/sak/1/behandlinger") {
                addAuthSaksbehandler()
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            }.let {
                assertEquals(HttpStatusCode.OK, it.status)
                it.body<BehandlingListe>()
            }.also {
                assertEquals(1, it.behandlinger.size)
            }

            client.post( "/behandlinger/$behandlingId/gyldigfremsatt") {
                addAuthSaksbehandler()
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(
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

            }.let {
                assertEquals(HttpStatusCode.OK, it.status)
            }

            client.get( "/behandlinger/$behandlingId") {
                addAuthSaksbehandler()
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            }.also {
                assertEquals(HttpStatusCode.OK, it.status)
                val behandling: DetaljertBehandling = it.body()
                assertNotNull(behandling.id)
                assertEquals("innsender", behandling.innsender)
                assertEquals(VurderingsResultat.OPPFYLT, behandling.gyldighetsproeving?.resultat)

            }
            client.post( "/behandlinger/$behandlingId/hendelser/vedtak/FATTET") {
                addAuthSaksbehandler()
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(
                        VedtakHendelse(
                            12L,
                            "Saksbehandlier",
                            Tidspunkt.now(),
                            null,
                            null
                        )
                    )
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            }.also {
                assertEquals(HttpStatusCode.OK, it.status)
            }

            client.get( "/behandlinger/$behandlingId") {
                addAuthSaksbehandler()
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            }.also {
                assertEquals(HttpStatusCode.OK, it.status)
                val behandling: DetaljertBehandling = it.body()
                assertNotNull(behandling.id)
                assertEquals("FATTET_VEDTAK", behandling.status?.name)

            }

            client.get( "/oppgaver") {
                addAuthAttesterer()
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            }.also {
                assertEquals(HttpStatusCode.OK, it.status)
                val oppgaver: OppgaveListeDto = it.body()
                assertEquals(1, oppgaver.oppgaver.size)
                assertEquals(behandlingId, oppgaver.oppgaver.first().behandlingId)
            }
        }

        beans.behandlingHendelser().nyHendelse.close()

        assertNotNull(behandlingOpprettet)
        consumer.poll(2000).also { assertFalse(it.isEmpty) }.forEach {
            assertEquals(behandlingOpprettet.toString(), it.key())
            assertEquals("BEHANDLING:OPPRETTET", objectMapper.readTree(it.value())["@event"].textValue())
        }
        beans.datasourceBuilder().dataSource.connection.use {
            HendelseDao { it }.finnHendelserIBehandling(behandlingOpprettet!!).also { println(it) }
        }




        postgreSQLContainer.stop()
    }
}

val clientCredentialTokenMedKanSetteKildeRolle =
    "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJhenVyZSIsInN1YiI6ImVuLWFwcCIsIm9pZCI6ImVuLWFwcCIsIm5hbWUiOiJKb2huIERvZSIsImlhdCI6MTUxNjIzOTAyMiwiTkFWaWRlbnQiOiJTYWtzYmVoYW5kbGVyMDEiLCJyb2xlcyI6WyJrYW4tc2V0dGUta2lsZGUiXX0.2ftwnoZiUfUa_J6WUkqj_Wdugb0CnvVXsEs-JYnQw_g"
val saksbehandlerToken =
    "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJhenVyZSIsInN1YiI6ImF6dXJlLWlkIGZvciBzYWtzYmVoYW5kbGVyIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyLCJOQVZpZGVudCI6IlNha3NiZWhhbmRsZXIwMSJ9.271mDij4YsO4Kk8w8AvX5BXxlEA8U-UAOtdG1Ix_kQY"
val attestererToken =
    "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJhenVyZSIsInN1YiI6ImF6dXJlLWlkIGZvciBzYWtzYmVoYW5kbGVyIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyLCJOQVZpZGVudCI6IlNha3NiZWhhbmRsZXIwMSIsImdyb3VwcyI6WyIwYWYzOTU1Zi1kZjg1LTRlYjAtYjViMi00NWJmMmM4YWViOWUiLCI2M2Y0NmY3NC04NGE4LTRkMWMtODdhOC03ODUzMmFiM2FlNjAiXX0.YzF4IXwaolgOCODNwkEKn43iZbwHpQuSmQObQm0co-A"

fun HttpRequestBuilder.addAuthSaksbehandler() {
    header(HttpHeaders.Authorization, "Bearer $saksbehandlerToken")
}
fun HttpRequestBuilder.addAuthAttesterer() {
    header(HttpHeaders.Authorization, "Bearer $attestererToken")
}

fun HttpRequestBuilder.addAuthServiceBruker() {
    header(HttpHeaders.Authorization, "Bearer $clientCredentialTokenMedKanSetteKildeRolle")
}


class TestBeanFactory(
    private val jdbcUrl: String,
    private val kafkaConfig: KafkaConfig,
    private val rapidtopic: String
) : CommonFactory() {
    override fun datasourceBuilder(): DataSourceBuilder = DataSourceBuilder(mapOf("DB_JDBC_URL" to jdbcUrl))

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

