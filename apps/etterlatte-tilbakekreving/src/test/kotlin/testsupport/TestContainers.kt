package no.nav.etterlatte.testsupport

import no.nav.etterlatte.libs.database.POSTGRES_VERSION
import org.testcontainers.containers.PostgreSQLContainer

object TestContainers {
    val postgreSQLContainer = PostgreSQLContainer<Nothing>("postgres:$POSTGRES_VERSION")
}
