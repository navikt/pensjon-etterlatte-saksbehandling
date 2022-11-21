package no.nav.etterlatte.grunnlagsendring

import no.nav.etterlatte.behandling.BehandlingDao
import no.nav.etterlatte.database.DataSourceBuilder
import no.nav.etterlatte.grunnlagsendringshendelse
import no.nav.etterlatte.grunnlagsinformasjonDoedshendelse
import no.nav.etterlatte.grunnlagsinformasjonForelderBarnRelasjonHendelse
import no.nav.etterlatte.grunnlagsinformasjonUtflyttingshendelse
import no.nav.etterlatte.libs.common.behandling.GrunnlagsendringStatus
import no.nav.etterlatte.libs.common.behandling.GrunnlagsendringsType
import no.nav.etterlatte.libs.common.behandling.Grunnlagsinformasjon
import no.nav.etterlatte.libs.common.behandling.RevurderingAarsak
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.revurdering
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
    private lateinit var sakRepo: SakDao
    private lateinit var grunnlagsendringshendelsesRepo: GrunnlagsendringshendelseDao
    private lateinit var behandlingRepo: BehandlingDao

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
        behandlingRepo = BehandlingDao { connection }
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
        val sakid = sakRepo.opprettSak("1234", SakType.BARNEPENSJON).id
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
            { assertEquals(hendelse.data, hendelseFraDatabase!!.data) }
        )
    }

    @Test
    fun `skal lagre og hente ut grunnlagsendringshendelse med rett type`() {
        val uuidDoed = UUID.randomUUID()
        val uuidUtflytting = UUID.randomUUID()
        val uuidForelderBarn = UUID.randomUUID()
        val sakid = sakRepo.opprettSak("1234", SakType.BARNEPENSJON).id
        val grunnlagsinfoDoed = grunnlagsinformasjonDoedshendelse()
        val grunnlagsinfoUtflytting = grunnlagsinformasjonUtflyttingshendelse()
        val grunnlagsinfoForelderBarn = grunnlagsinformasjonForelderBarnRelasjonHendelse()
        val doedshendelse = grunnlagsendringshendelse(
            id = uuidDoed,
            sakId = sakid,
            type = GrunnlagsendringsType.DOEDSFALL,
            data = grunnlagsinfoDoed
        )
        val utflyttingsHendelse = grunnlagsendringshendelse(
            id = uuidUtflytting,
            sakId = sakid,
            type = GrunnlagsendringsType.UTFLYTTING,
            data = grunnlagsinfoUtflytting
        )
        val barnForeldreRelasjonHendelse = grunnlagsendringshendelse(
            id = uuidForelderBarn,
            sakId = sakid,
            type = GrunnlagsendringsType.FORELDER_BARN_RELASJON,
            data = grunnlagsinfoForelderBarn
        )
        grunnlagsendringshendelsesRepo.opprettGrunnlagsendringshendelse(doedshendelse)
        grunnlagsendringshendelsesRepo.opprettGrunnlagsendringshendelse(utflyttingsHendelse)
        grunnlagsendringshendelsesRepo.opprettGrunnlagsendringshendelse(barnForeldreRelasjonHendelse)

        val doedshendelseFraDatabase = grunnlagsendringshendelsesRepo.hentGrunnlagsendringshendelse(uuidDoed)
        val utflyttingHendelseFraDatabase = grunnlagsendringshendelsesRepo.hentGrunnlagsendringshendelse(uuidUtflytting)
        val forelderBarnRelasjonHendelseFraDatabase =
            grunnlagsendringshendelsesRepo.hentGrunnlagsendringshendelse(uuidForelderBarn)

        assertTrue(doedshendelseFraDatabase?.data is Grunnlagsinformasjon.Doedsfall)
        assertTrue(utflyttingHendelseFraDatabase?.data is Grunnlagsinformasjon.Utflytting)
        assertTrue(forelderBarnRelasjonHendelseFraDatabase?.data is Grunnlagsinformasjon.ForelderBarnRelasjon)
    }

    @Test
    fun `skal hente grunnlagsendringshendelser som er eldre enn en time`() {
        val sakid = sakRepo.opprettSak("1234", SakType.BARNEPENSJON).id
        listOf(
            grunnlagsendringshendelse(
                sakId = sakid,
                opprettet = LocalDateTime.now(),
                data = grunnlagsinformasjonDoedshendelse()
            ),
            grunnlagsendringshendelse(
                sakId = sakid,
                opprettet = LocalDateTime.now().minusMinutes(30),
                data = grunnlagsinformasjonDoedshendelse()
            ),
            grunnlagsendringshendelse(
                sakId = sakid,
                opprettet = LocalDateTime.now().minusHours(1), // eldre enn en time
                data = grunnlagsinformasjonDoedshendelse()
            ),
            grunnlagsendringshendelse(
                sakId = sakid,
                opprettet = LocalDateTime.now().minusDays(4), // eldre enn en time
                data = grunnlagsinformasjonDoedshendelse()
            ),
            grunnlagsendringshendelse(
                sakId = sakid,
                opprettet = LocalDateTime.now().minusYears(1), // eldre enn en time
                data = grunnlagsinformasjonDoedshendelse()
            )
        ).forEach {
            grunnlagsendringshendelsesRepo.opprettGrunnlagsendringshendelse(it)
        }
        val hendelserEldreEnn1Time = grunnlagsendringshendelsesRepo.hentIkkeVurderteGrunnlagsendringshendelserEldreEnn(
            60
        )
        assertAll(
            "henter kun grunnlagsendringshendelser som er eldre enn 1 time",
            { assertEquals(3, hendelserEldreEnn1Time.size) },
            { assertTrue { hendelserEldreEnn1Time.all { it.opprettet <= LocalDateTime.now().minusHours(1) } } }
        )
    }

    @Test
    fun `hentGrunnlagsendringshendelserMedStatuserISak henter kun statuser som er angitt`() {
        val sakid = sakRepo.opprettSak("1234", SakType.BARNEPENSJON).id
        listOf(
            grunnlagsendringshendelse(
                sakId = sakid,
                opprettet = LocalDateTime.now(),
                data = grunnlagsinformasjonDoedshendelse()
            ).copy(status = GrunnlagsendringStatus.FORKASTET),
            grunnlagsendringshendelse(
                sakId = sakid,
                opprettet = LocalDateTime.now().minusMinutes(30),
                data = grunnlagsinformasjonDoedshendelse()
            ).copy(
                status = GrunnlagsendringStatus.GYLDIG_OG_KAN_TAS_MED_I_BEHANDLING
            ),
            grunnlagsendringshendelse(
                sakId = sakid,
                opprettet = LocalDateTime.now().minusDays(4),
                data = grunnlagsinformasjonDoedshendelse()
            ).copy(
                status = GrunnlagsendringStatus.IKKE_VURDERT
            ),
            grunnlagsendringshendelse(
                sakId = sakid,
                opprettet = LocalDateTime.now().minusYears(1),
                data = grunnlagsinformasjonDoedshendelse()
            ).copy(
                status = GrunnlagsendringStatus.TATT_MED_I_BEHANDLING
            )
        ).forEach {
            grunnlagsendringshendelsesRepo.opprettGrunnlagsendringshendelse(it)
        }
        val alleHendelser = grunnlagsendringshendelsesRepo.hentGrunnlagsendringshendelserMedStatuserISak(
            sakid,
            GrunnlagsendringStatus.values().toList()
        )
        assertEquals(alleHendelser.size, 4)
        assertEquals(
            alleHendelser.map { it.status }.toSet(),
            setOf(
                GrunnlagsendringStatus.IKKE_VURDERT,
                GrunnlagsendringStatus.TATT_MED_I_BEHANDLING,
                GrunnlagsendringStatus.GYLDIG_OG_KAN_TAS_MED_I_BEHANDLING,
                GrunnlagsendringStatus.FORKASTET
            )
        )
        val uhaandterteHendelser = grunnlagsendringshendelsesRepo.hentGrunnlagsendringshendelserMedStatuserISak(
            sakid,
            listOf(GrunnlagsendringStatus.IKKE_VURDERT, GrunnlagsendringStatus.GYLDIG_OG_KAN_TAS_MED_I_BEHANDLING)
        )
        assertEquals(uhaandterteHendelser.size, 2)
    }

    @Test
    fun `oppdaterGrunnlagsendringStatuForType skal oppdatere status for grunnlagsendringshendelser`() {
        val sak1 = sakRepo.opprettSak("1234", SakType.BARNEPENSJON).id
        val sak2 = sakRepo.opprettSak("4321", SakType.BARNEPENSJON).id

        listOf(
            grunnlagsendringshendelse(
                sakId = sak1,
                data = grunnlagsinformasjonDoedshendelse()
            ),
            grunnlagsendringshendelse(
                sakId = sak1,
                data = grunnlagsinformasjonDoedshendelse()
            ),
            grunnlagsendringshendelse(
                sakId = sak2,
                data = grunnlagsinformasjonDoedshendelse()
            )
        ).forEach {
            grunnlagsendringshendelsesRepo.opprettGrunnlagsendringshendelse(it)
        }

        val hendelserFoerOppdatertStatus = grunnlagsendringshendelsesRepo.hentAlleGrunnlagsendringshendelser()
        grunnlagsendringshendelsesRepo.oppdaterGrunnlagsendringStatusForType(
            saker = listOf(sak1, sak2),
            foerStatus = GrunnlagsendringStatus.IKKE_VURDERT,
            etterStatus = GrunnlagsendringStatus.FORKASTET,
            type = GrunnlagsendringsType.DOEDSFALL
        )
        val hendelserEtterOppdatertStatus = grunnlagsendringshendelsesRepo.hentAlleGrunnlagsendringshendelser()
        assertAll(
            "skal oppdatere statuser for grunnlagsendringshendelser",
            { assertEquals(hendelserFoerOppdatertStatus.size, hendelserEtterOppdatertStatus.size) },
            { assertTrue(hendelserFoerOppdatertStatus.all { it.status == GrunnlagsendringStatus.IKKE_VURDERT }) },
            { assertTrue(hendelserEtterOppdatertStatus.all { it.status == GrunnlagsendringStatus.FORKASTET }) }

        )
    }

    @Test
    fun `settBehandlingIdForTattMedIBehandling skal sette referanse til behandling`() {
        val hendelseId = UUID.randomUUID()
        val sak1 = sakRepo.opprettSak("1234", SakType.BARNEPENSJON).id
        val grunnlagsendringstype = GrunnlagsendringsType.DOEDSFALL
        val revurderingId = UUID.randomUUID()
        val revurdering = revurdering(id = revurderingId, sak = sak1, revurderingAarsak = RevurderingAarsak.SOEKER_DOD)
        behandlingRepo.opprettRevurdering(revurdering)
        listOf(
            grunnlagsendringshendelse(
                id = hendelseId,
                sakId = sak1,
                data = grunnlagsinformasjonDoedshendelse(),
                type = grunnlagsendringstype,
                status = GrunnlagsendringStatus.TATT_MED_I_BEHANDLING
            )
        ).forEach {
            grunnlagsendringshendelsesRepo.opprettGrunnlagsendringshendelse(it)
        }
        grunnlagsendringshendelsesRepo.settBehandlingIdForTattMedIBehandling(sak1, revurderingId, grunnlagsendringstype)
        val lagretHendelse = grunnlagsendringshendelsesRepo.hentGrunnlagsendringshendelse(hendelseId)
        assertEquals(revurderingId, lagretHendelse?.behandlingId)
    }

    @Test
    fun `hentGyldigeGrunnlagsendringshendelserISak skal hente alle grunnlagsendringshendelser med status GYLDIG_OG_KAN_TAS_MED_I_BEHANDLING`() { // ktlint-disable max-line-length
        val sak1 = sakRepo.opprettSak("1234", SakType.BARNEPENSJON).id
        val sak2 = sakRepo.opprettSak("4321", SakType.BARNEPENSJON).id

        listOf(
            grunnlagsendringshendelse(
                sakId = sak1,
                data = grunnlagsinformasjonDoedshendelse()
            ),
            grunnlagsendringshendelse(
                sakId = sak1,
                data = grunnlagsinformasjonDoedshendelse()
            ),
            grunnlagsendringshendelse(
                sakId = sak2,
                data = grunnlagsinformasjonDoedshendelse()
            )
        ).forEach {
            grunnlagsendringshendelsesRepo.opprettGrunnlagsendringshendelse(it)
        }
        grunnlagsendringshendelsesRepo.oppdaterGrunnlagsendringStatusForType(
            saker = listOf(sak1, sak2),
            foerStatus = GrunnlagsendringStatus.IKKE_VURDERT,
            etterStatus = GrunnlagsendringStatus.GYLDIG_OG_KAN_TAS_MED_I_BEHANDLING,
            type = GrunnlagsendringsType.DOEDSFALL
        )

        val resultat = grunnlagsendringshendelsesRepo.hentGyldigeGrunnlagsendringshendelserISak(sak1)

        assertEquals(2, resultat.size)
        assertTrue(resultat.all { it.status == GrunnlagsendringStatus.GYLDIG_OG_KAN_TAS_MED_I_BEHANDLING })
    }
}