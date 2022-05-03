package no.nav.etterlatte

import com.typesafe.config.ConfigFactory
import io.ktor.auth.*
import io.ktor.config.*
import no.nav.etterlatte.grunnlag.*
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
import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.helse.rapids_rivers.RapidsConnection

interface BeanFactory {
    fun datasourceBuilder(): DataSourceBuilder
    fun sakService(): SakService
    fun grunnlagsService(): GrunnlagService
    fun tokenValidering(): Authentication.Configuration.()->Unit
    fun sakDao(): SakDao
    fun grunnlagDao(): GrunnlagDao
    fun opplysningDao(): OpplysningDao
    fun rapid():RapidsConnection
    fun grunnlagHendelser(): GrunnlagHendelser
    fun behandlingsFactory(): GrunnlagFactory
}

abstract class CommonFactory: BeanFactory{
    internal var cache: MutableMap<Any, Any> = mutableMapOf()
    internal inline fun <reified T> cached(creator: ()->T):T{
        if(!cache.containsKey(T::class.java)){
            cache[T::class.java] = creator() as Any
        }
        return cache[T::class.java] as T

    }

    override fun grunnlagHendelser(): GrunnlagHendelser {
        return cached { GrunnlagHendelser(rapid(), GrunnlagFactory(grunnlagDao(), opplysningDao()), datasourceBuilder().dataSource) }
    }
    override fun behandlingsFactory(): GrunnlagFactory {
        return cached { GrunnlagFactory(grunnlagDao(), opplysningDao()) }
    }

    override fun sakService(): SakService = RealSakService(sakDao())
    override fun grunnlagsService(): GrunnlagService = RealGrunnlagService(grunnlagDao(), opplysningDao(), GrunnlagFactory(grunnlagDao(), opplysningDao())) //grunnlagHendelser().nyHendelse)
    override fun sakDao(): SakDao = SakDao{ databaseContext().activeTx()}
    override fun grunnlagDao(): GrunnlagDao = GrunnlagDao { databaseContext().activeTx() }
    override fun opplysningDao(): OpplysningDao = OpplysningDao { databaseContext().activeTx() }
}

class EnvBasedBeanFactory(val env: Map<String, String>): CommonFactory() {

    override fun datasourceBuilder(): DataSourceBuilder = cached { DataSourceBuilder(env) }
    override fun tokenValidering(): Authentication.Configuration.() -> Unit = { tokenValidationSupport(config = HoconApplicationConfig(ConfigFactory.load())) }
    override fun rapid(): RapidsConnection {
        return RapidApplication.create(env)

    }

    private fun kafkaConfig(): KafkaConfig = GcpKafkaConfig(
        bootstrapServers = env.getValue("KAFKA_BROKERS"),
        truststore = env.getValue("KAFKA_TRUSTSTORE_PATH"),
        truststorePassword = env.getValue("KAFKA_CREDSTORE_PASSWORD"),
        keystoreLocation = env.getValue("KAFKA_KEYSTORE_PATH"),
        keystorePassword = env.getValue("KAFKA_CREDSTORE_PASSWORD")
    )
}