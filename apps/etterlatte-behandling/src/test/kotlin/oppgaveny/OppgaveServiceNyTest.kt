package no.nav.etterlatte.oppgaveny

import com.nimbusds.jwt.JWTClaimsSet
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.NotFoundException
import io.mockk.every
import io.mockk.mockk
import no.nav.etterlatte.Context
import no.nav.etterlatte.DatabaseKontekst
import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.SaksbehandlerMedEnheterOgRoller
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.funksjonsbrytere.DummyFeatureToggleService
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.oppgaveNy.OppgaveKilde
import no.nav.etterlatte.libs.common.oppgaveNy.OppgaveType
import no.nav.etterlatte.libs.common.oppgaveNy.Status
import no.nav.etterlatte.libs.common.oppgaveNy.VedtakOppgaveDTO
import no.nav.etterlatte.libs.common.person.AdressebeskyttelseGradering
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.toLocalDatetimeUTC
import no.nav.etterlatte.libs.common.tidspunkt.toTidspunkt
import no.nav.etterlatte.libs.database.DataSourceBuilder
import no.nav.etterlatte.libs.database.POSTGRES_VERSION
import no.nav.etterlatte.libs.database.migrate
import no.nav.etterlatte.sak.SakDao
import no.nav.etterlatte.sak.SakTilgangDao
import no.nav.etterlatte.tilgangsstyring.AzureGroup
import no.nav.etterlatte.tilgangsstyring.SaksbehandlerMedRoller
import no.nav.etterlatte.token.Saksbehandler
import no.nav.security.token.support.core.jwt.JwtTokenClaims
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import java.sql.Connection
import java.util.*
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OppgaveServiceNyTest {

    private lateinit var dataSource: DataSource
    private lateinit var sakDao: SakDao
    private lateinit var oppgaveDaoNy: OppgaveDaoNy
    private lateinit var oppgaveServiceNy: OppgaveServiceNy
    private lateinit var saktilgangDao: SakTilgangDao
    private lateinit var oppgaveDaoMedEndringssporing: OppgaveDaoMedEndringssporing
    private lateinit var featureToggleService: FeatureToggleService
    private val saksbehandlerRolleDev = "8bb9b8d1-f46a-4ade-8ee8-5895eccdf8cf"
    private val strengtfortroligDev = "5ef775f2-61f8-4283-bf3d-8d03f428aa14"
    private val attestantRolleDev = "63f46f74-84a8-4d1c-87a8-78532ab3ae60"
    private val saksbehandler = mockk<SaksbehandlerMedEnheterOgRoller>()

    @Container
    private val postgreSQLContainer = PostgreSQLContainer<Nothing>("postgres:$POSTGRES_VERSION")

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
        featureToggleService = DummyFeatureToggleService()
        sakDao = SakDao { connection }
        oppgaveDaoNy = OppgaveDaoNyImpl { connection }
        oppgaveDaoMedEndringssporing = OppgaveDaoMedEndringssporingImpl(oppgaveDaoNy) { connection }
        oppgaveServiceNy = OppgaveServiceNy(oppgaveDaoMedEndringssporing, sakDao, true, featureToggleService)
        saktilgangDao = SakTilgangDao(dataSource)
    }

    @BeforeEach
    fun beforeEach() {
        every { saksbehandler.enheter() } returns Enheter.nasjonalTilgangEnheter()
        Kontekst.set(
            Context(
                saksbehandler,
                object : DatabaseKontekst {
                    override fun activeTx(): Connection {
                        throw IllegalArgumentException()
                    }

                    override fun <T> inTransaction(gjenbruk: Boolean, block: () -> T): T {
                        return block()
                    }
                }
            )
        )
    }

    @AfterEach
    fun afterEach() {
        dataSource.connection.use {
            it.prepareStatement("TRUNCATE oppgave CASCADE;").execute()
        }
    }

    @Test
    fun `skal kunne tildele oppgave uten saksbehandler`() {
        val opprettetSak = sakDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val nyOppgave = oppgaveServiceNy.opprettNyOppgaveMedSakOgReferanse(
            "referanse",
            opprettetSak.id,
            OppgaveKilde.BEHANDLING,
            OppgaveType.FOERSTEGANGSBEHANDLING,
            null
        )
        val nysaksbehandler = "nysaksbehandler"
        oppgaveServiceNy.tildelSaksbehandler(nyOppgave.id, nysaksbehandler)

        val oppgaveMedNySaksbehandler = oppgaveServiceNy.hentOppgave(nyOppgave.id)
        Assertions.assertEquals(nysaksbehandler, oppgaveMedNySaksbehandler?.saksbehandler)
    }

    @Test
    fun `skal ikke kunne tildele oppgave med saksbehandler`() {
        val opprettetSak = sakDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val nyOppgave = oppgaveServiceNy.opprettNyOppgaveMedSakOgReferanse(
            "referanse",
            opprettetSak.id,
            OppgaveKilde.BEHANDLING,
            OppgaveType.FOERSTEGANGSBEHANDLING,
            null
        )
        val nysaksbehandler = "nysaksbehandler"
        oppgaveServiceNy.tildelSaksbehandler(nyOppgave.id, nysaksbehandler)
        val err = assertThrows<BadRequestException> {
            oppgaveServiceNy.tildelSaksbehandler(nyOppgave.id, "enda en")
        }
        Assertions.assertTrue(err.message!!.startsWith("Oppgaven har allerede en saksbehandler"))
    }

    @Test
    fun `skal ikke kunne tildele hvis oppgaven ikke finnes`() {
        val nysaksbehandler = "nysaksbehandler"
        val err = assertThrows<NotFoundException> {
            oppgaveServiceNy.tildelSaksbehandler(UUID.randomUUID(), nysaksbehandler)
        }
        Assertions.assertTrue(err.message!!.startsWith("Oppgaven finnes ikke"))
    }

    @Test
    fun `skal ikke kunne tildele en lukket oppgave`() {
        val opprettetSak = sakDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val nyOppgave = oppgaveServiceNy.opprettNyOppgaveMedSakOgReferanse(
            "referanse",
            opprettetSak.id,
            OppgaveKilde.BEHANDLING,
            OppgaveType.FOERSTEGANGSBEHANDLING,
            null
        )
        oppgaveDaoNy.endreStatusPaaOppgave(nyOppgave.id, Status.FERDIGSTILT)
        val nysaksbehandler = "nysaksbehandler"
        assertThrows<IllegalStateException> {
            oppgaveServiceNy.tildelSaksbehandler(nyOppgave.id, nysaksbehandler)
        }
    }

    @Test
    fun `avbrytAapneOppgaverForBehandling setter alle åpne oppgaver for behandling til avbrutt`() {
        val sak = sakDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val behandlingsId = UUID.randomUUID().toString()
        val oppgaveBehandling = oppgaveServiceNy.opprettNyOppgaveMedSakOgReferanse(
            referanse = behandlingsId,
            sakId = sak.id,
            oppgaveKilde = OppgaveKilde.BEHANDLING,
            oppgaveType = OppgaveType.FOERSTEGANGSBEHANDLING,
            merknad = null
        )
        val oppgaveAttestering = oppgaveServiceNy.opprettNyOppgaveMedSakOgReferanse(
            referanse = behandlingsId,
            sakId = sak.id,
            oppgaveKilde = OppgaveKilde.BEHANDLING,
            oppgaveType = OppgaveType.ATTESTERING,
            merknad = null
        )
        oppgaveServiceNy.tildelSaksbehandler(oppgaveBehandling.id, "saksbehandler")
        oppgaveServiceNy.avbrytAapneOppgaverForBehandling(behandlingsId)
        val oppgaveBehandlingEtterAvbryt = oppgaveServiceNy.hentOppgave(oppgaveBehandling.id)
        val oppgaveAttesteringEtterAvbryt = oppgaveServiceNy.hentOppgave(oppgaveAttestering.id)

        Assertions.assertEquals(Status.AVBRUTT, oppgaveBehandlingEtterAvbryt?.status)
        Assertions.assertEquals(Status.AVBRUTT, oppgaveAttesteringEtterAvbryt?.status)
    }

    @Test
    fun `avbrytAapneOppgaverForBehandling endrer ikke avsluttede oppgaver eller oppgaver til andre behandlinger`() {
        val sak = sakDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val behandlingId = UUID.randomUUID().toString()
        val annenBehandlingId = UUID.randomUUID().toString()
        val saksbehandler = "saksbehandler"

        val oppgaveFerdigstilt = oppgaveServiceNy.opprettNyOppgaveMedSakOgReferanse(
            referanse = behandlingId,
            sakId = sak.id,
            oppgaveKilde = OppgaveKilde.BEHANDLING,
            oppgaveType = OppgaveType.FOERSTEGANGSBEHANDLING,
            merknad = null
        )
        oppgaveServiceNy.tildelSaksbehandler(oppgaveFerdigstilt.id, saksbehandler)
        oppgaveServiceNy.ferdigStillOppgaveUnderBehandling(behandlingId, saksbehandler)

        val oppgaveUnderBehandlingAnnenBehandling = oppgaveServiceNy.opprettNyOppgaveMedSakOgReferanse(
            referanse = annenBehandlingId,
            sakId = sak.id,
            oppgaveKilde = OppgaveKilde.BEHANDLING,
            oppgaveType = OppgaveType.ATTESTERING,
            merknad = null
        )
        oppgaveServiceNy.tildelSaksbehandler(oppgaveUnderBehandlingAnnenBehandling.id, saksbehandler)
        oppgaveServiceNy.avbrytAapneOppgaverForBehandling(behandlingId)

        val oppgaveFerdigstiltEtterAvbryt = oppgaveServiceNy.hentOppgave(oppgaveFerdigstilt.id)
        val oppgaveUnderBehandlingEtterAvbryt = oppgaveServiceNy.hentOppgave(oppgaveUnderBehandlingAnnenBehandling.id)
        Assertions.assertEquals(Status.FERDIGSTILT, oppgaveFerdigstiltEtterAvbryt?.status)
        Assertions.assertEquals(Status.UNDER_BEHANDLING, oppgaveUnderBehandlingEtterAvbryt?.status)
    }

    @Test
    fun `skal kunne bytte oppgave med saksbehandler`() {
        val opprettetSak = sakDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val nyOppgave = oppgaveServiceNy.opprettNyOppgaveMedSakOgReferanse(
            "referanse",
            opprettetSak.id,
            OppgaveKilde.BEHANDLING,
            OppgaveType.FOERSTEGANGSBEHANDLING,
            null
        )
        val nysaksbehandler = "nysaksbehandler"
        oppgaveServiceNy.byttSaksbehandler(nyOppgave.id, nysaksbehandler)

        val oppgaveMedNySaksbehandler = oppgaveServiceNy.hentOppgave(nyOppgave.id)
        Assertions.assertEquals(nysaksbehandler, oppgaveMedNySaksbehandler?.saksbehandler)
    }

    @Test
    fun `skal ikke kunne bytte saksbehandler på lukket oppgave`() {
        val opprettetSak = sakDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val nyOppgave = oppgaveServiceNy.opprettNyOppgaveMedSakOgReferanse(
            "referanse",
            opprettetSak.id,
            OppgaveKilde.BEHANDLING,
            OppgaveType.FOERSTEGANGSBEHANDLING,
            null
        )
        oppgaveDaoNy.endreStatusPaaOppgave(nyOppgave.id, Status.FERDIGSTILT)
        val nysaksbehandler = "nysaksbehandler"
        assertThrows<IllegalStateException> {
            oppgaveServiceNy.byttSaksbehandler(nyOppgave.id, nysaksbehandler)
        }
        val oppgaveMedNySaksbehandler = oppgaveServiceNy.hentOppgave(nyOppgave.id)
        Assertions.assertEquals(nyOppgave.saksbehandler, oppgaveMedNySaksbehandler?.saksbehandler)
    }

    @Test
    fun `hvis oppgaveListeErAv settes saksbehandler før oppgaver lukkes`() {
        val saksbehandler = "saksbehandler"
        val opprettetSak = sakDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val referanse = "behandlingId"

        val oppgaveServiceNaarOppgaveIkkeErPaa =
            OppgaveServiceNy(oppgaveDaoMedEndringssporing, sakDao, false, featureToggleService)
        val nyOppgave = oppgaveServiceNaarOppgaveIkkeErPaa.opprettNyOppgaveMedSakOgReferanse(
            referanse = referanse,
            sakId = opprettetSak.id,
            oppgaveType = OppgaveType.FOERSTEGANGSBEHANDLING,
            oppgaveKilde = OppgaveKilde.BEHANDLING,
            merknad = null
        )
        oppgaveServiceNaarOppgaveIkkeErPaa.lukkOppgaveUnderbehandlingOgLagNyMedType(
            fattetoppgave = VedtakOppgaveDTO(sakId = opprettetSak.id, referanse = referanse),
            oppgaveType = OppgaveType.ATTESTERING,
            saksbehandler = saksbehandler,
            merknad = null
        )
        val oppgaveEtterLukking = oppgaveServiceNaarOppgaveIkkeErPaa.hentOppgave(nyOppgave.id)
        Assertions.assertEquals(saksbehandler, oppgaveEtterLukking?.saksbehandler)
    }

    @Test
    fun `skal ikke kunne bytte saksbehandler på en ikke eksisterende sak`() {
        val nysaksbehandler = "nysaksbehandler"
        val err = assertThrows<NotFoundException> {
            oppgaveServiceNy.byttSaksbehandler(UUID.randomUUID(), nysaksbehandler)
        }
        Assertions.assertTrue(err.message!!.startsWith("Oppgaven finnes ikke"))
    }

    @Test
    fun `skal kunne fjerne saksbehandler fra oppgave`() {
        val opprettetSak = sakDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val nyOppgave = oppgaveServiceNy.opprettNyOppgaveMedSakOgReferanse(
            "referanse",
            opprettetSak.id,
            OppgaveKilde.BEHANDLING,
            OppgaveType.FOERSTEGANGSBEHANDLING,
            null
        )
        val nysaksbehandler = "nysaksbehandler"
        oppgaveServiceNy.tildelSaksbehandler(nyOppgave.id, nysaksbehandler)
        oppgaveServiceNy.fjernSaksbehandler(nyOppgave.id)
        val oppgaveUtenSaksbehandler = oppgaveServiceNy.hentOppgave(nyOppgave.id)
        Assertions.assertNotNull(oppgaveUtenSaksbehandler?.id)
        Assertions.assertNull(oppgaveUtenSaksbehandler?.saksbehandler)
        Assertions.assertEquals(Status.NY, oppgaveUtenSaksbehandler?.status)
    }

    @Test
    fun `kan ikke fjerne saksbehandler hvis det ikke er satt på oppgaven`() {
        val opprettetSak = sakDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val nyOppgave = oppgaveServiceNy.opprettNyOppgaveMedSakOgReferanse(
            "referanse",
            opprettetSak.id,
            OppgaveKilde.BEHANDLING,
            OppgaveType.FOERSTEGANGSBEHANDLING,
            null
        )
        val err = assertThrows<BadRequestException> {
            oppgaveServiceNy.fjernSaksbehandler(nyOppgave.id)
        }
        Assertions.assertTrue(err.message!!.startsWith("Oppgaven har ingen saksbehandler"))
    }

    @Test
    fun `skal ikke kunne fjerne saksbehandler på en lukket oppgave`() {
        val opprettetSak = sakDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val nyOppgave = oppgaveServiceNy.opprettNyOppgaveMedSakOgReferanse(
            "referanse",
            opprettetSak.id,
            OppgaveKilde.BEHANDLING,
            OppgaveType.FOERSTEGANGSBEHANDLING,
            null
        )
        val saksbehandler = "saksbehandler"
        oppgaveServiceNy.tildelSaksbehandler(nyOppgave.id, saksbehandler)
        oppgaveDaoNy.endreStatusPaaOppgave(nyOppgave.id, Status.FERDIGSTILT)
        assertThrows<IllegalStateException> {
            oppgaveServiceNy.fjernSaksbehandler(nyOppgave.id)
        }
        val lagretOppgave = oppgaveServiceNy.hentOppgave(nyOppgave.id)

        Assertions.assertEquals(lagretOppgave?.saksbehandler, saksbehandler)
    }

    @Test
    fun `kan redigere frist`() {
        val opprettetSak = sakDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val nyOppgave = oppgaveServiceNy.opprettNyOppgaveMedSakOgReferanse(
            "referanse",
            opprettetSak.id,
            OppgaveKilde.BEHANDLING,
            OppgaveType.FOERSTEGANGSBEHANDLING,
            null
        )
        oppgaveServiceNy.tildelSaksbehandler(nyOppgave.id, "nysaksbehandler")
        val nyFrist = Tidspunkt.now().toLocalDatetimeUTC().plusMonths(4L).toTidspunkt()
        oppgaveServiceNy.redigerFrist(nyOppgave.id, nyFrist)
        val oppgaveMedNyFrist = oppgaveServiceNy.hentOppgave(nyOppgave.id)
        Assertions.assertEquals(nyFrist, oppgaveMedNyFrist?.frist)
    }

    @Test
    fun `kan ikke redigere frist tilbake i tid`() {
        val opprettetSak = sakDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val nyOppgave = oppgaveServiceNy.opprettNyOppgaveMedSakOgReferanse(
            "referanse",
            opprettetSak.id,
            OppgaveKilde.BEHANDLING,
            OppgaveType.FOERSTEGANGSBEHANDLING,
            null
        )
        oppgaveServiceNy.tildelSaksbehandler(nyOppgave.id, "nysaksbehandler")
        val nyFrist = Tidspunkt.now().toLocalDatetimeUTC().minusMonths(1L).toTidspunkt()

        val err = assertThrows<BadRequestException> {
            oppgaveServiceNy.redigerFrist(nyOppgave.id, nyFrist)
        }

        Assertions.assertTrue(err.message!!.startsWith("Tidspunkt tilbake i tid id: "))
    }

    @Test
    fun `kan ikke redigere frist på en lukket oppgave`() {
        val opprettetSak = sakDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val nyOppgave = oppgaveServiceNy.opprettNyOppgaveMedSakOgReferanse(
            "referanse",
            opprettetSak.id,
            OppgaveKilde.BEHANDLING,
            OppgaveType.FOERSTEGANGSBEHANDLING,
            null
        )

        oppgaveDaoNy.endreStatusPaaOppgave(nyOppgave.id, Status.FERDIGSTILT)
        assertThrows<IllegalStateException> {
            oppgaveServiceNy.redigerFrist(
                oppgaveId = nyOppgave.id,
                frist = Tidspunkt.now().toLocalDatetimeUTC().plusMonths(1L).toTidspunkt()
            )
        }
        val lagretOppgave = oppgaveServiceNy.hentOppgave(nyOppgave.id)
        Assertions.assertEquals(nyOppgave.frist, lagretOppgave?.frist)
    }

    @Test
    fun `Håndtering av vedtaksfatting`() {
        val opprettetSak = sakDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val referanse = "referanse"
        val nyOppgave = oppgaveServiceNy.opprettNyOppgaveMedSakOgReferanse(
            referanse,
            opprettetSak.id,
            OppgaveKilde.BEHANDLING,
            OppgaveType.FOERSTEGANGSBEHANDLING,
            null
        )

        oppgaveServiceNy.tildelSaksbehandler(nyOppgave.id, "saksbehandler")

        val vedtakOppgaveDTO = oppgaveServiceNy.lukkOppgaveUnderbehandlingOgLagNyMedType(
            VedtakOppgaveDTO(opprettetSak.id, referanse),
            OppgaveType.ATTESTERING,
            null
        )

        val saksbehandlerOppgave = oppgaveServiceNy.hentOppgave(nyOppgave.id)
        Assertions.assertEquals(Status.FERDIGSTILT, saksbehandlerOppgave?.status)
        Assertions.assertEquals(OppgaveType.ATTESTERING, vedtakOppgaveDTO.type)
        Assertions.assertEquals(referanse, vedtakOppgaveDTO.referanse)
    }

    @Test
    fun `Skal ikke kunne attestere vedtak hvis ingen oppgaver er under behandling altså tildelt en saksbehandler`() {
        val opprettetSak = sakDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val referanse = "referanse"
        oppgaveServiceNy.opprettNyOppgaveMedSakOgReferanse(
            referanse,
            opprettetSak.id,
            OppgaveKilde.BEHANDLING,
            OppgaveType.FOERSTEGANGSBEHANDLING,
            null
        )

        val err = assertThrows<BadRequestException> {
            oppgaveServiceNy.lukkOppgaveUnderbehandlingOgLagNyMedType(
                VedtakOppgaveDTO(opprettetSak.id, referanse),
                OppgaveType.ATTESTERING,
                null
            )
        }

        Assertions.assertTrue(
            err.message!!.startsWith("Det må finnes en oppgave under behandling, gjelder behandling:")
        )
    }

    @Test
    fun `kan ikke attestere uten at det finnes en oppgave på behandlingen`() {
        val opprettetSak = sakDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val referanse = "referanse"

        val err = assertThrows<BadRequestException> {
            oppgaveServiceNy.lukkOppgaveUnderbehandlingOgLagNyMedType(
                VedtakOppgaveDTO(opprettetSak.id, referanse),
                OppgaveType.ATTESTERING,
                null
            )
        }

        Assertions.assertEquals(
            "Må ha en oppgave for å kunne lage attesteringsoppgave",
            err.message!!
        )
    }

    @Test
    fun `Skal ikke kunne attestere vedtak hvis det finnes flere oppgaver under behandling for behandlingen`() {
        val opprettetSak = sakDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val referanse = "referanse"
        val oppgaveEn = oppgaveServiceNy.opprettNyOppgaveMedSakOgReferanse(
            referanse,
            opprettetSak.id,
            OppgaveKilde.BEHANDLING,
            OppgaveType.FOERSTEGANGSBEHANDLING,
            null
        )

        val oppgaveTo = oppgaveServiceNy.opprettNyOppgaveMedSakOgReferanse(
            referanse,
            opprettetSak.id,
            OppgaveKilde.BEHANDLING,
            OppgaveType.FOERSTEGANGSBEHANDLING,
            null
        )
        oppgaveServiceNy.tildelSaksbehandler(oppgaveEn.id, "saksbehandler")
        oppgaveServiceNy.tildelSaksbehandler(oppgaveTo.id, "saksbehandler")

        val err = assertThrows<BadRequestException> {
            oppgaveServiceNy.lukkOppgaveUnderbehandlingOgLagNyMedType(
                VedtakOppgaveDTO(opprettetSak.id, referanse),
                OppgaveType.ATTESTERING,
                null
            )
        }

        Assertions.assertTrue(
            err.message!!.startsWith("Skal kun ha en oppgave under behandling, gjelder behandling:")
        )
    }

    @Test
    fun `kan ikke fjerne saksbehandler hvis oppgaven ikke finnes`() {
        val err = assertThrows<NotFoundException> {
            oppgaveServiceNy.fjernSaksbehandler(UUID.randomUUID())
        }
        Assertions.assertTrue(err.message!!.startsWith("Oppgaven finnes ikke"))
    }

    @Test
    fun `Skal kun få saker som ikke er adressebeskyttet tilbake hvis saksbehandler ikke har spesialroller`() {
        val opprettetSak = sakDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val nyOppgave = oppgaveServiceNy.opprettNyOppgaveMedSakOgReferanse(
            "referanse",
            opprettetSak.id,
            OppgaveKilde.BEHANDLING,
            OppgaveType.FOERSTEGANGSBEHANDLING,
            null
        )

        val adressebeskyttetSak = sakDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        oppgaveServiceNy.opprettNyOppgaveMedSakOgReferanse(
            "referanse",
            adressebeskyttetSak.id,
            OppgaveKilde.BEHANDLING,
            OppgaveType.FOERSTEGANGSBEHANDLING,
            null
        )

        saktilgangDao.oppdaterAdresseBeskyttelse(adressebeskyttetSak.id, AdressebeskyttelseGradering.STRENGT_FORTROLIG)
        val jwtclaims = JWTClaimsSet.Builder().claim("groups", saksbehandlerRolleDev).build()
        val saksbehandlerMedRoller = SaksbehandlerMedRoller(
            Saksbehandler("", "ident", JwtTokenClaims(jwtclaims)),
            mapOf(AzureGroup.SAKSBEHANDLER to saksbehandlerRolleDev)
        )
        val oppgaver = oppgaveServiceNy.finnOppgaverForBruker(saksbehandlerMedRoller)
        Assertions.assertEquals(1, oppgaver.size)
        val oppgaveUtenbeskyttelse = oppgaver[0]
        Assertions.assertEquals(nyOppgave.id, oppgaveUtenbeskyttelse.id)
        Assertions.assertEquals(nyOppgave.sakId, opprettetSak.id)
    }

    @Test
    fun `Skal kunne endre enhet for oppgaver tilknyttet sak`() {
        val opprettetSak = sakDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        oppgaveServiceNy.opprettNyOppgaveMedSakOgReferanse(
            referanse = "referanse",
            sakId = opprettetSak.id,
            oppgaveKilde = OppgaveKilde.BEHANDLING,
            oppgaveType = OppgaveType.FOERSTEGANGSBEHANDLING,
            merknad = null
        )

        val jwtclaims = JWTClaimsSet.Builder().claim("groups", saksbehandlerRolleDev).build()
        val saksbehandlerMedRoller = SaksbehandlerMedRoller(
            Saksbehandler("", "ident", JwtTokenClaims(jwtclaims)),
            mapOf(AzureGroup.SAKSBEHANDLER to saksbehandlerRolleDev)
        )
        val oppgaverUtenEndring = oppgaveServiceNy.finnOppgaverForBruker(saksbehandlerMedRoller)
        Assertions.assertEquals(1, oppgaverUtenEndring.size)
        Assertions.assertEquals(Enheter.AALESUND.enhetNr, oppgaverUtenEndring[0].enhet)

        oppgaveServiceNy.endreEnhetForOppgaverTilknyttetSak(opprettetSak.id, Enheter.STEINKJER.enhetNr)
        val oppgaverMedEndring = oppgaveServiceNy.finnOppgaverForBruker(saksbehandlerMedRoller)
        Assertions.assertEquals(1, oppgaverMedEndring.size)
        Assertions.assertEquals(Enheter.STEINKJER.enhetNr, oppgaverMedEndring[0].enhet)
    }

    @Test
    fun `Skal kun få saker som  er strengt fotrolig tilbake hvis saksbehandler har rolle strengt fortrolig`() {
        val opprettetSak = sakDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        oppgaveServiceNy.opprettNyOppgaveMedSakOgReferanse(
            "referanse",
            opprettetSak.id,
            OppgaveKilde.BEHANDLING,
            OppgaveType.FOERSTEGANGSBEHANDLING,
            null
        )

        val adressebeskyttetSak = sakDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val adressebeskyttetOppgave = oppgaveServiceNy.opprettNyOppgaveMedSakOgReferanse(
            "referanse",
            adressebeskyttetSak.id,
            OppgaveKilde.BEHANDLING,
            OppgaveType.FOERSTEGANGSBEHANDLING,
            null
        )

        saktilgangDao.oppdaterAdresseBeskyttelse(adressebeskyttetSak.id, AdressebeskyttelseGradering.STRENGT_FORTROLIG)
        val jwtclaims = JWTClaimsSet.Builder().claim("groups", strengtfortroligDev).build()
        val saksbehandlerMedRoller = SaksbehandlerMedRoller(
            Saksbehandler("", "ident", JwtTokenClaims(jwtclaims)),
            mapOf(AzureGroup.STRENGT_FORTROLIG to strengtfortroligDev)
        )
        val oppgaver = oppgaveServiceNy.finnOppgaverForBruker(saksbehandlerMedRoller)
        Assertions.assertEquals(1, oppgaver.size)
        val strengtFortroligOppgave = oppgaver[0]
        Assertions.assertEquals(adressebeskyttetOppgave.id, strengtFortroligOppgave.id)
        Assertions.assertEquals(adressebeskyttetOppgave.sakId, adressebeskyttetSak.id)
    }

    @Test
    fun `saksbehandler med attestant rolle skal få attestant oppgaver`() {
        val opprettetSak = sakDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        oppgaveServiceNy.opprettNyOppgaveMedSakOgReferanse(
            "referanse",
            opprettetSak.id,
            OppgaveKilde.BEHANDLING,
            OppgaveType.FOERSTEGANGSBEHANDLING,
            null
        )

        val attestantSak = sakDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val attestantOppgave = oppgaveServiceNy.opprettNyOppgaveMedSakOgReferanse(
            "referanse",
            attestantSak.id,
            OppgaveKilde.BEHANDLING,
            OppgaveType.ATTESTERING,
            null
        )

        val jwtclaims = JWTClaimsSet.Builder().claim("groups", attestantRolleDev).build()
        val saksbehandlerMedRoller = SaksbehandlerMedRoller(
            Saksbehandler("", "ident", JwtTokenClaims(jwtclaims)),
            mapOf(AzureGroup.ATTESTANT to attestantRolleDev)
        )
        val oppgaver = oppgaveServiceNy.finnOppgaverForBruker(saksbehandlerMedRoller)
        Assertions.assertEquals(1, oppgaver.size)
        val strengtFortroligOppgave = oppgaver[0]
        Assertions.assertEquals(attestantOppgave.id, strengtFortroligOppgave.id)
        Assertions.assertEquals(attestantOppgave.sakId, attestantSak.id)
    }

    @Test
    fun `skal tracke at en tildeling av saksbehandler blir lagret med oppgaveendringer`() {
        val opprettetSak = sakDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val nyOppgave = oppgaveServiceNy.opprettNyOppgaveMedSakOgReferanse(
            "referanse",
            opprettetSak.id,
            OppgaveKilde.BEHANDLING,
            OppgaveType.FOERSTEGANGSBEHANDLING,
            null
        )
        val nysaksbehandler = "nysaksbehandler"
        oppgaveServiceNy.tildelSaksbehandler(nyOppgave.id, nysaksbehandler)

        val oppgaveMedNySaksbehandler = oppgaveServiceNy.hentOppgave(nyOppgave.id)
        Assertions.assertEquals(nysaksbehandler, oppgaveMedNySaksbehandler?.saksbehandler)

        val hentEndringerForOppgave = oppgaveDaoMedEndringssporing.hentEndringerForOppgave(nyOppgave.id)
        Assertions.assertEquals(1, hentEndringerForOppgave.size)
        val endringPaaOppgave = hentEndringerForOppgave[0]
        Assertions.assertNull(endringPaaOppgave.oppgaveFoer.saksbehandler)
        Assertions.assertEquals("nysaksbehandler", endringPaaOppgave.oppgaveEtter.saksbehandler)
        Assertions.assertEquals(Status.NY, endringPaaOppgave.oppgaveFoer.status)
        Assertions.assertEquals(Status.UNDER_BEHANDLING, endringPaaOppgave.oppgaveEtter.status)
    }

    @Test
    fun `skal ferdigstille en oppgave hivs det finnes kun en som er under behandling`() {
        val opprettetSak = sakDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val behandlingsref = UUID.randomUUID().toString()
        val oppgave = oppgaveServiceNy.opprettNyOppgaveMedSakOgReferanse(
            behandlingsref,
            opprettetSak.id,
            OppgaveKilde.BEHANDLING,
            OppgaveType.FOERSTEGANGSBEHANDLING,
            null
        )

        oppgaveServiceNy.tildelSaksbehandler(oppgave.id, "saksbehandler01")
        oppgaveServiceNy.ferdigStillOppgaveUnderBehandling(behandlingsref)
        val ferdigstiltOppgave = oppgaveServiceNy.hentOppgave(oppgave.id)
        Assertions.assertEquals(Status.FERDIGSTILT, ferdigstiltOppgave?.status)
    }

    @Test
    fun `skal lukke nye ikke ferdige eller feilregistrerte oppgaver hvis ny søknad kommer inn`() {
        val opprettetSak = sakDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val behandlingsref = UUID.randomUUID().toString()
        val oppgaveSomSkalBliAvbrutt = oppgaveServiceNy.opprettNyOppgaveMedSakOgReferanse(
            behandlingsref,
            opprettetSak.id,
            OppgaveKilde.BEHANDLING,
            OppgaveType.FOERSTEGANGSBEHANDLING,
            null
        )
        oppgaveServiceNy.tildelSaksbehandler(oppgaveSomSkalBliAvbrutt.id, "saksbehandler01")

        oppgaveServiceNy.opprettFoerstegangsbehandlingsOppgaveForInnsendSoeknad(behandlingsref, opprettetSak.id)

        val alleOppgaver = oppgaveDaoNy.hentOppgaverForBehandling(behandlingsref)
        Assertions.assertEquals(2, alleOppgaver.size)
        val avbruttOppgave = oppgaveDaoNy.hentOppgave(oppgaveSomSkalBliAvbrutt.id)!!
        Assertions.assertEquals(avbruttOppgave.status, Status.AVBRUTT)
    }

    @Test
    fun `Skal filtrere bort oppgaver med annen enhet`() {
        every { saksbehandler.enheter() } returns listOf(Enheter.AALESUND.enhetNr)
        Kontekst.set(
            Context(
                saksbehandler,
                object : DatabaseKontekst {
                    override fun activeTx(): Connection {
                        throw IllegalArgumentException()
                    }

                    override fun <T> inTransaction(gjenbruk: Boolean, block: () -> T): T {
                        return block()
                    }
                }
            )
        )

        val aalesundSak = sakDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val behandlingsref = UUID.randomUUID().toString()
        val oppgaveAalesund = oppgaveServiceNy.opprettNyOppgaveMedSakOgReferanse(
            behandlingsref,
            aalesundSak.id,
            OppgaveKilde.BEHANDLING,
            OppgaveType.FOERSTEGANGSBEHANDLING,
            null
        )
        val saksbehandlerid = "saksbehandler01"
        oppgaveServiceNy.tildelSaksbehandler(oppgaveAalesund.id, saksbehandlerid)

        val saksteinskjer = sakDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.STEINKJER.enhetNr)
        val behrefsteinkjer = UUID.randomUUID().toString()
        val oppgavesteinskjer = oppgaveServiceNy.opprettNyOppgaveMedSakOgReferanse(
            behrefsteinkjer,
            saksteinskjer.id,
            OppgaveKilde.BEHANDLING,
            OppgaveType.FOERSTEGANGSBEHANDLING,
            null
        )

        oppgaveServiceNy.tildelSaksbehandler(oppgavesteinskjer.id, saksbehandlerid)

        val jwtclaims = JWTClaimsSet.Builder().claim("groups", saksbehandlerRolleDev).build()
        val saksbehandlerMedRoller = SaksbehandlerMedRoller(
            Saksbehandler("", "ident", JwtTokenClaims(jwtclaims)),
            mapOf(AzureGroup.SAKSBEHANDLER to saksbehandlerRolleDev)
        )
        val finnOppgaverForBruker = oppgaveServiceNy.finnOppgaverForBruker(saksbehandlerMedRoller)

        Assertions.assertEquals(1, finnOppgaverForBruker.size)
        val AalesundfunnetOppgave = finnOppgaverForBruker[0]
        Assertions.assertEquals(Enheter.AALESUND.enhetNr, AalesundfunnetOppgave.enhet)
    }

    @Test
    fun `kan hente saksbehandler på en oppgave tilknyttet behandling som er under arbeid`() {
        val opprettetSak = sakDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val behandlingId = UUID.randomUUID().toString()
        val nyOppgave = oppgaveServiceNy.opprettNyOppgaveMedSakOgReferanse(
            behandlingId,
            opprettetSak.id,
            OppgaveKilde.BEHANDLING,
            OppgaveType.FOERSTEGANGSBEHANDLING,
            null
        )
        val saksbehandler = "saksbehandler"

        oppgaveServiceNy.tildelSaksbehandler(nyOppgave.id, saksbehandler)

        val saksbehandlerHentet =
            oppgaveServiceNy.hentSaksbehandlerForBehandling(UUID.fromString(behandlingId))

        Assertions.assertEquals(saksbehandler, saksbehandlerHentet)
    }
}