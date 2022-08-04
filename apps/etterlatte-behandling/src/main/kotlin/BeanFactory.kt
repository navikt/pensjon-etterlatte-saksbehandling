package no.nav.etterlatte

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
import no.nav.etterlatte.kafka.standardProducer
import no.nav.etterlatte.sak.RealSakService
import no.nav.etterlatte.sak.SakDao
import no.nav.etterlatte.sak.SakService

interface BeanFactory {
    fun datasourceBuilder(): DataSourceBuilder
    fun sakService(): SakService
    fun foerstegangsbehandlingService(): FoerstegangsbehandlingService
    fun revurderingService(): RevurderingService
    fun generellBehandlingService(): GenerellBehandlingService
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
            behandlingDao(),
            foerstegangsbehandlingFactory(),
            revurderingFactory(),
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

    override fun generellBehandlingService(): GenerellBehandlingService =
        RealGenerellBehandlingService(
            behandlingDao(),
            behandlingHendelser().nyHendelse,
            foerstegangsbehandlingFactory(),
            revurderingFactory(),
            hendelseDao()
        )

    override fun sakDao(): SakDao = SakDao { databaseContext().activeTx() }
    override fun behandlingDao(): BehandlingDao = BehandlingDao { databaseContext().activeTx() }
    override fun hendelseDao(): HendelseDao = HendelseDao { databaseContext().activeTx() }
}

class EnvBasedBeanFactory(val env: Map<String, String>) : CommonFactory() {
    private val datasourceBuilder: DataSourceBuilder by lazy { DataSourceBuilder(env) }
    override fun datasourceBuilder() = datasourceBuilder

    override fun rapid(): KafkaProdusent<String, String> {
        return kafkaConfig().standardProducer(env.getValue("KAFKA_RAPID_TOPIC"))
    }

    private fun kafkaConfig(): KafkaConfig = GcpKafkaConfig.fromEnv(env)
}