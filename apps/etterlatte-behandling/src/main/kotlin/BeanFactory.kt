package no.nav.etterlatte

import com.typesafe.config.ConfigFactory
import io.ktor.auth.*
import io.ktor.config.*
import no.nav.etterlatte.behandling.*
import no.nav.etterlatte.sak.RealSakService
import no.nav.etterlatte.sak.SakDao
import no.nav.etterlatte.sak.SakService
import no.nav.security.token.support.ktor.tokenValidationSupport

interface BeanFactory {
    fun datasourceBuilder(): DataSourceBuilder
    fun sakService(): SakService
    fun vilkaarKlient(): VilkaarKlient
    fun behandlingService(): BehandlingService
    fun tokenValidering(): Authentication.Configuration.()->Unit
    fun sakDao(): SakDao
    fun behandlingDao(): BehandlingDao
    fun opplysningDao(): OpplysningDao
}

abstract class CommonFactory: BeanFactory{
    override fun sakService(): SakService = RealSakService(sakDao())
    override fun behandlingService(): BehandlingService = RealBehandlingService(behandlingDao(), opplysningDao(), vilkaarKlient())
    override fun sakDao(): SakDao = SakDao{ databaseContext().activeTx()}
    override fun behandlingDao(): BehandlingDao = BehandlingDao { databaseContext().activeTx() }
    override fun opplysningDao(): OpplysningDao = OpplysningDao { databaseContext().activeTx() }
}

class EnvBasedBeanFactory(val env: Map<String, String>): CommonFactory() {
    override fun datasourceBuilder(): DataSourceBuilder = DataSourceBuilder(env)
    override fun vilkaarKlient(): VilkaarKlient = KtorVilkarClient("http://etterlatte-vilkaar")
    override fun tokenValidering(): Authentication.Configuration.() -> Unit = { tokenValidationSupport(config = HoconApplicationConfig(ConfigFactory.load())) }
}