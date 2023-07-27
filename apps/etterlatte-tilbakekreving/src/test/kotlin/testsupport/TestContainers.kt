package no.nav.etterlatte.testsupport

import no.nav.etterlatte.libs.database.POSTGRES_VERSION
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.PostgreSQLContainer

object TestContainers {

    val postgreSQLContainer = PostgreSQLContainer<Nothing>("postgres:$POSTGRES_VERSION")

    val ibmMQContainer = GenericContainer<Nothing>("ibmcom/mq").apply {
        withEnv("LICENSE", "accept")
        withEnv("MQ_QMGR_NAME", "QM1")
        withExposedPorts(1414)
    }
}