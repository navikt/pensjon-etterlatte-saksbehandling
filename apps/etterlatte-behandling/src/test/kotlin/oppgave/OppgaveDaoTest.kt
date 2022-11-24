package oppgave

import no.nav.etterlatte.database.DataSourceBuilder
import no.nav.etterlatte.grunnlagsendring.GrunnlagsendringshendelseDao
import no.nav.etterlatte.grunnlagsendringshendelse
import no.nav.etterlatte.grunnlagsinformasjonDoedshendelse
import no.nav.etterlatte.grunnlagsinformasjonUtflyttingshendelse
import no.nav.etterlatte.libs.common.behandling.GrunnlagsendringStatus
import no.nav.etterlatte.libs.common.behandling.GrunnlagsendringsType
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.oppgave.OppgaveDao
import no.nav.etterlatte.sak.SakDao
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import java.util.UUID
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class OppgaveDaoTest {

    @Container
    private val postgreSQLContainer = PostgreSQLContainer<Nothing>("postgres:12")

    private lateinit var dataSource: DataSource
    private lateinit var oppgaveDao: OppgaveDao
    private lateinit var grunnlagsendringshendelsesRepo: GrunnlagsendringshendelseDao
    private lateinit var sakRepo: SakDao

    @BeforeAll
    fun beforeAll() {
        postgreSQLContainer.start()
        postgreSQLContainer.withUrlParam("user", postgreSQLContainer.username)
        postgreSQLContainer.withUrlParam("password", postgreSQLContainer.password)

        val dsb = DataSourceBuilder(mapOf("DB_JDBC_URL" to postgreSQLContainer.jdbcUrl))
        dataSource = dsb.dataSource

        dsb.migrate()

        val connection = dataSource.connection
        oppgaveDao = OppgaveDao { connection }
        sakRepo = SakDao { connection }
        grunnlagsendringshendelsesRepo = GrunnlagsendringshendelseDao { connection }
    }

    @Test
    fun `uhaandterteGrunnlagsendringshendelser hentes som oppgaver hvis de har gyldig status`() {
        val uuid = UUID.randomUUID()
        val sakid = sakRepo.opprettSak("02458201458", SakType.BARNEPENSJON).id
        val grunnlagsinformasjon = grunnlagsinformasjonDoedshendelse()
        val hendelse = grunnlagsendringshendelse(
            id = uuid,
            sakId = sakid,
            data = grunnlagsinformasjon,
            status = GrunnlagsendringStatus.GYLDIG_OG_KAN_TAS_MED_I_BEHANDLING
        )
        val utflyttingsHendelse = grunnlagsinformasjonUtflyttingshendelse()
        val hendelseIgnorert = grunnlagsendringshendelse(
            id = UUID.randomUUID(),
            sakId = sakid,
            type = GrunnlagsendringsType.UTFLYTTING,
            data = utflyttingsHendelse,
            status = GrunnlagsendringStatus.IKKE_VURDERT
        )

        grunnlagsendringshendelsesRepo.opprettGrunnlagsendringshendelse(hendelse)
        grunnlagsendringshendelsesRepo.opprettGrunnlagsendringshendelse(hendelseIgnorert)
        val oppgaver = oppgaveDao.finnOppgaverFraGrunnlagsendringshendelser()
        assertEquals(oppgaver.size, 1)
        assertEquals(oppgaver[0].sakId, sakid)
    }
}