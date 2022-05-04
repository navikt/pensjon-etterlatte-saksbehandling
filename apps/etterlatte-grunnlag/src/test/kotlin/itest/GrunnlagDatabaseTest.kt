package no.nav.etterlatte.itest

import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.treeToValue
import no.nav.etterlatte.DataSourceBuilder
import no.nav.etterlatte.grunnlag.OpplysningDao
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstyper
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.SoeknadMottattDato
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
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
        //val sakrepo = SakDao { connection }
        //val grunnlagRepo = GrunnlagDao { connection }
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

        Assertions.assertEquals(1, opplysningRepo.finnOpplysningerIGrunnlag(1).size)
        Assertions.assertEquals(datoMottat, opplysningRepo.finnOpplysningerIGrunnlag(1).first().opplysning.let { objectMapper.treeToValue<SoeknadMottattDato>(it) }?.mottattDato )

        connection.close()
    }

}
