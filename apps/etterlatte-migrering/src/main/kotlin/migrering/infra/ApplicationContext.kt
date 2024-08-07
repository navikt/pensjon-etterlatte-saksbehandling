package no.nav.etterlatte.migrering

import no.nav.etterlatte.libs.common.Miljoevariabler
import no.nav.etterlatte.libs.database.DataSourceBuilder

internal class ApplicationContext {
    val dataSource = DataSourceBuilder.createDataSource(Miljoevariabler.systemEnv())

    val pesysRepository = PesysRepository(dataSource)
}
