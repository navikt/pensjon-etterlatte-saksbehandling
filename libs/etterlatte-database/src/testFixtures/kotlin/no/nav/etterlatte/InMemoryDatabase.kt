package no.nav.etterlatte

import no.nav.etterlatte.libs.database.DataSourceBuilder
import no.nav.etterlatte.libs.database.migrate
import org.testcontainers.containers.PostgreSQLContainer
import javax.sql.DataSource

fun opprettInMemoryDatabase(postgreSQLContainer: PostgreSQLContainer<Nothing>): InMemoryDatabase {
    postgreSQLContainer.start()
    postgreSQLContainer.withUrlParam("user", postgreSQLContainer.username)
    postgreSQLContainer.withUrlParam("password", postgreSQLContainer.password)
    val datasource =
        DataSourceBuilder.createDataSource(
            postgreSQLContainer.jdbcUrl,
            postgreSQLContainer.username,
            postgreSQLContainer.password,
        ).also { it.migrate() }
    return InMemoryDatabase(postgreSQLContainer, datasource)
}

data class InMemoryDatabase(
    val sqlContainer: PostgreSQLContainer<Nothing>,
    val dataSource: DataSource,
)
