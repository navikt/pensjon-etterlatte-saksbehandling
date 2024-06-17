package no.nav.etterlatte.migrering

import no.nav.etterlatte.libs.database.DataSourceBuilder

internal class ApplicationContext {
    val dataSource = DataSourceBuilder.createDataSource(System.getenv())

    val pesysRepository = PesysRepository(dataSource)
}
