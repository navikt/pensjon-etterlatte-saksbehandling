package oppgave

import no.nav.etterlatte.STOR_SNERK
import no.nav.etterlatte.TRIVIELL_MIDTPUNKT
import no.nav.etterlatte.behandling.BehandlingDao
import no.nav.etterlatte.behandling.domain.GrunnlagsendringStatus
import no.nav.etterlatte.behandling.domain.GrunnlagsendringsType
import no.nav.etterlatte.behandling.domain.Grunnlagsendringshendelse
import no.nav.etterlatte.behandling.domain.OpprettBehandling
import no.nav.etterlatte.grunnlagsendring.GrunnlagsendringshendelseDao
import no.nav.etterlatte.grunnlagsendring.samsvarDoedsdatoer
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.behandling.Prosesstype
import no.nav.etterlatte.libs.common.behandling.RevurderingAarsak
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.behandling.Saksrolle
import no.nav.etterlatte.libs.common.pdlhendelse.AdressebeskyttelseGradering
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.toLocalDatetimeUTC
import no.nav.etterlatte.libs.database.DataSourceBuilder
import no.nav.etterlatte.libs.database.migrate
import no.nav.etterlatte.oppgave.OppgaveDao
import no.nav.etterlatte.oppgave.domain.Oppgave
import no.nav.etterlatte.sak.SakDao
import no.nav.etterlatte.sak.SakDaoAdressebeskyttelse
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import java.time.LocalDate
import java.util.*
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class OppgaveDaoTest {

    @Container
    private val postgreSQLContainer = PostgreSQLContainer<Nothing>("postgres:14")

    private lateinit var dataSource: DataSource
    private lateinit var oppgaveDao: OppgaveDao
    private lateinit var grunnlagsendringshendelsesRepo: GrunnlagsendringshendelseDao
    private lateinit var behandlingDao: BehandlingDao
    private lateinit var sakDao: SakDao
    private lateinit var sakDaoAdressebeskyttelse: SakDaoAdressebeskyttelse

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
        sakDao = SakDao { connection }
        grunnlagsendringshendelsesRepo = GrunnlagsendringshendelseDao { connection }
        behandlingDao = BehandlingDao { connection }
        sakDaoAdressebeskyttelse = SakDaoAdressebeskyttelse(dataSource)
    }

    @AfterEach
    fun afterEach() {
        dataSource.connection.use {
            it.prepareStatement("TRUNCATE sak CASCADE;").execute()
        }
    }

    @Test
    fun `uhaandterteGrunnlagsendringshendelser hentes som oppgaver hvis de har gyldig status`() {
        val fnr = "02458201458"

        val sakid = sakDao.opprettSak(fnr, SakType.BARNEPENSJON).id
        val hendelse = Grunnlagsendringshendelse(
            id = UUID.randomUUID(),
            sakId = sakid,
            type = GrunnlagsendringsType.DOEDSFALL,
            opprettet = Tidspunkt.now().toLocalDatetimeUTC().minusDays(2),
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
            opprettet = Tidspunkt.now().toLocalDatetimeUTC().minusDays(3),
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
        val fnr = TRIVIELL_MIDTPUNKT.value
        val sak = sakDao.opprettSak(fnr, SakType.BARNEPENSJON)
        val automatisk = lagRegulering(Prosesstype.AUTOMATISK, fnr, sak.id)
        val manuel = lagRegulering(Prosesstype.MANUELL, fnr, sak.id)

        behandlingDao.opprettBehandling(automatisk)
        behandlingDao.opprettBehandling(manuel)

        val oppgaver = oppgaveDao.finnOppgaverMedStatuser(listOf(BehandlingStatus.OPPRETTET))
        assertEquals(1, oppgaver.size)
        assertEquals(manuel.id, (oppgaver[0] as Oppgave.BehandlingOppgave).behandlingId)
    }

    @Test
    fun `skal ikke returnere strengt fortrolig oppgave for annen enn rolle som har strengt fortrolig`() {
        val fnr = STOR_SNERK.value

        val sak = sakDao.opprettSak(fnr, SakType.BARNEPENSJON)
        behandlingDao.opprettBehandling(lagRegulering(Prosesstype.MANUELL, fnr, sak.id))
        sakDaoAdressebeskyttelse.oppdaterAdresseBeskyttelse(sak.id, AdressebeskyttelseGradering.STRENGT_FORTROLIG)

        val sakk = sakDao.opprettSak(TRIVIELL_MIDTPUNKT.value, SakType.BARNEPENSJON)
        behandlingDao.opprettBehandling(lagRegulering(Prosesstype.MANUELL, TRIVIELL_MIDTPUNKT.value, sakk.id))

        val alleBehandlingsStatuser = BehandlingStatus.values().asList()
        val oppgaver = oppgaveDao.finnOppgaverMedStatuser(alleBehandlingsStatuser)
        assertEquals(1, oppgaver.size)

        val strengtFortroligOppgaver = oppgaveDao.finnOppgaverForStrengtFortroligOgStrengtFortroligUtland(
            alleBehandlingsStatuser
        )
        assertEquals(1, strengtFortroligOppgaver.size)

        sakDaoAdressebeskyttelse.oppdaterAdresseBeskyttelse(sak.id, AdressebeskyttelseGradering.UGRADERT)
        assertEquals(2, oppgaveDao.finnOppgaverMedStatuser(alleBehandlingsStatuser).size)
        assertEquals(
            0,
            oppgaveDao.finnOppgaverForStrengtFortroligOgStrengtFortroligUtland(alleBehandlingsStatuser).size
        )

        sakDaoAdressebeskyttelse.oppdaterAdresseBeskyttelse(
            sak.id,
            AdressebeskyttelseGradering.STRENGT_FORTROLIG_UTLAND
        )
        assertEquals(1, oppgaveDao.finnOppgaverMedStatuser(alleBehandlingsStatuser).size)
        assertEquals(
            1,
            oppgaveDao.finnOppgaverForStrengtFortroligOgStrengtFortroligUtland(alleBehandlingsStatuser).size
        )
    }

    private fun lagRegulering(prosesstype: Prosesstype, fnr: String, sakId: Long): OpprettBehandling {
        return OpprettBehandling(
            type = BehandlingType.REVURDERING,
            revurderingsAarsak = RevurderingAarsak.REGULERING,
            sakId = sakId,
            status = BehandlingStatus.OPPRETTET,
            persongalleri = Persongalleri(
                soeker = fnr,
                innsender = null,
                soesken = listOf(),
                avdoed = listOf(),
                gjenlevende = listOf()
            ),
            soeknadMottattDato = null,
            kommerBarnetTilgode = null,
            vilkaarUtfall = null,
            virkningstidspunkt = null,
            opphoerAarsaker = listOf(),
            fritekstAarsak = null,
            prosesstype = prosesstype
        )
    }
}