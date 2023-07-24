package no.nav.etterlatte.grunnlagsendring

import no.nav.etterlatte.behandling.BehandlingDao
import no.nav.etterlatte.behandling.domain.GrunnlagsendringStatus
import no.nav.etterlatte.behandling.domain.GrunnlagsendringsType
import no.nav.etterlatte.behandling.domain.OpprettBehandling
import no.nav.etterlatte.behandling.kommerbarnettilgode.KommerBarnetTilGodeDao
import no.nav.etterlatte.behandling.revurdering.RevurderingDao
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.grunnlagsendringshendelseMedSamsvar
import no.nav.etterlatte.grunnlagsinformasjonDoedshendelse
import no.nav.etterlatte.grunnlagsinformasjonForelderBarnRelasjonHendelse
import no.nav.etterlatte.grunnlagsinformasjonUtflyttingshendelse
import no.nav.etterlatte.ikkeSamsvarMellomPdlOgGrunnlagDoed
import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.behandling.Prosesstype
import no.nav.etterlatte.libs.common.behandling.RevurderingAarsak
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.toLocalDatetimeUTC
import no.nav.etterlatte.libs.database.DataSourceBuilder
import no.nav.etterlatte.libs.database.POSTGRES_VERSION
import no.nav.etterlatte.libs.database.migrate
import no.nav.etterlatte.opprettBehandling
import no.nav.etterlatte.sak.SakDao
import no.nav.etterlatte.samsvarMellomPdlOgGrunnlagDoed
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertAll
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import java.time.LocalDate
import java.util.*
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class GrunnlagsendringshendelseDaoTest {

    @Container
    private val postgreSQLContainer = PostgreSQLContainer<Nothing>("postgres:$POSTGRES_VERSION")

    private lateinit var dataSource: DataSource
    private lateinit var sakRepo: SakDao
    private lateinit var grunnlagsendringshendelsesRepo: GrunnlagsendringshendelseDao
    private lateinit var behandlingRepo: BehandlingDao

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
        sakRepo = SakDao { connection }
        behandlingRepo =
            BehandlingDao(KommerBarnetTilGodeDao { connection }, RevurderingDao { connection }) { connection }
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
        val sakid = sakRepo.opprettSak("1234", SakType.BARNEPENSJON, Enheter.defaultEnhet.enhetNr).id
        val grunnlagsinformasjon = grunnlagsinformasjonDoedshendelse()
        val hendelse = grunnlagsendringshendelseMedSamsvar(
            id = uuid,
            sakId = sakid,
            type = GrunnlagsendringsType.DOEDSFALL,
            fnr = grunnlagsinformasjon.fnr,
            opprettet = Tidspunkt.now().toLocalDatetimeUTC(),
            samsvarMellomKildeOgGrunnlag = samsvarDoedsdatoer(
                grunnlagsinformasjon.doedsdato,
                grunnlagsinformasjon.doedsdato
            )
        )
        grunnlagsendringshendelsesRepo.opprettGrunnlagsendringshendelse(hendelse)
        val hendelseFraDatabase = grunnlagsendringshendelsesRepo.hentGrunnlagsendringshendelse(uuid)

        assertAll(
            "grunnlagsendringshendelse lagres korrekt i database",
            { assertEquals(hendelse.id, hendelseFraDatabase!!.id) },
            { assertEquals(hendelse.sakId, hendelseFraDatabase!!.sakId) },
            { assertEquals(hendelse.type, hendelseFraDatabase!!.type) },
            { assertEquals(hendelse.opprettet, hendelseFraDatabase!!.opprettet) },
            { assertEquals(hendelse.samsvarMellomKildeOgGrunnlag, hendelseFraDatabase!!.samsvarMellomKildeOgGrunnlag) }
        )
    }

    @Test
    fun `skal lagre og hente ut grunnlagsendringshendelse med rett type`() {
        val uuidDoed = UUID.randomUUID()
        val uuidUtflytting = UUID.randomUUID()
        val uuidForelderBarn = UUID.randomUUID()
        val sakid = sakRepo.opprettSak("1234", SakType.BARNEPENSJON, Enheter.defaultEnhet.enhetNr).id
        val grunnlagsinfoDoed = grunnlagsinformasjonDoedshendelse()
        val samsvarDoed = samsvarDoedsdatoer(grunnlagsinfoDoed.doedsdato, grunnlagsinfoDoed.doedsdato)
        val grunnlagsinfoUtflytting = grunnlagsinformasjonUtflyttingshendelse()
        val samsvarUtflytting = samsvarUtflytting(null, null)
        val grunnlagsinfoForelderBarn = grunnlagsinformasjonForelderBarnRelasjonHendelse()
        val samsvarForelderBarn = samsvarBarn(emptyList(), emptyList())
        val doedshendelse = grunnlagsendringshendelseMedSamsvar(
            id = uuidDoed,
            sakId = sakid,
            type = GrunnlagsendringsType.DOEDSFALL,
            fnr = grunnlagsinfoDoed.fnr,
            samsvarMellomKildeOgGrunnlag = samsvarDoed
        )
        val utflyttingsHendelse = grunnlagsendringshendelseMedSamsvar(
            id = uuidUtflytting,
            sakId = sakid,
            type = GrunnlagsendringsType.UTFLYTTING,
            fnr = grunnlagsinfoUtflytting.fnr,
            samsvarMellomKildeOgGrunnlag = samsvarUtflytting
        )
        val barnForeldreRelasjonHendelse = grunnlagsendringshendelseMedSamsvar(
            id = uuidForelderBarn,
            sakId = sakid,
            type = GrunnlagsendringsType.FORELDER_BARN_RELASJON,
            fnr = grunnlagsinfoForelderBarn.fnr,
            samsvarMellomKildeOgGrunnlag = samsvarForelderBarn
        )
        grunnlagsendringshendelsesRepo.opprettGrunnlagsendringshendelse(doedshendelse)
        grunnlagsendringshendelsesRepo.opprettGrunnlagsendringshendelse(utflyttingsHendelse)
        grunnlagsendringshendelsesRepo.opprettGrunnlagsendringshendelse(barnForeldreRelasjonHendelse)

        val doedshendelseFraDatabase = grunnlagsendringshendelsesRepo.hentGrunnlagsendringshendelse(uuidDoed)
        val utflyttingHendelseFraDatabase = grunnlagsendringshendelsesRepo.hentGrunnlagsendringshendelse(uuidUtflytting)
        val forelderBarnRelasjonHendelseFraDatabase =
            grunnlagsendringshendelsesRepo.hentGrunnlagsendringshendelse(uuidForelderBarn)

        assertEquals(doedshendelseFraDatabase?.type, GrunnlagsendringsType.DOEDSFALL)
        assertEquals(utflyttingHendelseFraDatabase?.type, GrunnlagsendringsType.UTFLYTTING)
        assertEquals(forelderBarnRelasjonHendelseFraDatabase?.type, GrunnlagsendringsType.FORELDER_BARN_RELASJON)
    }

    @Test
    fun `skal kunne sette status historisk på en hendelse som er tilknyttet en revurdering`() {
        val hendelseId = UUID.randomUUID()
        val sak1 = sakRepo.opprettSak("1234", SakType.BARNEPENSJON, Enheter.defaultEnhet.enhetNr).id
        val grunnlagsendringstype = GrunnlagsendringsType.DOEDSFALL
        val opprettBehandling = opprettBehandling(
            type = BehandlingType.REVURDERING,
            sakId = sak1,
            revurderingAarsak = RevurderingAarsak.REGULERING
        )
        behandlingRepo.opprettBehandling(opprettBehandling)

        val grunnlagsendringshendelseMedSamsvar = grunnlagsendringshendelseMedSamsvar(
            id = hendelseId,
            sakId = sak1,
            fnr = grunnlagsinformasjonDoedshendelse().fnr,
            type = grunnlagsendringstype,
            status = GrunnlagsendringStatus.SJEKKET_AV_JOBB,
            samsvarMellomKildeOgGrunnlag = null
        )
        grunnlagsendringshendelsesRepo.opprettGrunnlagsendringshendelse(grunnlagsendringshendelseMedSamsvar)
        grunnlagsendringshendelsesRepo.settBehandlingIdForTattMedIRevurdering(hendelseId, opprettBehandling.id)

        grunnlagsendringshendelsesRepo.oppdaterGrunnlagsendringHistorisk(opprettBehandling.id)
        val historiskRevurderingsHendelse = grunnlagsendringshendelsesRepo
            .hentGrunnlagsendringshendelse(hendelseId)
        assertEquals(GrunnlagsendringStatus.HISTORISK, historiskRevurderingsHendelse?.status)
        assertEquals(opprettBehandling.id, historiskRevurderingsHendelse?.behandlingId)
    }

    @Test
    fun `skal hente grunnlagsendringshendelser som er eldre enn en time`() {
        val sakid = sakRepo.opprettSak("1234", SakType.BARNEPENSJON, Enheter.defaultEnhet.enhetNr).id
        listOf(
            grunnlagsendringshendelseMedSamsvar(
                sakId = sakid,
                opprettet = Tidspunkt.now().toLocalDatetimeUTC(),
                fnr = grunnlagsinformasjonDoedshendelse().fnr,
                samsvarMellomKildeOgGrunnlag = null
            ),
            grunnlagsendringshendelseMedSamsvar(
                sakId = sakid,
                opprettet = Tidspunkt.now().toLocalDatetimeUTC().minusMinutes(30),
                fnr = grunnlagsinformasjonDoedshendelse().fnr,
                samsvarMellomKildeOgGrunnlag = null
            ),
            grunnlagsendringshendelseMedSamsvar(
                sakId = sakid,
                opprettet = Tidspunkt.now().toLocalDatetimeUTC().minusHours(1), // eldre enn en time
                fnr = grunnlagsinformasjonDoedshendelse().fnr,
                samsvarMellomKildeOgGrunnlag = null
            ),
            grunnlagsendringshendelseMedSamsvar(
                sakId = sakid,
                opprettet = Tidspunkt.now().toLocalDatetimeUTC().minusDays(4), // eldre enn en time
                fnr = grunnlagsinformasjonDoedshendelse().fnr,
                samsvarMellomKildeOgGrunnlag = null
            ),
            grunnlagsendringshendelseMedSamsvar(
                sakId = sakid,
                opprettet = Tidspunkt.now().toLocalDatetimeUTC().minusYears(1), // eldre enn en time
                fnr = grunnlagsinformasjonDoedshendelse().fnr,
                samsvarMellomKildeOgGrunnlag = null
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
            {
                assertTrue {
                    hendelserEldreEnn1Time.all {
                        it.opprettet <= Tidspunkt.now().toLocalDatetimeUTC().minusHours(1)
                    }
                }
            }
        )
    }

    @Test
    fun `hentGrunnlagsendringshendelserMedStatuserISak henter kun statuser som er angitt`() {
        val sakid = sakRepo.opprettSak("1234", SakType.BARNEPENSJON, Enheter.defaultEnhet.enhetNr).id
        listOf(
            with(grunnlagsinformasjonDoedshendelse()) {
                grunnlagsendringshendelseMedSamsvar(
                    sakId = sakid,
                    opprettet = Tidspunkt.now().toLocalDatetimeUTC(),
                    fnr = this.fnr,
                    samsvarMellomKildeOgGrunnlag = samsvarDoedsdatoer(this.doedsdato, this.doedsdato)
                )
            }.copy(status = GrunnlagsendringStatus.FORKASTET),
            with(grunnlagsinformasjonDoedshendelse()) {
                grunnlagsendringshendelseMedSamsvar(
                    sakId = sakid,
                    opprettet = Tidspunkt.now().toLocalDatetimeUTC().minusMinutes(30),
                    fnr = this.fnr,
                    samsvarMellomKildeOgGrunnlag = samsvarDoedsdatoer(this.doedsdato, this.doedsdato)
                )
            }.copy(
                status = GrunnlagsendringStatus.SJEKKET_AV_JOBB
            ),
            with(
                grunnlagsinformasjonDoedshendelse()
            ) {
                grunnlagsendringshendelseMedSamsvar(
                    sakId = sakid,
                    opprettet = Tidspunkt.now().toLocalDatetimeUTC().minusDays(4),
                    fnr = this.fnr,
                    samsvarMellomKildeOgGrunnlag = samsvarDoedsdatoer(this.doedsdato, this.doedsdato)
                )
            }.copy(
                status = GrunnlagsendringStatus.VENTER_PAA_JOBB
            ),
            with(
                grunnlagsinformasjonDoedshendelse()
            ) {
                grunnlagsendringshendelseMedSamsvar(
                    sakId = sakid,
                    opprettet = Tidspunkt.now().toLocalDatetimeUTC().minusYears(1),
                    fnr = this.fnr,
                    samsvarMellomKildeOgGrunnlag = samsvarDoedsdatoer(this.doedsdato, this.doedsdato)
                )
            }.copy(
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
                GrunnlagsendringStatus.VENTER_PAA_JOBB,
                GrunnlagsendringStatus.TATT_MED_I_BEHANDLING,
                GrunnlagsendringStatus.SJEKKET_AV_JOBB,
                GrunnlagsendringStatus.FORKASTET
            )
        )
        val uhaandterteHendelser = grunnlagsendringshendelsesRepo.hentGrunnlagsendringshendelserMedStatuserISak(
            sakid,
            listOf(GrunnlagsendringStatus.VENTER_PAA_JOBB, GrunnlagsendringStatus.SJEKKET_AV_JOBB)
        )
        assertEquals(uhaandterteHendelser.size, 2)
    }

    @Test
    fun `oppdaterGrunnlagsendringStatus skal oppdatere grunnlagsendringshendelser`() {
        val sak1 = sakRepo.opprettSak("1234", SakType.BARNEPENSJON, Enheter.defaultEnhet.enhetNr).id
        val id1 = UUID.randomUUID()
        val doedsdato = LocalDate.of(2022, 8, 1)

        grunnlagsendringshendelseMedSamsvar(
            id = id1,
            type = GrunnlagsendringsType.DOEDSFALL,
            sakId = sak1,
            fnr = grunnlagsinformasjonDoedshendelse(doedsdato = doedsdato).fnr,
            samsvarMellomKildeOgGrunnlag = null
        ).also {
            grunnlagsendringshendelsesRepo.opprettGrunnlagsendringshendelse(it)
        }

        val samsvarMellomPdlOgGrunnlag = samsvarMellomPdlOgGrunnlagDoed(doedsdato)

        val hendelserFoerOppdatertStatus = grunnlagsendringshendelsesRepo.hentAlleGrunnlagsendringshendelser()
        grunnlagsendringshendelsesRepo.oppdaterGrunnlagsendringStatusOgSamsvar(
            hendelseId = id1,
            foerStatus = GrunnlagsendringStatus.VENTER_PAA_JOBB,
            etterStatus = GrunnlagsendringStatus.FORKASTET,
            samsvarMellomKildeOgGrunnlag = samsvarMellomPdlOgGrunnlag
        )
        val hendelserEtterOppdatertStatus = grunnlagsendringshendelsesRepo.hentAlleGrunnlagsendringshendelser()
        assertAll(
            "skal oppdatere statuser for grunnlagsendringshendelser",
            { assertEquals(hendelserFoerOppdatertStatus.size, hendelserEtterOppdatertStatus.size) },
            { assertTrue(hendelserFoerOppdatertStatus.all { it.status == GrunnlagsendringStatus.VENTER_PAA_JOBB }) },
            { assertTrue(hendelserEtterOppdatertStatus.all { it.status == GrunnlagsendringStatus.FORKASTET }) }

        )
    }

    @Test
    fun `settBehandlingIdForTattMedIBehandling skal sette referanse til behandling`() {
        val hendelseId = UUID.randomUUID()
        val sak1 = sakRepo.opprettSak("1234", SakType.BARNEPENSJON, Enheter.defaultEnhet.enhetNr).id
        val grunnlagsendringstype = GrunnlagsendringsType.DOEDSFALL
        val opprettBehandling = opprettBehandling(
            type = BehandlingType.REVURDERING,
            sakId = sak1,
            revurderingAarsak = RevurderingAarsak.REGULERING
        )
        behandlingRepo.opprettBehandling(opprettBehandling)

        listOf(
            grunnlagsendringshendelseMedSamsvar(
                id = hendelseId,
                sakId = sak1,
                fnr = grunnlagsinformasjonDoedshendelse().fnr,
                type = grunnlagsendringstype,
                status = GrunnlagsendringStatus.SJEKKET_AV_JOBB,
                samsvarMellomKildeOgGrunnlag = null
            )
        ).forEach {
            grunnlagsendringshendelsesRepo.opprettGrunnlagsendringshendelse(it)
        }
        grunnlagsendringshendelsesRepo.settBehandlingIdForTattMedIRevurdering(hendelseId, opprettBehandling.id)
        val lagretHendelse = grunnlagsendringshendelsesRepo.hentGrunnlagsendringshendelse(hendelseId)
        assertEquals(opprettBehandling.id, lagretHendelse?.behandlingId)
    }

    @Test
    fun `kobleGrunnlagsendringshendelserFraBehandlingId skal oppdatere riktige grunnlagsendringshendelser`() {
        val sak1 = sakRepo.opprettSak("1", SakType.BARNEPENSJON, Enheter.defaultEnhet.enhetNr).id
        val sak2 = sakRepo.opprettSak("2", SakType.BARNEPENSJON, Enheter.defaultEnhet.enhetNr).id

        val behandling1 = OpprettBehandling(
            type = BehandlingType.REVURDERING,
            sakId = sak1,
            status = BehandlingStatus.OPPRETTET,
            persongalleri = Persongalleri(
                soeker = "12312312312",
                innsender = null,
                soesken = listOf(),
                avdoed = listOf(),
                gjenlevende = listOf()
            ),
            soeknadMottattDato = null,
            virkningstidspunkt = null,
            revurderingsAarsak = RevurderingAarsak.SOESKENJUSTERING,
            opphoerAarsaker = listOf(),
            fritekstAarsak = null,
            prosesstype = Prosesstype.MANUELL,
            kilde = Vedtaksloesning.GJENNY,
            merknad = null
        )
        val behandling2 = behandling1.copy(sakId = sak2)
        assertNotEquals(behandling1.id, behandling2.id)

        behandlingRepo.opprettBehandling(
            behandling = behandling1
        )
        behandlingRepo.opprettBehandling(behandling = behandling2)

        val id1 = UUID.randomUUID()
        val id2 = UUID.randomUUID()
        val id3 = UUID.randomUUID()
        val doedsdato = LocalDate.of(2022, 7, 21)
        val samsvarMellomPdlOgGrunnlag = ikkeSamsvarMellomPdlOgGrunnlagDoed(doedsdato)
        val fnrDoedshendelse = grunnlagsinformasjonDoedshendelse().fnr

        listOf(
            grunnlagsendringshendelseMedSamsvar(
                id = id1,
                sakId = sak1,
                fnr = fnrDoedshendelse,
                samsvarMellomKildeOgGrunnlag = samsvarMellomPdlOgGrunnlag,
                status = GrunnlagsendringStatus.SJEKKET_AV_JOBB
            ),
            grunnlagsendringshendelseMedSamsvar(
                id = id2,
                sakId = sak1,
                fnr = fnrDoedshendelse,
                samsvarMellomKildeOgGrunnlag = samsvarMellomPdlOgGrunnlag,
                status = GrunnlagsendringStatus.SJEKKET_AV_JOBB
            ),
            grunnlagsendringshendelseMedSamsvar(
                id = id3,
                sakId = sak2,
                fnr = fnrDoedshendelse,
                samsvarMellomKildeOgGrunnlag = samsvarMellomPdlOgGrunnlag,
                status = GrunnlagsendringStatus.SJEKKET_AV_JOBB
            )
        ).forEach {
            grunnlagsendringshendelsesRepo.opprettGrunnlagsendringshendelse(it)
        }

        grunnlagsendringshendelsesRepo.settBehandlingIdForTattMedIRevurdering(id1, behandling1.id)
        grunnlagsendringshendelsesRepo.settBehandlingIdForTattMedIRevurdering(id3, behandling2.id)

        grunnlagsendringshendelsesRepo.kobleGrunnlagsendringshendelserFraBehandlingId(behandling1.id)

        val hendelse1 = grunnlagsendringshendelsesRepo.hentGrunnlagsendringshendelse(id1)
        val hendelse2 = grunnlagsendringshendelsesRepo.hentGrunnlagsendringshendelse(id2)
        val hendelse3 = grunnlagsendringshendelsesRepo.hentGrunnlagsendringshendelse(id3)

        assertEquals(GrunnlagsendringStatus.SJEKKET_AV_JOBB, hendelse1?.status)
        assertNull(hendelse1?.behandlingId)
        assertEquals(GrunnlagsendringStatus.SJEKKET_AV_JOBB, hendelse2?.status)
        assertEquals(GrunnlagsendringStatus.TATT_MED_I_BEHANDLING, hendelse3?.status)
        assertNotNull(hendelse3?.behandlingId)
    }

    @Suppress("ktlint:standard:max-line-length")
    @Test
    fun `hentGyldigeGrunnlagsendringshendelserISak skal hente alle grunnlagsendringshendelser med status SJEKKET_AV_JOBB`() {
        val sak1 = sakRepo.opprettSak("1234", SakType.BARNEPENSJON, Enheter.defaultEnhet.enhetNr).id
        val sak2 = sakRepo.opprettSak("4321", SakType.BARNEPENSJON, Enheter.defaultEnhet.enhetNr).id
        val id1 = UUID.randomUUID()
        val id2 = UUID.randomUUID()
        val id3 = UUID.randomUUID()
        val doedsdato = LocalDate.of(2022, 7, 21)
        val samsvarMellomPdlOgGrunnlag = ikkeSamsvarMellomPdlOgGrunnlagDoed(doedsdato)
        val fnrDoedshendelse = grunnlagsinformasjonDoedshendelse().fnr

        listOf(
            grunnlagsendringshendelseMedSamsvar(
                id = id1,
                sakId = sak1,
                fnr = fnrDoedshendelse,
                samsvarMellomKildeOgGrunnlag = samsvarMellomPdlOgGrunnlag
            ),
            grunnlagsendringshendelseMedSamsvar(
                id = id2,
                sakId = sak2,
                fnr = fnrDoedshendelse,
                samsvarMellomKildeOgGrunnlag = samsvarMellomPdlOgGrunnlag
            ),
            grunnlagsendringshendelseMedSamsvar(
                id = id3,
                sakId = sak2,
                fnr = fnrDoedshendelse,
                samsvarMellomKildeOgGrunnlag = samsvarMellomPdlOgGrunnlag
            )
        ).forEach {
            grunnlagsendringshendelsesRepo.opprettGrunnlagsendringshendelse(it)
        }
        grunnlagsendringshendelsesRepo.oppdaterGrunnlagsendringStatusOgSamsvar(
            hendelseId = id1,
            foerStatus = GrunnlagsendringStatus.VENTER_PAA_JOBB,
            etterStatus = GrunnlagsendringStatus.SJEKKET_AV_JOBB,
            samsvarMellomKildeOgGrunnlag = samsvarMellomPdlOgGrunnlag
        )
        grunnlagsendringshendelsesRepo.oppdaterGrunnlagsendringStatusOgSamsvar(
            hendelseId = id2,
            foerStatus = GrunnlagsendringStatus.VENTER_PAA_JOBB,
            etterStatus = GrunnlagsendringStatus.SJEKKET_AV_JOBB,
            samsvarMellomKildeOgGrunnlag = samsvarMellomPdlOgGrunnlag
        )
        grunnlagsendringshendelsesRepo.oppdaterGrunnlagsendringStatusOgSamsvar(
            hendelseId = id3,
            foerStatus = GrunnlagsendringStatus.VENTER_PAA_JOBB,
            etterStatus = GrunnlagsendringStatus.SJEKKET_AV_JOBB,
            samsvarMellomKildeOgGrunnlag = samsvarMellomPdlOgGrunnlag
        )

        val resultat = grunnlagsendringshendelsesRepo.hentGrunnlagsendringshendelserSomErSjekketAvJobb(sak2)

        assertEquals(2, resultat.size)
        assertTrue(resultat.all { it.status == GrunnlagsendringStatus.SJEKKET_AV_JOBB })
    }
}