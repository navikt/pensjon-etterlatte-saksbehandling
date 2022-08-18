package no.nav.etterlatte.testsupport

import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.PostgreSQLContainer

object TestContainers {

    val postgreSQLContainer = PostgreSQLContainer<Nothing>("postgres:14")

    val ibmMQContainer = GenericContainer<Nothing>("ibmcom/mq").apply {
        withEnv("LICENSE", "accept")
        withEnv("MQ_QMGR_NAME", "QM1")
        withExposedPorts(1414)
    }
}