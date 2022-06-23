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
    fun rapid(): KafkaProdusent<String, String>
    fun behandlingHendelser(): BehandlingsHendelser
    fun behandlingsFactory(): BehandlingFactory


}

abstract class CommonFactory : BeanFactory {
    internal var cache: MutableMap<Any, Any> = mutableMapOf()
    internal inline fun <reified T> cached(creator: () -> T): T {
        if (!cache.containsKey(T::class.java)) {
            cache[T::class.java] = creator() as Any
        }
        return cache[T::class.java] as T

    }

    override fun behandlingHendelser(): BehandlingsHendelser {
        return cached {
            BehandlingsHendelser(
                rapid(),
                BehandlingFactory(behandlingDao()),
                datasourceBuilder().dataSource
            )
        }
    }

    override fun behandlingsFactory(): BehandlingFactory {
        return cached { BehandlingFactory(behandlingDao()) }
    }

    override fun sakService(): SakService = RealSakService(sakDao())
    override fun behandlingService(): BehandlingService = RealBehandlingService(
        behandlingDao(),
        BehandlingFactory(behandlingDao()),
        behandlingHendelser().nyHendelse
    )

    override fun sakDao(): SakDao = SakDao { databaseContext().activeTx() }
    override fun behandlingDao(): BehandlingDao = BehandlingDao { databaseContext().activeTx() }
}

class EnvBasedBeanFactory(val env: Map<String, String>) : CommonFactory() {
    override fun datasourceBuilder(): DataSourceBuilder = cached { DataSourceBuilder(env) }
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