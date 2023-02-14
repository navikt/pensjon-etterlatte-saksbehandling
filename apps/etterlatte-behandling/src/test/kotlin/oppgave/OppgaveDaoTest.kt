package oppgave

import no.nav.etterlatte.grunnlagsendring.GrunnlagsendringshendelseDao
import no.nav.etterlatte.grunnlagsendring.samsvarDoedsdatoer
import no.nav.etterlatte.libs.common.behandling.GrunnlagsendringStatus
import no.nav.etterlatte.libs.common.behandling.GrunnlagsendringsType
import no.nav.etterlatte.libs.common.behandling.Grunnlagsendringshendelse
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.behandling.Saksrolle
import no.nav.etterlatte.libs.database.DataSourceBuilder
import no.nav.etterlatte.libs.database.migrate
import no.nav.etterlatte.oppgave.OppgaveDao
import no.nav.etterlatte.sak.SakDao
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class OppgaveDaoTest {

    @Container
    private val postgreSQLContainer = PostgreSQLContainer<Nothing>("postgres:14")

    private lateinit var dataSource: DataSource
    private lateinit var oppgaveDao: OppgaveDao
    private lateinit var grunnlagsendringshendelsesRepo: GrunnlagsendringshendelseDao
    private lateinit var sakRepo: SakDao

    @BeforeAll
    fun beforeAll() {
        postgreSQLContainer.start()
        postgreSQLContainer.withUrlParam("user", postgreSQLContainer.username)
        postgreSQLContainer.withUrlParam("password", postgreSQLContainer.password)

        dataSource = DataSourceBuilder.createDataSource(
            jdbcUrl = postgreSQLContainer.jdbcUrl,
            username = postgreSQLContainer.username,
            password = postgreSQLContainer.password
        ).apply { migrate() }

        val connection = dataSource.connection
        oppgaveDao = OppgaveDao { connection }
        sakRepo = SakDao { connection }
        grunnlagsendringshendelsesRepo = GrunnlagsendringshendelseDao { connection }
    }

    @Test
    fun `uhaandterteGrunnlagsendringshendelser hentes som oppgaver hvis de har gyldig status`() {
        val sakid = sakRepo.opprettSak("02458201458", SakType.BARNEPENSJON).id
        val hendelse = Grunnlagsendringshendelse(
            id = UUID.randomUUID(),
            sakId = sakid,
            type = GrunnlagsendringsType.DOEDSFALL,
            opprettet = LocalDateTime.now().minusDays(2),
            status = GrunnlagsendringStatus.SJEKKET_AV_JOBB,
            behandlingId = null,
            hendelseGjelderRolle = Saksrolle.SOEKER,
            gjelderPerson = "02458201458",
            samsvarMellomPdlOgGrunnlag = samsvarDoedsdatoer(LocalDate.now().minusDays(3), null)
        )
        val hendelseIgnorert = Grunnlagsendringshendelse(
            id = UUID.randomUUID(),
            sakId = sakid,
            type = GrunnlagsendringsType.DOEDSFALL,
            opprettet = LocalDateTime.now().minusDays(3),
            status = GrunnlagsendringStatus.FORKASTET,
            behandlingId = null,
            hendelseGjelderRolle = Saksrolle.SOESKEN,
            gjelderPerson = "12312312312",
            samsvarMellomPdlOgGrunnlag = samsvarDoedsdatoer(LocalDate.now(), LocalDate.now())
        )

        grunnlagsendringshendelsesRepo.opprettGrunnlagsendringshendelse(hendelse)
        grunnlagsendringshendelsesRepo.opprettGrunnlagsendringshendelse(hendelseIgnorert)
        val oppgaver = oppgaveDao.finnOppgaverFraGrunnlagsendringshendelser()
        assertEquals(oppgaver.size, 1)
        assertEquals(oppgaver[0].sakId, sakid)
    }
}