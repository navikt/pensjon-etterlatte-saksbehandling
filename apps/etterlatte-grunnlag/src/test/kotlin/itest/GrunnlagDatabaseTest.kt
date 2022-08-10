package no.nav.etterlatte.itest

import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.treeToValue
import no.nav.etterlatte.DataSourceBuilder
import no.nav.etterlatte.grunnlag.OpplysningDao
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstyper
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.SoeknadMottattDato
import no.nav.etterlatte.libs.common.objectMapper
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import java.time.Instant
import java.time.LocalDate
import java.time.Month
import java.util.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class GrunnlagDaoIntegrationTest {
    @Container
    private val postgreSQLContainer = PostgreSQLContainer<Nothing>("postgres:14")

    private lateinit var opplysningRepo: OpplysningDao

    @BeforeAll
    fun beforeAll() {
        postgreSQLContainer.start()
        postgreSQLContainer.withUrlParam("user", postgreSQLContainer.username)
        postgreSQLContainer.withUrlParam("password", postgreSQLContainer.password)

        val dsb = DataSourceBuilder(mapOf("DB_JDBC_URL" to postgreSQLContainer.jdbcUrl))
        dsb.migrate()
        opplysningRepo = OpplysningDao(dsb.dataSource)
    }

    @AfterAll
    fun afterAll() {
        postgreSQLContainer.stop()
    }

    @Test
    fun `Legge til opplysning og hente den etterpaa`() {
        val datoMottat = LocalDate.of(2022, Month.MAY, 3).atStartOfDay()

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
        val uuid = UUID.randomUUID()
        Grunnlagsopplysning(
            uuid,
            Grunnlagsopplysning.Pdl("pdl", Instant.now(), null),
            Opplysningstyper.SOEKNAD_MOTTATT_DATO,
            objectMapper.createObjectNode(),
            objectMapper.createObjectNode()
        ).also {
            opplysningRepo.leggOpplysningTilGrunnlag(2, it)
        }


        assertEquals(1, opplysningRepo.finnHendelserIGrunnlag(1).size)
        assertEquals(1, opplysningRepo.finnHendelserIGrunnlag(2).size)
        assertEquals(uuid, opplysningRepo.finnHendelserIGrunnlag(2).first().opplysning.id)
        assertEquals(datoMottat,
            opplysningRepo.finnHendelserIGrunnlag(1)
                .first().opplysning.let { objectMapper.treeToValue<SoeknadMottattDato>(it.opplysning) }.mottattDato
        )
    }

    @Test
    fun `Skal hente opplysning fra nyeste hendelse basert paa sakId og opplysningType`() {
        Grunnlagsopplysning(
            UUID.randomUUID(),
            Grunnlagsopplysning.Pdl("pdl", Instant.now(), null),
            Opplysningstyper.AVDOED_PDL_V1,
            objectMapper.createObjectNode(),
            objectMapper.createObjectNode()
        ).also { opplysningRepo.leggOpplysningTilGrunnlag(33, it) }
        val uuid = UUID.randomUUID()
        Grunnlagsopplysning(
            uuid,
            Grunnlagsopplysning.Pdl("pdl", Instant.now(), null),
            Opplysningstyper.AVDOED_PDL_V1,
            objectMapper.createObjectNode(),
            objectMapper.createObjectNode()
        ).also {
            opplysningRepo.leggOpplysningTilGrunnlag(33, it)
        }

        assertEquals(uuid, opplysningRepo.finnNyesteGrunnlag(33, Opplysningstyper.AVDOED_PDL_V1)?.opplysning?.id)
        // Skal håndtere at opplysning ikke finnes
        assertEquals(null, opplysningRepo.finnNyesteGrunnlag(0L, Opplysningstyper.AVDOED_PDL_V1))
    }

    @Test
    fun `Ny opplysning skal overskrive gammel, new way`() {
        Grunnlagsopplysning(
            UUID.randomUUID(),
            Grunnlagsopplysning.Pdl("pdl", Instant.now(), null),
            Opplysningstyper.SOEKNAD_MOTTATT_DATO,
            objectMapper.createObjectNode(),
            objectMapper.createObjectNode()
        ).also { opplysningRepo.leggOpplysningTilGrunnlag(2, it) }
        val uuid = UUID.randomUUID()
        Grunnlagsopplysning(
            uuid,
            Grunnlagsopplysning.Pdl("pdl", Instant.now(), null),
            Opplysningstyper.SOEKNAD_MOTTATT_DATO,
            objectMapper.createObjectNode(),
            objectMapper.createObjectNode()
        ).also {
            opplysningRepo.leggOpplysningTilGrunnlag(2, it)
        }

        opplysningRepo.finnHendelserIGrunnlag(2).also {
            assertEquals(1, it.size)
            assertEquals(uuid, it.first().opplysning.id)
        }
    }
}
