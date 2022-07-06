package no.nav.etterlatte

import com.typesafe.config.ConfigFactory
import io.ktor.auth.*
import io.ktor.config.*
import no.nav.etterlatte.behandling.*
import no.nav.etterlatte.kafka.GcpKafkaConfig
import no.nav.etterlatte.kafka.KafkaConfig
import no.nav.etterlatte.kafka.KafkaProdusent
import no.nav.etterlatte.kafka.KafkaProdusentImpl
import no.nav.etterlatte.sak.RealSakService
import no.nav.etterlatte.sak.SakDao
import no.nav.etterlatte.sak.SakService
import no.nav.security.token.support.ktor.tokenValidationSupport
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.common.serialization.StringSerializer

interface BeanFactory {
    fun datasourceBuilder(): DataSourceBuilder
    fun sakService(): SakService
    fun behandlingService(): BehandlingService
    fun tokenValidering(): Authentication.Configuration.() -> Unit
    fun sakDao(): SakDao
    fun behandlingDao(): BehandlingDao
    fun hendelseDao(): HendelseDao
    fun rapid(): KafkaProdusent<String, String>
    fun behandlingHendelser(): BehandlingsHendelser
    fun behandlingsFactory(): BehandlingFactory

}

abstract class CommonFactory : BeanFactory {
    private val behandlingsHendelser: BehandlingsHendelser by lazy { BehandlingsHendelser(
        rapid(),
        behandlingsFactory(),
        datasourceBuilder().dataSource
    ) }
    private val behandlingsFactory: BehandlingFactory by lazy { BehandlingFactory(behandlingDao(), hendelseDao()) }

    override fun behandlingHendelser(): BehandlingsHendelser {
        return behandlingsHendelser
    }

    override fun behandlingsFactory(): BehandlingFactory {
        return behandlingsFactory
    }

    override fun sakService(): SakService = RealSakService(sakDao())
    override fun behandlingService(): BehandlingService = RealBehandlingService(
        behandlingDao(),
        behandlingsFactory(),
        behandlingHendelser().nyHendelse
    )

    override fun sakDao(): SakDao = SakDao { databaseContext().activeTx() }
    override fun behandlingDao(): BehandlingDao = BehandlingDao { databaseContext().activeTx() }
    override fun hendelseDao(): HendelseDao = HendelseDao { databaseContext().activeTx() }
}

class EnvBasedBeanFactory(val env: Map<String, String>) : CommonFactory() {
    private val datasourceBuilder: DataSourceBuilder by lazy { DataSourceBuilder(env) }
    override fun datasourceBuilder() = datasourceBuilder
    override fun tokenValidering(): Authentication.Configuration.() -> Unit =
        { tokenValidationSupport(config = HoconApplicationConfig(ConfigFactory.load())) }

    override fun rapid(): KafkaProdusent<String, String> {
        return KafkaProdusentImpl(
            KafkaProducer(kafkaConfig().producerConfig(), StringSerializer(), StringSerializer()),
            env.getValue("KAFKA_RAPID_TOPIC")
        )
    }

    private fun kafkaConfig(): KafkaConfig = GcpKafkaConfig(
        bootstrapServers = env.getValue("KAFKA_BROKERS"),
        truststore = env.getValue("KAFKA_TRUSTSTORE_PATH"),
        truststorePassword = env.getValue("KAFKA_CREDSTORE_PASSWORD"),
        keystoreLocation = env.getValue("KAFKA_KEYSTORE_PATH"),
        keystorePassword = env.getValue("KAFKA_CREDSTORE_PASSWORD")
    )
}