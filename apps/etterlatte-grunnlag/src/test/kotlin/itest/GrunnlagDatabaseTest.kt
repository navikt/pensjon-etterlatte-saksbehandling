package no.nav.etterlatte.itest

import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.treeToValue
import no.nav.etterlatte.DataSourceBuilder
import no.nav.etterlatte.grunnlag.OpplysningDao
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstyper
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.SoeknadMottattDato
import no.nav.etterlatte.libs.common.objectMapper
import org.junit.jupiter.api.*
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import java.time.Instant
import java.time.LocalDate
import java.time.Month
import java.util.*
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class GrunnlagDaoIntegrationTest {
    @Container
    private val postgreSQLContainer = PostgreSQLContainer<Nothing>("postgres:14")


    private lateinit var dataSource: DataSource

    @BeforeAll
    fun beforeAll() {
        postgreSQLContainer.start()
        postgreSQLContainer.withUrlParam("user", postgreSQLContainer.username)
        postgreSQLContainer.withUrlParam("password", postgreSQLContainer.password)

        val dsb = DataSourceBuilder(mapOf("DB_JDBC_URL" to postgreSQLContainer.jdbcUrl))
        dataSource = dsb.dataSource

        dsb.migrate()
    }

    @AfterAll
    fun afterAll() {
        postgreSQLContainer.stop()
    }

    @Test
    fun `Legge til opplysning og hente den etterpå`() {
        val connection = dataSource.connection
        val opplysningRepo = OpplysningDao { connection }
        val datoMottat = LocalDate.of(2022,Month.MAY,3).atStartOfDay()

        Grunnlagsopplysning(
            UUID.randomUUID(),
            Grunnlagsopplysning.Pdl("pdl", Instant.now(), null),
            Opplysningstyper.SOEKNAD_MOTTATT_DATO,
            objectMapper.createObjectNode(),
            objectMapper.valueToTree(SoeknadMottattDato(datoMottat)) as ObjectNode
        ).also { opplysningRepo.leggOpplysningTilGrunnlag(1, it) }
        Grunnlagsopplysning(
            UUID.randomUUID(),
            Grunnlagsopplysning.Pdl("pdl", Instant.now(), null),
            Opplysningstyper.SOEKNAD_MOTTATT_DATO,
            objectMapper.createObjectNode(),
            objectMapper.createObjectNode()
        ).also { opplysningRepo.leggOpplysningTilGrunnlag(2, it) }
        val uuid =UUID.randomUUID()
        Grunnlagsopplysning(
            uuid,
            Grunnlagsopplysning.Pdl("pdl", Instant.now(), null),
            Opplysningstyper.SOEKNAD_MOTTATT_DATO,
            objectMapper.createObjectNode(),
            objectMapper.createObjectNode()
        ).also {
            opplysningRepo.leggOpplysningTilGrunnlag(2, it) }


        Assertions.assertEquals(1, opplysningRepo.finnHendelserIGrunnlag(1).size)
        Assertions.assertEquals(1, opplysningRepo.finnHendelserIGrunnlag(2).size)
        Assertions.assertEquals(uuid, opplysningRepo.finnHendelserIGrunnlag(2).first().opplysning.id)
        Assertions.assertEquals(datoMottat, opplysningRepo.finnHendelserIGrunnlag(1).first().opplysning.let { objectMapper.treeToValue<SoeknadMottattDato>(it.opplysning) }?.mottattDato )

        connection.close()
    }

    @Test
    fun `Ny opplysning skal overskrive gammel, new way`() {
        val connection = dataSource.connection
        val opplysningRepo = OpplysningDao { connection }

        Grunnlagsopplysning(
            UUID.randomUUID(),
            Grunnlagsopplysning.Pdl("pdl", Instant.now(), null),
            Opplysningstyper.SOEKNAD_MOTTATT_DATO,
            objectMapper.createObjectNode(),
            objectMapper.createObjectNode()
        ).also { opplysningRepo.leggOpplysningTilGrunnlag(2, it) }
        val uuid =UUID.randomUUID()
        Grunnlagsopplysning(
            uuid,
            Grunnlagsopplysning.Pdl("pdl", Instant.now(), null),
            Opplysningstyper.SOEKNAD_MOTTATT_DATO,
            objectMapper.createObjectNode(),
            objectMapper.createObjectNode()
        ).also {
            opplysningRepo.leggOpplysningTilGrunnlag(2, it) }

        opplysningRepo.finnHendelserIGrunnlag(2).also {
            Assertions.assertEquals(1, it.size)
            Assertions.assertEquals(uuid, it.first().opplysning.id)
        }

        connection.close()
    }

}
