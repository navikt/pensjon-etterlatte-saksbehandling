package oppgave

import no.nav.etterlatte.behandling.BehandlingDao
import no.nav.etterlatte.behandling.domain.Regulering
import no.nav.etterlatte.grunnlagsendring.GrunnlagsendringshendelseDao
import no.nav.etterlatte.grunnlagsendring.samsvarDoedsdatoer
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.GrunnlagsendringStatus
import no.nav.etterlatte.libs.common.behandling.GrunnlagsendringsType
import no.nav.etterlatte.libs.common.behandling.Grunnlagsendringshendelse
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.behandling.Prosesstype
import no.nav.etterlatte.libs.common.behandling.RevurderingAarsak
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.behandling.Saksrolle
import no.nav.etterlatte.libs.database.DataSourceBuilder
import no.nav.etterlatte.libs.database.migrate
import no.nav.etterlatte.oppgave.OppgaveDao
import no.nav.etterlatte.oppgave.domain.Oppgave
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
    val fnr = "02458201458"

    @Container
    private val postgreSQLContainer = PostgreSQLContainer<Nothing>("postgres:14")

    private lateinit var dataSource: DataSource
    private lateinit var oppgaveDao: OppgaveDao
    private lateinit var grunnlagsendringshendelsesRepo: GrunnlagsendringshendelseDao
    private lateinit var behandlingDao: BehandlingDao
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
        behandlingDao = BehandlingDao { connection }
    }

    @Test
    fun `uhaandterteGrunnlagsendringshendelser hentes som oppgaver hvis de har gyldig status`() {
        val sakid = sakRepo.opprettSak(fnr, SakType.BARNEPENSJON).id
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

    @Test
    fun `manuelle reguleringer vises i oppgavelisten men ikke automatiske`() {
        sakRepo.opprettSak(fnr, SakType.BARNEPENSJON).id
        val automatisk = lagRegulering(Prosesstype.AUTOMATISK)
        val manuel = lagRegulering(Prosesstype.MANUELL)

        behandlingDao.opprettRegulering(automatisk)
        behandlingDao.opprettRegulering(manuel)

        val oppgaver = oppgaveDao.finnOppgaverMedStatuser(listOf(BehandlingStatus.OPPRETTET))
        assertEquals(oppgaver.size, 1)
        assertEquals(manuel.id, (oppgaver[0] as Oppgave.BehandlingOppgave).behandlingId)
    }

    private fun lagRegulering(prosesstype: Prosesstype): Regulering {
        return Regulering(
            id = UUID.randomUUID(),
            sak = 1,
            behandlingOpprettet = LocalDateTime.now(),
            sistEndret = LocalDateTime.now(),
            status = BehandlingStatus.OPPRETTET,
            persongalleri = Persongalleri(
                soeker = fnr,
                innsender = null,
                soesken = listOf(),
                avdoed = listOf(),
                gjenlevende = listOf()
            ),
            kommerBarnetTilgode = null,
            vilkaarUtfall = null,
            virkningstidspunkt = null,
            revurderingsaarsak = RevurderingAarsak.GRUNNBELOEPREGULERING,
            prosesstype = prosesstype

        )
    }
}