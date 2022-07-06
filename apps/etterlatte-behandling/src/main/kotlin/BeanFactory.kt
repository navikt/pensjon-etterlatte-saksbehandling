package no.nav.etterlatte

import com.typesafe.config.ConfigFactory
import io.ktor.auth.Authentication
import io.ktor.config.HoconApplicationConfig
import no.nav.etterlatte.behandling.BehandlingDao
import no.nav.etterlatte.behandling.BehandlingsHendelser
import no.nav.etterlatte.behandling.GenerellBehandlingService
import no.nav.etterlatte.behandling.HendelseDao
import no.nav.etterlatte.behandling.RealGenerellBehandlingService
import no.nav.etterlatte.behandling.foerstegangsbehandling.FoerstegangsbehandlingFactory
import no.nav.etterlatte.behandling.foerstegangsbehandling.FoerstegangsbehandlingService
import no.nav.etterlatte.behandling.foerstegangsbehandling.RealFoerstegangsbehandlingService
import no.nav.etterlatte.behandling.revurdering.RealRevurderingService
import no.nav.etterlatte.behandling.revurdering.RevurderingFactory
import no.nav.etterlatte.behandling.revurdering.RevurderingService
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
    fun foerstegangsbehandlingService(): FoerstegangsbehandlingService
    fun revurderingService(): RevurderingService
    fun generellBehandlingService(): GenerellBehandlingService
    fun tokenValidering(): Authentication.Configuration.() -> Unit
    fun sakDao(): SakDao
    fun behandlingDao(): BehandlingDao
    fun hendelseDao(): HendelseDao
    fun rapid(): KafkaProdusent<String, String>
    fun behandlingHendelser(): BehandlingsHendelser
    fun foerstegangsbehandlingFactory(): FoerstegangsbehandlingFactory

    fun revurderingFactory(): RevurderingFactory

}

abstract class CommonFactory : BeanFactory {
    private val behandlingsHendelser: BehandlingsHendelser by lazy {
        BehandlingsHendelser(
            rapid(),
            foerstegangsbehandlingFactory(),
            datasourceBuilder().dataSource
        )
    }
    private val foerstegangsbehandlingFactory: FoerstegangsbehandlingFactory by lazy {
        FoerstegangsbehandlingFactory(
            behandlingDao(),
            hendelseDao()
        )
    }

    private val revurderingFactory: RevurderingFactory by lazy {
        RevurderingFactory(behandlingDao(), hendelseDao())
    }

    override fun behandlingHendelser(): BehandlingsHendelser {
        return behandlingsHendelser
    }

    override fun foerstegangsbehandlingFactory(): FoerstegangsbehandlingFactory {
        return foerstegangsbehandlingFactory
    }

    override fun revurderingFactory(): RevurderingFactory {
        return revurderingFactory
    }

    override fun sakService(): SakService = RealSakService(sakDao())

    override fun foerstegangsbehandlingService(): RealFoerstegangsbehandlingService =
        RealFoerstegangsbehandlingService(
            behandlingDao(),
            foerstegangsbehandlingFactory(),
            behandlingHendelser().nyHendelse
        )

    override fun revurderingService(): RealRevurderingService =
        RealRevurderingService(
            behandlingDao(),
            revurderingFactory(),
            behandlingHendelser().nyHendelse
        )

    override fun generellBehandlingService(): GenerellBehandlingService = RealGenerellBehandlingService(
        behandlingDao(),
        behandlingHendelser().nyHendelse,
        foerstegangsbehandlingFactory(),
        revurderingFactory()
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