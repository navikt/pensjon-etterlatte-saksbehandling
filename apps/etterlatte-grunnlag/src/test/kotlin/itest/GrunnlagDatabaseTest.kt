package no.nav.etterlatte.itest

import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.treeToValue
import grunnlag.ADRESSE_DEFAULT
import grunnlag.SOEKER_FOEDSELSNUMMER
import grunnlag.kilde
import lagGrunnlagsopplysning
import no.nav.etterlatte.DataSourceBuilder
import no.nav.etterlatte.grunnlag.OpplysningDao
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype.AVDOED_PDL_V1
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype.BOSTEDSADRESSE
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype.SOEKER_PDL_V1
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype.SOEKNAD_MOTTATT_DATO
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype.SOESKEN_I_BEREGNINGEN
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.SoeknadMottattDato
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.periode.Periode
import no.nav.etterlatte.libs.common.person.Foedselsnummer
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.libs.common.toJsonNode
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import java.time.LocalDate
import java.time.Month
import java.time.YearMonth
import java.util.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class GrunnlagDaoIntegrationTest {
    @Container
    private val postgreSQLContainer = PostgreSQLContainer<Nothing>("postgres:14")

    private lateinit var opplysningRepo: OpplysningDao
    private lateinit var dsb: DataSourceBuilder

    @BeforeAll
    fun beforeAll() {
        postgreSQLContainer.start()
        postgreSQLContainer.withUrlParam("user", postgreSQLContainer.username)
        postgreSQLContainer.withUrlParam("password", postgreSQLContainer.password)

        dsb = DataSourceBuilder(mapOf("DB_JDBC_URL" to postgreSQLContainer.jdbcUrl))
        dsb.migrate()
        opplysningRepo = OpplysningDao(dsb.dataSource)
    }

    @AfterAll
    fun afterAll() {
        postgreSQLContainer.stop()
    }

    @AfterEach
    fun afterEach() {
        dsb.dataSource.connection.prepareStatement(""" TRUNCATE grunnlagshendelse""").execute()
    }

    @Test
    fun `Legge til opplysning og hente den etterpaa`() {
        val datoMottat = LocalDate.of(2022, Month.MAY, 3).atStartOfDay()
        val uuid = UUID.randomUUID()

        lagGrunnlagsopplysning(
            opplysningstype = SOEKNAD_MOTTATT_DATO,
            verdi = objectMapper.valueToTree(SoeknadMottattDato(datoMottat)) as ObjectNode
        ).also { opplysningRepo.leggOpplysningTilGrunnlag(1, it) }
        lagGrunnlagsopplysning(SOEKNAD_MOTTATT_DATO).also { opplysningRepo.leggOpplysningTilGrunnlag(2, it) }
        lagGrunnlagsopplysning(
            opplysningstype = SOEKNAD_MOTTATT_DATO,
            uuid = uuid
        ).also { opplysningRepo.leggOpplysningTilGrunnlag(2, it) }

        assertEquals(1, opplysningRepo.finnHendelserIGrunnlag(1).size)
        assertEquals(1, opplysningRepo.finnHendelserIGrunnlag(2).size)
        assertEquals(uuid, opplysningRepo.finnHendelserIGrunnlag(2).first().opplysning.id)
        assertEquals(
            datoMottat,
            opplysningRepo.finnHendelserIGrunnlag(1)
                .first().opplysning.let { objectMapper.treeToValue<SoeknadMottattDato>(it.opplysning) }.mottattDato
        )
    }

    @Test
    fun `kan legge til en personopplysning`() {
        val uuid = UUID.randomUUID()
        val fnr = Foedselsnummer.of("13082819155")

        lagGrunnlagsopplysning(
            uuid = uuid,
            opplysningstype = Opplysningstype.NAVN,
            verdi = objectMapper.valueToTree("Per"),
            fnr = fnr
        ).also { opplysningRepo.leggOpplysningTilGrunnlag(1, it, fnr) }

        assertEquals(1, opplysningRepo.finnHendelserIGrunnlag(1).size)
        assertEquals(uuid, opplysningRepo.finnHendelserIGrunnlag(1).first().opplysning.id)
        assertEquals(
            fnr,
            opplysningRepo.finnHendelserIGrunnlag(1).first().opplysning.fnr
        )
    }

    @Test
    fun `skal liste alle nyeste opplysningene knyttet til en sak`() {
        lagGrunnlagsopplysning(SOEKNAD_MOTTATT_DATO).also { opplysningRepo.leggOpplysningTilGrunnlag(1, it) }
        lagGrunnlagsopplysning(SOEKNAD_MOTTATT_DATO).also { opplysningRepo.leggOpplysningTilGrunnlag(2, it) }
        lagGrunnlagsopplysning(SOEKER_PDL_V1).also { opplysningRepo.leggOpplysningTilGrunnlag(2, it) }

        assertEquals(1, opplysningRepo.finnHendelserIGrunnlag(1).size)
        assertEquals(2, opplysningRepo.finnHendelserIGrunnlag(2).size)
    }

    @Test
    fun `Skal hente opplysning fra nyeste hendelse basert paa sakId og opplysningType`() {
        val uuid = UUID.randomUUID()

        lagGrunnlagsopplysning(AVDOED_PDL_V1).also { opplysningRepo.leggOpplysningTilGrunnlag(33, it) }
        lagGrunnlagsopplysning(AVDOED_PDL_V1, uuid = uuid).also { opplysningRepo.leggOpplysningTilGrunnlag(33, it) }

        assertEquals(uuid, opplysningRepo.finnNyesteGrunnlag(33, Opplysningstype.AVDOED_PDL_V1)?.opplysning?.id)
        // Skal håndtere at opplysning ikke finnes
        assertEquals(null, opplysningRepo.finnNyesteGrunnlag(0L, Opplysningstype.AVDOED_PDL_V1))
    }

    @Test
    fun `Ny opplysning skal overskrive gammel, new way`() {
        val uuid = UUID.randomUUID()

        lagGrunnlagsopplysning(SOEKNAD_MOTTATT_DATO).also { opplysningRepo.leggOpplysningTilGrunnlag(2, it) }
        lagGrunnlagsopplysning(SOEKNAD_MOTTATT_DATO, uuid = uuid).also {
            opplysningRepo.leggOpplysningTilGrunnlag(
                2,
                it
            )
        }

        opplysningRepo.finnHendelserIGrunnlag(2).also {
            assertEquals(1, it.size)
            assertEquals(uuid, it.first().opplysning.id)
        }
    }

    @Test
    fun `Det er mulig å slette alle opplysninger i en sak`() {
        lagGrunnlagsopplysning(SOEKNAD_MOTTATT_DATO).also { opplysningRepo.leggOpplysningTilGrunnlag(2, it) }
        lagGrunnlagsopplysning(SOESKEN_I_BEREGNINGEN).also {
            opplysningRepo.leggOpplysningTilGrunnlag(
                2,
                it
            )
        }

        opplysningRepo.slettAlleOpplysningerISak(2)
        opplysningRepo.finnHendelserIGrunnlag(2).also {
            assertEquals(0, it.size)
        }
    }

    @Test
    fun `kan lage og hente periodiserte opplysninger`() {
        val opplysning = Grunnlagsopplysning(
            id = UUID.randomUUID(),
            kilde = kilde,
            opplysningType = BOSTEDSADRESSE,
            meta = objectMapper.createObjectNode(),
            opplysning = ADRESSE_DEFAULT.first().toJsonNode(),
            attestering = null,
            fnr = SOEKER_FOEDSELSNUMMER,
            periode = Periode(
                fom = YearMonth.of(2022, 1),
                tom = YearMonth.of(2022, 12)
            )
        )

        opplysningRepo.leggOpplysningTilGrunnlag(1, opplysning, SOEKER_FOEDSELSNUMMER)
        val expected = OpplysningDao.GrunnlagHendelse(opplysning, 1, 1).toJson()
        val actual = opplysningRepo.hentAlleGrunnlagForSak(1).first().toJson()

        assertEquals(expected, actual)
    }
}