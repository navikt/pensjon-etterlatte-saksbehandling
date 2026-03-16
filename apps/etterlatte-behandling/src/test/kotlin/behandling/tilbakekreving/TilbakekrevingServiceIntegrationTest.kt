package behandling.tilbakekreving

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.spyk
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.BehandlingIntegrationTest
import no.nav.etterlatte.SaksbehandlerMedEnheterOgRoller
import no.nav.etterlatte.behandling.hendelse.HendelseDao
import no.nav.etterlatte.behandling.klienter.BrevApiKlient
import no.nav.etterlatte.behandling.klienter.TilbakekrevingKlient
import no.nav.etterlatte.behandling.klienter.VedtakKlient
import no.nav.etterlatte.behandling.randomSakId
import no.nav.etterlatte.behandling.tilbakekreving.TilbakekrevingDao
import no.nav.etterlatte.behandling.tilbakekreving.TilbakekrevingService
import no.nav.etterlatte.behandling.tilbakekreving.TilbakekrevingUnderBehandlingFinnesAlleredeException
import no.nav.etterlatte.brev.BrevKlient
import no.nav.etterlatte.brev.Brevkoder
import no.nav.etterlatte.brev.Brevtype
import no.nav.etterlatte.brev.model.Adresse
import no.nav.etterlatte.brev.model.Brev
import no.nav.etterlatte.brev.model.BrevProsessType
import no.nav.etterlatte.brev.model.Mottaker
import no.nav.etterlatte.brev.model.Spraak
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.funksjonsbrytere.DummyFeatureToggleService
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.kafka.TestProdusent
import no.nav.etterlatte.ktor.token.simpleAttestant
import no.nav.etterlatte.ktor.token.simpleSaksbehandler
import no.nav.etterlatte.libs.common.Enhetsnummer
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.oppgave.OppgaveKilde
import no.nav.etterlatte.libs.common.oppgave.OppgaveType
import no.nav.etterlatte.libs.common.oppgave.Status
import no.nav.etterlatte.libs.common.person.MottakerFoedselsnummer
import no.nav.etterlatte.libs.common.rapidsandrivers.EVENT_NAME_KEY
import no.nav.etterlatte.libs.common.rapidsandrivers.SKAL_SENDE_BREV
import no.nav.etterlatte.libs.common.sak.VedtakSak
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tilbakekreving.KlasseType
import no.nav.etterlatte.libs.common.tilbakekreving.Kontrollfelt
import no.nav.etterlatte.libs.common.tilbakekreving.TILBAKEKREVING_STATISTIKK_RIVER_KEY
import no.nav.etterlatte.libs.common.tilbakekreving.TilbakekrevingBehandling
import no.nav.etterlatte.libs.common.tilbakekreving.TilbakekrevingHendelseType
import no.nav.etterlatte.libs.common.tilbakekreving.TilbakekrevingPeriode
import no.nav.etterlatte.libs.common.tilbakekreving.TilbakekrevingResultat
import no.nav.etterlatte.libs.common.tilbakekreving.TilbakekrevingSkyld
import no.nav.etterlatte.libs.common.tilbakekreving.TilbakekrevingStatus
import no.nav.etterlatte.libs.common.toObjectNode
import no.nav.etterlatte.libs.common.toUUID30
import no.nav.etterlatte.libs.common.vedtak.Attestasjon
import no.nav.etterlatte.libs.common.vedtak.VedtakDto
import no.nav.etterlatte.libs.common.vedtak.VedtakFattet
import no.nav.etterlatte.libs.common.vedtak.VedtakInnholdDto
import no.nav.etterlatte.libs.common.vedtak.VedtakKafkaHendelseHendelseType
import no.nav.etterlatte.libs.common.vedtak.VedtakStatus
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import no.nav.etterlatte.libs.testdata.grunnlag.GrunnlagTestData
import no.nav.etterlatte.nyKontekstMedBrukerOgDatabase
import no.nav.etterlatte.oppgave.OppgaveService
import no.nav.etterlatte.sak.SakSkrivDao
import no.nav.etterlatte.tilgangsstyring.SaksbehandlerMedRoller
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import java.util.UUID
import kotlin.random.Random

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class TilbakekrevingServiceIntegrationTest : BehandlingIntegrationTest() {
    private lateinit var tilbakekrevingDao: TilbakekrevingDao
    private lateinit var sakSkrivDao: SakSkrivDao
    private lateinit var hendelseDao: HendelseDao
    private lateinit var service: TilbakekrevingService
    private lateinit var oppgaveService: OppgaveService
    private lateinit var vedtakKlient: VedtakKlient

    private val brevApiKlient: BrevApiKlient = mockk()
    private val brevKlient: BrevKlient = mockk()
    private val tilbakekrevingKlient: TilbakekrevingKlient = mockk()
    private val rapid: TestProdusent<String, String> = spyk(TestProdusent())

    private val saksbehandler = simpleSaksbehandler()
    private val attestant = simpleAttestant()
    private val bruker = GrunnlagTestData().gjenlevende.foedselsnummer.value
    private val enhet = Enheter.defaultEnhet.enhetNr

    @BeforeEach
    fun setUp() {
        service = applicationContext.tilbakekrevingService
        sakSkrivDao = applicationContext.sakSkrivDao
        tilbakekrevingDao = applicationContext.tilbakekrevingDao
        hendelseDao = applicationContext.hendelseDao
        oppgaveService = applicationContext.oppgaveService
        vedtakKlient = applicationContext.vedtakKlient
    }

    @BeforeAll
    fun start() {
        val user =
            mockk<SaksbehandlerMedEnheterOgRoller> {
                every { name() } returns "User"
                every { enheter() } returns listOf(Enheter.defaultEnhet.enhetNr)
                every { saksbehandlerMedRoller } returns
                    mockk<SaksbehandlerMedRoller> {
                        every { harRolleAttestant() } returns true
                        every { harRolleStrengtFortrolig() } returns false
                        every { harRolleEgenAnsatt() } returns false
                    }
            }

        startServer(
            featureToggleService = DummyFeatureToggleService(),
            brevApiKlient = brevApiKlient,
            brevKlient = brevKlient,
            tilbakekrevingKlient = tilbakekrevingKlient,
            testProdusent = rapid,
        )

        nyKontekstMedBrukerOgDatabase(user, applicationContext.dataSource)
    }

    @AfterAll
    fun afterAllTests() {
        afterAll()
    }

    @AfterEach
    fun afterEachTest() {
        resetDatabase()
        rapid.publiserteMeldinger.clear()
    }

    @Test
    fun `skal opprette tilbakekrevingsbehandling fra kravgrunnlag og koble mot eksisterende oppgave`() {
        val sak = inTransaction { sakSkrivDao.opprettSak(bruker, SakType.BARNEPENSJON, enhet) }

        val oppgaveFraBehandlingMedFeilutbetaling =
            inTransaction {
                oppgaveService.opprettOppgave(
                    referanse = sak.id.toString(),
                    sakId = sak.id,
                    kilde = OppgaveKilde.TILBAKEKREVING,
                    type = OppgaveType.TILBAKEKREVING,
                    merknad = "Venter på kravgrunnlag",
                )
            }

        oppgaveFraBehandlingMedFeilutbetaling.referanse shouldBe sak.id.toString()
        oppgaveFraBehandlingMedFeilutbetaling.status shouldBe Status.NY
        oppgaveFraBehandlingMedFeilutbetaling.merknad shouldBe "Venter på kravgrunnlag"

        val tilbakekreving = service.opprettTilbakekreving(kravgrunnlag(sak), null, null)
        val sisteLagretHendelse = inTransaction { hendelseDao.hentHendelserISak(sak.id).maxBy { it.opprettet } }
        val oppgave = inTransaction { oppgaveService.hentOppgaverForReferanse(tilbakekreving.id.toString()).first() }

        oppgave.id shouldBe oppgaveFraBehandlingMedFeilutbetaling.id
        oppgave.referanse shouldBe tilbakekreving.id.toString()
        oppgave.status shouldBe Status.NY
        oppgave.merknad shouldBe "Kravgrunnlag mottatt"

        with(sisteLagretHendelse) {
            sakId shouldBe sak.id
            vedtakId shouldBe null
            this.behandlingId shouldBe tilbakekreving.id
            hendelse shouldBe TilbakekrevingHendelseType.OPPRETTET.lagEventnameForType()
            ident shouldBe null
            identType shouldBe null
            inntruffet shouldNotBe null
            opprettet shouldNotBe null
            kommentar shouldBe null
            valgtBegrunnelse shouldBe null
        }

        rapid.publiserteMeldinger.size shouldBe 1
        rapid.publiserteMeldinger.last().let { melding ->
            objectMapper.readTree(melding.verdi).let {
                it[EVENT_NAME_KEY].textValue() shouldBe TilbakekrevingHendelseType.OPPRETTET.lagEventnameForType()
                it[TILBAKEKREVING_STATISTIKK_RIVER_KEY] shouldNotBe null
            }
        }
    }

    @Test
    fun `skal opprette tilbakekrevingsbehandling fra kravgrunnlag og lage ny oppgave hvis eksisterende ikke finnes`() {
        val sak = inTransaction { sakSkrivDao.opprettSak(bruker, SakType.BARNEPENSJON, enhet) }
        val behandlingId = UUID.randomUUID()

        val oppgaverFraReferanse =
            inTransaction {
                oppgaveService.hentOppgaverForReferanse(behandlingId.toUUID30().value)
            }

        oppgaverFraReferanse.size shouldBe 0

        val tilbakekreving = service.opprettTilbakekreving(kravgrunnlag(sak), null, null)
        val oppgave = inTransaction { oppgaveService.hentOppgaverForReferanse(tilbakekreving.id.toString()).first() }

        oppgave.referanse shouldBe tilbakekreving.id.toString()
        oppgave.status shouldBe Status.NY
        oppgave.merknad shouldBe "Kravgrunnlag mottatt"

        rapid.publiserteMeldinger.size shouldBe 1
        rapid.publiserteMeldinger.last().let { melding ->
            objectMapper.readTree(melding.verdi).let {
                it[EVENT_NAME_KEY].textValue() shouldBe TilbakekrevingHendelseType.OPPRETTET.lagEventnameForType()
                it[TILBAKEKREVING_STATISTIKK_RIVER_KEY] shouldNotBe null
            }
        }
    }

    @Test
    fun `skal opprette tilbakekrevingsbehandling fra kravgrunnlag og sette paa vent uten tildelt saksbehandler`() {
        val sak = inTransaction { sakSkrivDao.opprettSak(bruker, SakType.BARNEPENSJON, enhet) }
        val tilbakekreving = service.opprettTilbakekreving(kravgrunnlag(sak), null, null)

        service.endreTilbakekrevingOppgaveStatus(sak.id, paaVent = true)

        val oppgavePaaVent =
            inTransaction { oppgaveService.hentOppgaverForReferanse(tilbakekreving.id.toString()).first() }
        oppgavePaaVent.status shouldBe Status.PAA_VENT

        service.endreTilbakekrevingOppgaveStatus(sak.id, paaVent = false)

        val oppgave = inTransaction { oppgaveService.hentOppgaverForReferanse(tilbakekreving.id.toString()).first() }
        oppgave.status shouldBe Status.NY
    }

    @Test
    fun `skal ikke kunne opprette tilbakekrevingsbehandling dersom det allerede finnes en`() {
        val sak = inTransaction { sakSkrivDao.opprettSak(bruker, SakType.BARNEPENSJON, enhet) }

        val tilbakekreving = service.opprettTilbakekreving(kravgrunnlag(sak), null, null)
        tilbakekreving.status shouldBe TilbakekrevingStatus.OPPRETTET

        assertThrows<TilbakekrevingUnderBehandlingFinnesAlleredeException> {
            service.opprettTilbakekreving(kravgrunnlag(sak), null, null)
        }
    }

    @Test
    fun `skal avbryte tilbakekrevingsbehandling`() {
        coEvery { vedtakKlient.fattVedtakTilbakekreving(any(), any(), any()) } returns vedtak()
        coEvery { brevApiKlient.hentVedtaksbrev(any(), any()) } returns vedtaksbrev()

        // Oppretter sak og tilbakekreving basert på kravgrunnlag
        val sak = inTransaction { sakSkrivDao.opprettSak(bruker, SakType.BARNEPENSJON, enhet) }
        val tilbakekreving = service.opprettTilbakekreving(kravgrunnlag(sak), null, null)
        val oppgave = inTransaction { oppgaveService.hentOppgaverForReferanse(tilbakekreving.id.toString()).first() }

        // Tildeler oppgaven til saksbehandler
        inTransaction { oppgaveService.tildelSaksbehandler(oppgave.id, saksbehandler.ident) }

        // Lagrer vurdering og perioder
        service.lagreVurdering(tilbakekreving.id, tilbakekrevingVurdering(), saksbehandler)
        service.lagrePerioder(tilbakekreving.id, tilbakekrevingPerioder(tilbakekreving), saksbehandler)
        service.validerVurderingOgPerioder(tilbakekreving.id, saksbehandler)

        // Avbryter tilbakekreving
        val avbruttTilbakekreving = runBlocking { service.avbrytTilbakekreving(sak.id, "merknad") }
        val sisteLagretHendelse = inTransaction { hendelseDao.hentHendelserISak(sak.id).maxBy { it.opprettet } }
        val avbruttOppgave = inTransaction { oppgaveService.hentOppgave(oppgave.id) }

        avbruttTilbakekreving.status shouldBe TilbakekrevingStatus.AVBRUTT
        avbruttOppgave.status shouldBe Status.AVBRUTT

        with(sisteLagretHendelse) {
            sakId shouldBe sak.id
            vedtakId shouldBe null
            behandlingId shouldBe tilbakekreving.id
            hendelse shouldBe TilbakekrevingHendelseType.AVBRUTT.lagEventnameForType()
            ident shouldBe null
            identType shouldBe null
            inntruffet shouldNotBe null
            opprettet shouldNotBe null
            kommentar shouldBe null
            valgtBegrunnelse shouldBe null
        }

        rapid.publiserteMeldinger.size shouldBe 2
        rapid.publiserteMeldinger.last().let { melding ->
            objectMapper.readTree(melding.verdi).let {
                it[EVENT_NAME_KEY].textValue() shouldBe TilbakekrevingHendelseType.AVBRUTT.lagEventnameForType()
                it[TILBAKEKREVING_STATISTIKK_RIVER_KEY] shouldNotBe null
            }
        }
    }

    @Test
    fun `skal oppdatere kravgrunnlag og perioder i tilbakekrevingsbehandling`() {
        val sak = inTransaction { sakSkrivDao.opprettSak(bruker, SakType.BARNEPENSJON, enhet) }

        coEvery { vedtakKlient.fattVedtakTilbakekreving(any(), any(), any()) } returns vedtak()
        coEvery { brevApiKlient.hentVedtaksbrev(any(), any()) } returns vedtaksbrev()
        coEvery { tilbakekrevingKlient.hentKravgrunnlag(any(), any(), any()) } returns
            kravgrunnlag(sak).copy(kontrollFelt = Kontrollfelt("ny_verdi"))

        // Oppretter tilbakekreving basert på kravgrunnlag
        val tilbakekreving = service.opprettTilbakekreving(kravgrunnlag(sak), null, null)
        val oppgave = inTransaction { oppgaveService.hentOppgaverForReferanse(tilbakekreving.id.toString()).first() }

        // Tildeler oppgaven til saksbehandler
        inTransaction { oppgaveService.tildelSaksbehandler(oppgave.id, saksbehandler.ident) }

        // Lagrer vurdering og perioder
        service.lagreVurdering(tilbakekreving.id, tilbakekrevingVurdering(), saksbehandler)
        service.lagrePerioder(tilbakekreving.id, tilbakekrevingPerioder(tilbakekreving), saksbehandler)
        service.validerVurderingOgPerioder(tilbakekreving.id, saksbehandler)

        // Oppdaterer kravgrunnlag
        val tilbakekrevingMedNyePerioder = service.oppdaterKravgrunnlag(tilbakekreving.id, saksbehandler)

        tilbakekrevingMedNyePerioder.status shouldBe TilbakekrevingStatus.UNDER_ARBEID

        // Nye perioder skal ha resatt verdiene som skal vurderes
        with(tilbakekrevingMedNyePerioder.tilbakekreving) {
            perioder.size shouldBe tilbakekreving.tilbakekreving.perioder.size
            perioder.forEach { perioder ->
                perioder.tilbakekrevingsbeloep.filter { it.klasseType == KlasseType.YTEL.name }.forEach {
                    with(it) {
                        skatt shouldBe null
                        resultat shouldBe null
                        rentetillegg shouldBe null
                        skyld shouldBe null
                        tilbakekrevingsprosent shouldBe null
                        beregnetFeilutbetaling shouldBe null
                        nettoTilbakekreving shouldBe null
                    }
                }
            }
        }

        tilbakekrevingMedNyePerioder.tilbakekreving.kravgrunnlag.kontrollFelt.value shouldBe "ny_verdi"

        coVerify {
            tilbakekrevingKlient.hentKravgrunnlag(saksbehandler, any(), any())
        }

        confirmVerified(tilbakekrevingKlient)
    }

    @Test
    fun `skal fatte vedtak for tilbakekrevingsbehandling`() {
        coEvery { vedtakKlient.fattVedtakTilbakekreving(any(), any(), any()) } returns vedtak()
        coEvery { brevApiKlient.hentVedtaksbrev(any(), any()) } returns vedtaksbrev()

        // Oppretter sak og tilbakekreving basert på kravgrunnlag
        val sak = inTransaction { sakSkrivDao.opprettSak(bruker, SakType.BARNEPENSJON, enhet) }
        val tilbakekreving = service.opprettTilbakekreving(kravgrunnlag(sak), null, null)
        val oppgave = inTransaction { oppgaveService.hentOppgaverForReferanse(tilbakekreving.id.toString()).first() }

        // Tildeler oppgaven til saksbehandler
        inTransaction { oppgaveService.tildelSaksbehandler(oppgave.id, saksbehandler.ident) }

        // Lagrer vurdering og perioder
        service.lagreVurdering(tilbakekreving.id, tilbakekrevingVurdering(), saksbehandler)
        service.lagrePerioder(tilbakekreving.id, tilbakekrevingPerioder(tilbakekreving), saksbehandler)
        service.validerVurderingOgPerioder(tilbakekreving.id, saksbehandler)

        // Fatter vedtak
        val tilbakekrevingMedFattetVedtak = runBlocking { service.fattVedtak(tilbakekreving.id, saksbehandler) }
        val sisteLagretHendelse = inTransaction { hendelseDao.hentHendelserISak(sak.id).maxBy { it.opprettet } }
        val oppgaveTilAttestering = inTransaction { oppgaveService.hentOppgave(oppgave.id) }

        tilbakekrevingMedFattetVedtak.status shouldBe TilbakekrevingStatus.FATTET_VEDTAK
        oppgaveTilAttestering.status shouldBe Status.ATTESTERING

        coVerify {
            vedtakKlient.lagreVedtakTilbakekreving(any(), saksbehandler, enhet)
            vedtakKlient.fattVedtakTilbakekreving(tilbakekreving.id, saksbehandler, enhet)
            brevApiKlient.hentVedtaksbrev(tilbakekreving.id, saksbehandler)
        }

        with(sisteLagretHendelse) {
            sakId shouldBe sak.id
            vedtakId shouldBe 1L
            behandlingId shouldBe tilbakekreving.id
            hendelse shouldBe TilbakekrevingHendelseType.FATTET_VEDTAK.lagEventnameForType()
            ident shouldBe saksbehandler.ident
            identType shouldBe "SAKSBEHANDLER"
            inntruffet shouldNotBe null
            opprettet shouldNotBe null
            kommentar shouldBe null
            valgtBegrunnelse shouldBe null
        }

        rapid.publiserteMeldinger.size shouldBe 2
        rapid.publiserteMeldinger.last().let { melding ->
            objectMapper.readTree(melding.verdi).let {
                it[EVENT_NAME_KEY].textValue() shouldBe TilbakekrevingHendelseType.FATTET_VEDTAK.lagEventnameForType()
                it[TILBAKEKREVING_STATISTIKK_RIVER_KEY] shouldNotBe null
            }
        }

        confirmVerified(vedtakKlient, brevApiKlient)
    }

    @Test
    fun `skal fatte og attestere vedtak for tilbakekrevingsbehandling`() {
        coEvery { brevApiKlient.hentVedtaksbrev(any(), any()) } returns vedtaksbrev()

        coEvery { brevKlient.ferdigstillStrukturertBrev(any(), any(), any()) } just runs
        coEvery { tilbakekrevingKlient.sendTilbakekrevingsvedtak(any(), any()) } just runs

        // Oppretter sak og tilbakekreving basert på kravgrunnlag
        val sak = inTransaction { sakSkrivDao.opprettSak(bruker, SakType.BARNEPENSJON, enhet) }
        val tilbakekreving = service.opprettTilbakekreving(kravgrunnlag(sak), null, null)
        val oppgave = inTransaction { oppgaveService.hentOppgaverForReferanse(tilbakekreving.id.toString()).first() }

        coEvery { vedtakKlient.hentVedtak(any(), any()) } returns
            VedtakDto(
                123L,
                tilbakekreving.id,
                VedtakStatus.FATTET_VEDTAK,
                VedtakSak("", SakType.OMSTILLINGSSTOENAD, sak.id),
                VedtakType.TILBAKEKREVING,
                VedtakFattet(
                    "fattet",
                    Enhetsnummer.ingenTilknytning,
                    Tidspunkt.now(),
                ),
                Attestasjon(
                    "attestert",
                    Enhetsnummer.ingenTilknytning,
                    Tidspunkt.now(),
                ),
                VedtakInnholdDto.VedtakTilbakekrevingDto(tilbakekreving.tilbakekreving.toObjectNode()),
            )

        coEvery { vedtakKlient.attesterVedtakTilbakekreving(any(), any(), any()) } returns
            tilbakekrevingsvedtak(saksbehandler, tilbakekreving.id, enhet)

        // Tildeler oppgaven til saksbehandler
        inTransaction { oppgaveService.tildelSaksbehandler(oppgave.id, saksbehandler.ident) }

        // Lagrer vurdering og perioder
        service.lagreVurdering(tilbakekreving.id, tilbakekrevingVurdering(), saksbehandler)
        service.lagrePerioder(tilbakekreving.id, tilbakekrevingPerioder(tilbakekreving), saksbehandler)
        service.validerVurderingOgPerioder(tilbakekreving.id, saksbehandler)

        // Fatter vedtak
        runBlocking { service.fattVedtak(tilbakekreving.id, saksbehandler) }

        // Tildeler oppgaven til attestant
        inTransaction { oppgaveService.tildelSaksbehandler(oppgave.id, attestant.ident()) }

        // Attesterer vedtaket
        val tilbakekrevingMedAttestertVedtak =
            runBlocking { service.attesterVedtak(tilbakekreving.id, "kommentar", attestant) }
        val sisteLagretHendelse = inTransaction { hendelseDao.hentHendelserISak(sak.id).maxBy { it.opprettet } }
        val oppgaveFerdigstilt = inTransaction { oppgaveService.hentOppgave(oppgave.id) }

        tilbakekrevingMedAttestertVedtak.status shouldBe TilbakekrevingStatus.ATTESTERT
        oppgaveFerdigstilt.status shouldBe Status.FERDIGSTILT

        coVerify {
            vedtakKlient.lagreVedtakTilbakekreving(any(), saksbehandler, enhet)
            vedtakKlient.fattVedtakTilbakekreving(tilbakekreving.id, saksbehandler, enhet)
            brevApiKlient.hentVedtaksbrev(tilbakekreving.id, saksbehandler)

            vedtakKlient.attesterVedtakTilbakekreving(tilbakekreving.id, attestant, enhet)
            vedtakKlient.hentVedtak(tilbakekreving.id, attestant)
            brevKlient.ferdigstillStrukturertBrev(tilbakekreving.id, Brevtype.VEDTAK, attestant)
            tilbakekrevingKlient.sendTilbakekrevingsvedtak(attestant, any())
        }

        with(sisteLagretHendelse) {
            sakId shouldBe sak.id
            vedtakId shouldBe 1L
            behandlingId shouldBe tilbakekreving.id
            hendelse shouldBe TilbakekrevingHendelseType.ATTESTERT.lagEventnameForType()
            ident shouldBe attestant.ident
            identType shouldBe "SAKSBEHANDLER"
            inntruffet shouldNotBe null
            opprettet shouldNotBe null
            kommentar shouldBe "kommentar"
            valgtBegrunnelse shouldBe null
        }

        rapid.publiserteMeldinger.size shouldBe 4
        rapid.publiserteMeldinger[2].let { melding ->
            objectMapper.readTree(melding.verdi).let {
                it[EVENT_NAME_KEY].textValue() shouldBe VedtakKafkaHendelseHendelseType.ATTESTERT.lagEventnameForType()
                it[SKAL_SENDE_BREV].booleanValue() shouldBe true
                it["vedtak"] shouldNotBe null
            }
        }
        rapid.publiserteMeldinger[3].let { melding ->
            objectMapper.readTree(melding.verdi).let {
                it[EVENT_NAME_KEY].textValue() shouldBe TilbakekrevingHendelseType.ATTESTERT.lagEventnameForType()
                it[TILBAKEKREVING_STATISTIKK_RIVER_KEY] shouldNotBe null
            }
        }

        confirmVerified(vedtakKlient, brevApiKlient, tilbakekrevingKlient)
    }

    @Test
    fun `skal fatte og underkjenne vedtak for tilbakekrevingsbehandling`() {
        coEvery { vedtakKlient.underkjennVedtakTilbakekreving(any(), any()) } returns vedtak()
        coEvery { brevApiKlient.hentVedtaksbrev(any(), any()) } returns vedtaksbrev()

        // Oppretter sak og tilbakekreving basert på kravgrunnlag
        val sak = inTransaction { sakSkrivDao.opprettSak(bruker, SakType.BARNEPENSJON, enhet) }
        val tilbakekreving = service.opprettTilbakekreving(kravgrunnlag(sak), null, null)
        val oppgave = inTransaction { oppgaveService.hentOppgaverForReferanse(tilbakekreving.id.toString()).first() }

        // Tildeler oppgaven til saksbehandler
        inTransaction { oppgaveService.tildelSaksbehandler(oppgave.id, saksbehandler.ident) }

        // Lagrer vurdering og perioder
        service.lagreVurdering(tilbakekreving.id, tilbakekrevingVurdering(), saksbehandler)
        service.lagrePerioder(tilbakekreving.id, tilbakekrevingPerioder(tilbakekreving), saksbehandler)
        service.validerVurderingOgPerioder(tilbakekreving.id, saksbehandler)

        // Fatter vedtak
        runBlocking { service.fattVedtak(tilbakekreving.id, saksbehandler) }

        // Tildeler oppgaven til attestant
        inTransaction { oppgaveService.tildelSaksbehandler(oppgave.id, attestant.ident) }

        // Underkjenner vedtaket
        val tilbakekrevingMedUnderkjentVedtak =
            runBlocking { service.underkjennVedtak(tilbakekreving.id, "kommentar", "feil beregning", attestant) }
        val sisteLagretHendelse = inTransaction { hendelseDao.hentHendelserISak(sak.id).maxBy { it.opprettet } }
        val oppgaveUnderkjent = inTransaction { oppgaveService.hentOppgave(oppgave.id) }

        tilbakekrevingMedUnderkjentVedtak.status shouldBe TilbakekrevingStatus.UNDERKJENT
        oppgaveUnderkjent.status shouldBe Status.UNDERKJENT

        coVerify {
            vedtakKlient.lagreVedtakTilbakekreving(any(), saksbehandler, enhet)
            vedtakKlient.fattVedtakTilbakekreving(tilbakekreving.id, saksbehandler, enhet)
            brevApiKlient.hentVedtaksbrev(tilbakekreving.id, saksbehandler)

            vedtakKlient.underkjennVedtakTilbakekreving(tilbakekreving.id, attestant)
        }

        with(sisteLagretHendelse) {
            sakId shouldBe sak.id
            vedtakId shouldBe 1L
            behandlingId shouldBe tilbakekreving.id
            hendelse shouldBe TilbakekrevingHendelseType.UNDERKJENT.lagEventnameForType()
            ident shouldBe attestant.ident
            identType shouldBe "SAKSBEHANDLER"
            inntruffet shouldNotBe null
            opprettet shouldNotBe null
            kommentar shouldBe "kommentar"
            valgtBegrunnelse shouldBe "feil beregning"
        }

        rapid.publiserteMeldinger.size shouldBe 3
        rapid.publiserteMeldinger.last().let { melding ->
            objectMapper.readTree(melding.verdi).let {
                it[EVENT_NAME_KEY].textValue() shouldBe TilbakekrevingHendelseType.UNDERKJENT.lagEventnameForType()
                it[TILBAKEKREVING_STATISTIKK_RIVER_KEY] shouldNotBe null
            }
        }

        confirmVerified(vedtakKlient, brevApiKlient)
    }

    private fun tilbakekrevingsvedtak(
        saksbehandler: BrukerTokenInfo,
        behandlingId: UUID,
        enhet: Enhetsnummer,
    ): VedtakDto =
        mockk(relaxed = true) {
            every { id } returns 1
            every { this@mockk.behandlingId } returns behandlingId
            every { vedtakFattet } returns
                VedtakFattet(
                    ansvarligSaksbehandler = saksbehandler.ident(),
                    ansvarligEnhet = enhet,
                    tidspunkt = Tidspunkt.now(),
                )
        }

    private fun vedtak(): VedtakDto =
        mockk {
            every { id } returns 1
        }

    private fun tilbakekrevingPerioder(tilbakekreving: TilbakekrevingBehandling) = listOf(oppdatertPeriode(tilbakekreving))

    private fun oppdatertPeriode(tilbakekreving: TilbakekrevingBehandling): TilbakekrevingPeriode =
        tilbakekreving.tilbakekreving.perioder.first().let {
            it.copy(
                tilbakekrevingsbeloep =
                    it.tilbakekrevingsbeloep
                        .filter { beloep ->
                            beloep.klasseType == KlasseType.YTEL.name
                        }.map { beloep ->
                            beloep.copy(
                                beregnetFeilutbetaling = 100,
                                bruttoTilbakekreving = 100,
                                nettoTilbakekreving = 100,
                                skatt = 10,
                                skyld = TilbakekrevingSkyld.BRUKER,
                                resultat = TilbakekrevingResultat.FULL_TILBAKEKREV,
                                tilbakekrevingsprosent = 100,
                                rentetillegg = 10,
                            )
                        },
            )
        }

    private fun opprettetBrevDto(brevId: Long) =
        Brev(
            id = brevId,
            status = no.nav.etterlatte.brev.model.Status.OPPRETTET,
            mottakere =
                listOf(
                    Mottaker(
                        UUID.randomUUID(),
                        navn = "Mottaker mottakersen",
                        foedselsnummer = MottakerFoedselsnummer("19448310410"),
                        orgnummer = null,
                        adresse =
                            Adresse(
                                adresseType = "",
                                landkode = "",
                                land = "",
                            ),
                        journalpostId = null,
                        bestillingId = null,
                    ),
                ),
            sakId = randomSakId(),
            behandlingId = null,
            tittel = null,
            spraak = Spraak.NB,
            prosessType = BrevProsessType.REDIGERBAR,
            soekerFnr = "",
            statusEndret = Tidspunkt.now(),
            opprettet = Tidspunkt.now(),
            brevtype = Brevtype.MANUELT,
            brevkoder = Brevkoder.TILBAKEKREVING,
        )

    private fun vedtaksbrev() = opprettetBrevDto(Random.nextLong())
}
