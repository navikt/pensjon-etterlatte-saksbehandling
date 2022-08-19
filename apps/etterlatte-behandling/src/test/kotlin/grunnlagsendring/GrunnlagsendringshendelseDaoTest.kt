package no.nav.etterlatte.grunnlagsendring

import no.nav.etterlatte.DataSourceBuilder
import no.nav.etterlatte.grunnlagsendringshendelse
import no.nav.etterlatte.grunnlagsinformasjonDoedshendelse
import no.nav.etterlatte.sak.SakDao
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertAll
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.*
import javax.sql.DataSource


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class GrunnlagsendringshendelseDaoTest {

    @Container
    private val postgreSQLContainer = PostgreSQLContainer<Nothing>("postgres:12")

    private lateinit var dataSource: DataSource
    lateinit var sakRepo: SakDao
    lateinit var grunnlagsendringshendelsesRepo: GrunnlagsendringshendelseDao

    @BeforeAll
    fun beforeAll() {
        postgreSQLContainer.start()
        postgreSQLContainer.withUrlParam("user", postgreSQLContainer.username)
        postgreSQLContainer.withUrlParam("password", postgreSQLContainer.password)

        val dsb = DataSourceBuilder(mapOf("DB_JDBC_URL" to postgreSQLContainer.jdbcUrl))
        dataSource = dsb.dataSource

        dsb.migrate()

        val connection = dataSource.connection
        sakRepo = SakDao { connection }
        grunnlagsendringshendelsesRepo = GrunnlagsendringshendelseDao { connection }
    }


    @AfterEach
    fun afterEach() {
        dataSource.connection.use {
            it.prepareStatement("TRUNCATE grunnlagsendringshendelse CASCADE;").execute()
        }
    }

    @AfterAll
    fun afterAll() {
        postgreSQLContainer.stop()
    }

    @Test
    fun `skal lagre grunnlagsendringshendelse i database og hente ut hendelsen fra databasen`() {
        val uuid = UUID.randomUUID()
        val sakid = sakRepo.opprettSak("1234", "BP").id
        val grunnlagsinformasjon = grunnlagsinformasjonDoedshendelse()
        val hendelse = grunnlagsendringshendelse(
            id = uuid,
            sakId = sakid,
            data = grunnlagsinformasjon,
            opprettet = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS)
        )
        grunnlagsendringshendelsesRepo.opprettGrunnlagsendringshendelse(hendelse)
        val hendelseFraDatabase = grunnlagsendringshendelsesRepo.hentGrunnlagsendringshendelse(uuid)

        assertAll(
            "grunnlagsendringshendelse lagres korrekt i database",
            { assertEquals(hendelse.id, hendelseFraDatabase!!.id) },
            { assertEquals(hendelse.sakId, hendelseFraDatabase!!.sakId) },
            { assertEquals(hendelse.type, hendelseFraDatabase!!.type) },
            { assertEquals(hendelse.opprettet, hendelseFraDatabase!!.opprettet) },
            { assertEquals(hendelse.data, hendelseFraDatabase!!.data) },
        )
    }

    @Test
    fun `skal hente grunnlagsendringshendelser som er eldre enn en time`() {
        val sakid = sakRepo.opprettSak("1234", "BP").id
        listOf(
            grunnlagsendringshendelse(
                sakId = sakid, opprettet = LocalDateTime.now(), data = grunnlagsinformasjonDoedshendelse()
            ),
            grunnlagsendringshendelse(
                sakId = sakid,
                opprettet = LocalDateTime.now().minusMinutes(30),
                data = grunnlagsinformasjonDoedshendelse()
            ),
            grunnlagsendringshendelse(
                sakId = sakid, opprettet = LocalDateTime.now().minusHours(1), data = grunnlagsinformasjonDoedshendelse()
            ),
            grunnlagsendringshendelse(
                sakId = sakid, opprettet = LocalDateTime.now().minusDays(4), data = grunnlagsinformasjonDoedshendelse()
            ),
            grunnlagsendringshendelse(
                sakId = sakid, opprettet = LocalDateTime.now().minusYears(1), data = grunnlagsinformasjonDoedshendelse()
            ),
        ).forEach {
            grunnlagsendringshendelsesRepo.opprettGrunnlagsendringshendelse(it)
        }
        val hendelserEldreEnn1Time = grunnlagsendringshendelsesRepo.hentIkkeVurderteGrunnlagsendringshendelserEldreEnn(
            60, GrunnlagsendringsType.SOEKER_DOED
        )
        assertAll("henter kun grunnlagsendringshendelser som er eldre enn 1 time",
            { assertEquals(3, hendelserEldreEnn1Time.size) },
            { assertTrue { hendelserEldreEnn1Time.all { it.opprettet <= LocalDateTime.now().minusHours(1) } } })
    }

    @Test
    fun `oppdaterGrunnlagsendringStatuForType skal oppdatere status for grunnlagsendringshendelser`() {

        val sak1 = sakRepo.opprettSak("1234", "BP").id
        val sak2 = sakRepo.opprettSak("4321", "BP").id

        listOf(
            grunnlagsendringshendelse(
                sakId = sak1, data = grunnlagsinformasjonDoedshendelse()
            ),
            grunnlagsendringshendelse(
                sakId = sak1, data = grunnlagsinformasjonDoedshendelse()
            ),
            grunnlagsendringshendelse(
                sakId = sak2, data = grunnlagsinformasjonDoedshendelse()
            ),
        ).forEach {
            grunnlagsendringshendelsesRepo.opprettGrunnlagsendringshendelse(it)
        }

        val hendelserFoerOppdatertStatus = grunnlagsendringshendelsesRepo.hentAlleGrunnlagsendringshendelser()
        grunnlagsendringshendelsesRepo.oppdaterGrunnlagsendringStatusForType(
            saker = listOf(sak1, sak2),
            foerStatus = GrunnlagsendringStatus.IKKE_VURDERT,
            etterStatus = GrunnlagsendringStatus.FORKASTET,
            type = GrunnlagsendringsType.SOEKER_DOED
        )
        val hendelserEtterOppdatertStatus = grunnlagsendringshendelsesRepo.hentAlleGrunnlagsendringshendelser()
        assertAll(
            "skal oppdatere statuser for grunnlagsendringshendelser",
            { assertEquals(hendelserFoerOppdatertStatus.size, hendelserEtterOppdatertStatus.size) },
            { assertTrue(hendelserFoerOppdatertStatus.all { it.status == GrunnlagsendringStatus.IKKE_VURDERT }) },
            { assertTrue(hendelserEtterOppdatertStatus.all { it.status == GrunnlagsendringStatus.FORKASTET }) },

            )
    }
}